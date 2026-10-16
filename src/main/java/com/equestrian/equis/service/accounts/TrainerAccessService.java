package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

// W4: the rules behind disabling and enabling a trainer. The account-actions endpoints call these.
@Service
public class TrainerAccessService {
    private final TrainerRepository trainers;
    private final RiderRepository riders;
    private final AssessmentBookingRepository bookings;
    private final InviteRepository invites;
    private final AuthTokenService tokens;

    public TrainerAccessService(TrainerRepository trainers, RiderRepository riders,
                                AssessmentBookingRepository bookings, InviteRepository invites,
                                AuthTokenService tokens) {
        this.trainers = trainers;
        this.riders = riders;
        this.bookings = bookings;
        this.invites = invites;
        this.tokens = tokens;
    }

    // Riders keep their trainer and seats, and upcoming bookings keep the assessor: the admin moves them in E7.
    @Transactional
    public Trainer disable(UUID id, boolean confirm) {
        Trainer t = trainers.findByIdForUpdate(id).orElseThrow(TrainerErrors::notFound);
        if (t.getStatus() == TrainerStatus.DISABLED) {
            return t; // repeat click
        }
        if (t.getStatus() != TrainerStatus.ACTIVE) {
            throw TrainerErrors.invalidStatus(t, "disable");
        }

        long riderCount = riders.countByTrainerId(id);
        long upcoming = bookings.countByAssessorIdAndStatus(id, BookingStatus.UPCOMING);
        if ((riderCount > 0 || upcoming > 0) && !confirm) {
            throw confirmRequired(t, riderCount, upcoming);
        }

        t.setStatus(TrainerStatus.DISABLED);
        invites.deleteByTrainerIdAndAcceptedAtIsNull(id);
        // Same transaction: if anything fails, the status change rolls back too.
        tokens.revokeAll(Role.TRAINER, id);
        return t;
    }

    // Tokens and links are not restored: the trainer signs in again with their old password.
    @Transactional
    public Trainer enable(UUID id) {
        Trainer t = trainers.findByIdForUpdate(id).orElseThrow(TrainerErrors::notFound);
        if (t.getStatus() == TrainerStatus.ACTIVE) {
            return t;
        }
        if (t.getStatus() != TrainerStatus.DISABLED) {
            throw TrainerErrors.invalidStatus(t, "enable");
        }
        t.setStatus(TrainerStatus.ACTIVE);
        return t;
    }

    private static ApiException confirmRequired(Trainer t, long riderCount, long upcoming) {
        String detail = t.getName() + " has " + plural(riderCount, "rider") + " and "
                + plural(upcoming, "upcoming assessment") + ". Confirm to disable.";
        return new ApiException(HttpStatus.CONFLICT, ErrorCodes.CONFIRM_REQUIRED, detail,
                Map.of("riderCount", riderCount, "upcomingAssessmentCount", upcoming));
    }

    private static String plural(long n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }
}
