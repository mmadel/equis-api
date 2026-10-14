package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.dto.assessment.NewMemberRequest;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class MemberBookingService {
    private final SessionRepository sessions;
    private final MemberRepository members;
    private final AssessmentBookingRepository bookings;
    private final TrainerRepository trainers;
    private final AssessmentRules rules;
    private final Clock clock;

    public MemberBookingService(SessionRepository sessions, MemberRepository members,
                                AssessmentBookingRepository bookings, TrainerRepository trainers,
                                AssessmentRules rules, Clock clock) {
        this.sessions = sessions;
        this.members = members;
        this.bookings = bookings;
        this.trainers = trainers;
        this.rules = rules;
        this.clock = clock;
    }

    // Registers a walk-in and books them into an assessment slot, with the trainer as assessor.
    // Every refusal is a runtime exception, so both rows roll back: a member is never left without the booking.
    @Transactional
    public AssessmentBooking registerAndBook(UUID trainerId, NewMemberRequest req) {
        Instant now = clock.instant();

        // The lock makes the seat count safe against a second trainer booking at the same time.
        Session slot = sessions.findByIdForUpdate(req.slotId()).orElseThrow(rules::slotNotFound);
        rules.requireAssessmentSlot(slot);
        rules.requireBookable(slot, now);
        rules.requireSeat(slot);
        rules.requirePhoneFree(req.phone());

        // saveAndFlush, so a unique-key clash fails here and is mapped; with save it would fail at commit as a 500.
        // After a clash the Hibernate session is unusable, which is fine: the method only throws from there.
        try {
            Member member = members.saveAndFlush(Member.register(req.name(), req.phone()));
            return bookings.saveAndFlush(AssessmentBooking.book(member, slot, trainers.getReferenceById(trainerId)));
        } catch (DataIntegrityViolationException e) {
            throw switch (Constraints.nameOf(e).orElse("")) {
                case "uq_member_phone" -> rules.phoneTaken();
                case "uq_assessment_booking_active_member" -> rules.memberAlreadyBooked(slot);
                default -> e;
            };
        }
    }
}
