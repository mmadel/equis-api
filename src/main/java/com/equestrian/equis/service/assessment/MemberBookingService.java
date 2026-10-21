package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.dto.assessment.AdminNewMemberRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class MemberBookingService {
    private final SessionRepository sessions;
    private final MemberRepository members;
    private final AssessmentBookingRepository bookings;
    private final AssessmentRules rules;
    private final TransactionTemplate freshRead;
    private final Clock clock;

    public MemberBookingService(SessionRepository sessions, MemberRepository members,
                                AssessmentBookingRepository bookings, AssessmentRules rules,
                                PlatformTransactionManager transactionManager, Clock clock) {
        this.sessions = sessions;
        this.members = members;
        this.bookings = bookings;
        this.rules = rules;
        this.clock = clock;
        this.freshRead = new TransactionTemplate(transactionManager);
        this.freshRead.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.freshRead.setReadOnly(true);
    }

    // W2's New person: records the person and books their first assessment with the chosen trainer.
    // Every refusal is a runtime exception, so both rows roll back: a member is never left without the booking.
    @Transactional
    public AssessmentBooking registerAndBook(AdminNewMemberRequest req) {
        Instant now = clock.instant();

        // The lock makes the seat count safe against a second booking into the same slot at the same time.
        Session slot = sessions.findByIdForUpdate(req.slotId()).orElseThrow(rules::slotNotFound);
        rules.requireAssessmentSlot(slot);
        rules.requireBookable(slot, now);
        rules.requireSeat(slot);
        Trainer assessor = rules.requireActiveAssessor(req.assessorId());
        rules.requirePhoneFree(req.phone());
        rules.requireEmailFree(req.email());

        // saveAndFlush, so a unique-key clash (a racing request for another slot) fails here and is mapped;
        // with save it would fail at commit as a 500.
        try {
            Member member = members.saveAndFlush(Member.register(req.name(), req.phone(), req.email()));
            return bookings.saveAndFlush(AssessmentBooking.book(member, slot, assessor));
        } catch (DataIntegrityViolationException e) {
            throw switch (Constraints.nameOf(e).orElse("")) {
                case "uq_member_phone" -> rules.phoneTaken(existing(() -> members.findByPhone(req.phone())));
                case "uq_member_email" -> rules.emailTaken(existing(() -> members.findByEmail(req.email())));
                case "uq_assessment_booking_active_member" -> rules.memberAlreadyBooked(slot);
                default -> e;
            };
        }
    }

    // After a clash this transaction can't run queries any more, so the person who won is read in a new one.
    private Member existing(Supplier<Optional<Member>> find) {
        return freshRead.execute(status -> find.get().orElse(null));
    }
}
