package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.UpdateMemberRequest;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

// W2's Edit: fixes a person's name, phone or email with the same rules as intake.
@Service
public class MemberDetailsService {
    private final MemberRepository members;
    private final RiderRepository riders;
    private final AssessmentBookingRepository bookings;
    private final AssessmentRules rules;
    private final AdminBookingRowAssembler rows;
    private final TransactionTemplate freshRead;

    public MemberDetailsService(MemberRepository members, RiderRepository riders,
                                AssessmentBookingRepository bookings, AssessmentRules rules,
                                AdminBookingRowAssembler rows, PlatformTransactionManager transactionManager) {
        this.members = members;
        this.riders = riders;
        this.bookings = bookings;
        this.rules = rules;
        this.rows = rows;
        this.freshRead = new TransactionTemplate(transactionManager);
        this.freshRead.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.freshRead.setReadOnly(true);
    }

    // Closed people can be edited too. Answers with the person's W2 row, which is their latest booking.
    @Transactional
    public AdminBookingRow update(UUID id, UpdateMemberRequest req) {
        Member member = members.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.MEMBER_NOT_FOUND, "Person not found."));
        if (req.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                    Map.of("fieldErrors", List.of(new ApiFieldError("name", ErrorCodes.NOTHING_TO_CHANGE))));
        }
        if (riders.existsByMemberId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.MEMBER_IS_RIDER,
                    member.getName() + " is a rider. Edit rider details instead.");
        }
        if (req.phone() != null) {
            rules.requirePhoneFree(req.phone(), id);
        }
        if (req.email() != null) {
            rules.requireEmailFree(req.email(), id);
        }

        member.updateDetails(
                req.name() != null ? req.name() : member.getName(),
                req.phone() != null ? req.phone() : member.getPhone(),
                req.email() != null ? req.email() : member.getEmail());
        // saveAndFlush, so a racing request that took the phone or email first fails here and is mapped.
        try {
            members.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw switch (Constraints.nameOf(e).orElse("")) {
                case "uq_member_phone" -> rules.phoneTaken(existing(() -> members.findByPhone(req.phone())));
                case "uq_member_email" -> rules.emailTaken(existing(() -> members.findByEmail(req.email())));
                default -> e;
            };
        }

        // Every person is added together with a booking, so there is always one.
        return rows.row(bookings.findLatestForMember(id)
                .orElseThrow(() -> new IllegalStateException("Member " + id + " has no booking")));
    }

    // After a clash this transaction can't run queries any more, so the person who won is read in a new one.
    private Member existing(Supplier<Optional<Member>> find) {
        return freshRead.execute(status -> find.get().orElse(null));
    }
}
