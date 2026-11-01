package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.dto.accounts.InviteTrainerRequest;
import com.equestrian.equis.dto.accounts.TrainerList;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.dto.accounts.TrainerTotals;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.AccountLinkService.IssuedLink;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// W4: the admin's trainer list, invites and row actions.
@Service
public class TrainerAdminService {
    private static final Map<TrainerStatus, Integer> STATUS_ORDER =
            Map.of(TrainerStatus.ACTIVE, 0, TrainerStatus.INVITED, 1, TrainerStatus.DISABLED, 2);
    private static final Comparator<Trainer> LIST_ORDER = Comparator
            .<Trainer>comparingInt(t -> STATUS_ORDER.get(t.getStatus()))
            .thenComparing(Trainer::getName, String.CASE_INSENSITIVE_ORDER);

    private final TrainerRepository trainers;
    private final AdminAccountRepository admins;
    private final AccountLinkService links;
    private final TrainerRowAssembler rows;
    private final ApplicationEventPublisher events;
    private final TrainerAccessService access;
    private final RiderRepository riders;
    private final AssessmentBookingRepository bookings;
    private final InviteRepository invites;
    private final MemberRepository members;

    public TrainerAdminService(TrainerRepository trainers, AdminAccountRepository admins, AccountLinkService links,
                               TrainerRowAssembler rows, ApplicationEventPublisher events,
                               TrainerAccessService access, RiderRepository riders,
                               AssessmentBookingRepository bookings, InviteRepository invites,
                               MemberRepository members) {
        this.trainers = trainers;
        this.admins = admins;
        this.links = links;
        this.rows = rows;
        this.events = events;
        this.access = access;
        this.riders = riders;
        this.bookings = bookings;
        this.invites = invites;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public TrainerList list() {
        List<Trainer> sorted = trainers.findAll().stream().sorted(LIST_ORDER).toList();
        List<TrainerRow> trainerRows = rows.rows(sorted);
        TrainerTotals totals = new TrainerTotals(
                count(sorted, TrainerStatus.ACTIVE),
                count(sorted, TrainerStatus.INVITED),
                count(sorted, TrainerStatus.DISABLED),
                // A disabled trainer's riders are not covered.
                trainerRows.stream()
                        .filter(r -> r.status().equals("active"))
                        .mapToLong(r -> r.riders().total())
                        .sum());
        return new TrainerList(totals, trainerRows);
    }

    // Nothing is saved and no email goes out if any check fails.
    @Transactional
    public TrainerRow invite(InviteTrainerRequest req) {
        if (trainers.existsByPhone(req.phone())) {
            throw TrainerErrors.phoneTaken();
        }
        // An email can't be both the admin's and a trainer's.
        if (trainers.existsByEmail(req.email()) || admins.existsByEmail(req.email())) {
            throw TrainerErrors.emailTaken();
        }
        // A person's email becomes their rider login, so a trainer can't take it.
        if (members.existsByEmail(req.email())) {
            throw AccountErrors.emailInUse();
        }
        Trainer trainer;
        try {
            trainer = trainers.saveAndFlush(Trainer.invite(req.name(), req.phone(), req.email()));
        } catch (DataIntegrityViolationException e) {
            // Two invites with the same phone or email at the same moment.
            throw switch (Constraints.nameOf(e).orElse("")) {
                case "uq_trainer_phone" -> TrainerErrors.phoneTaken();
                case "uq_trainer_email" -> TrainerErrors.emailTaken();
                default -> e;
            };
        }
        sendLink(trainer, InvitePurpose.INVITE);
        return rows.row(trainer);
    }

    // A new 72-hour invite link; issuing it deletes the old one, so the earlier email stops working.
    @Transactional
    public TrainerRow resendInvite(UUID id) {
        Trainer t = trainers.findByIdForUpdate(id).orElseThrow(TrainerErrors::notFound);
        if (t.getStatus() != TrainerStatus.INVITED) {
            throw TrainerErrors.invalidStatus(t, "resend-invite");
        }
        sendLink(t, InvitePurpose.INVITE);
        return rows.row(t);
    }

    // Only a 24-hour set-password link: no token is revoked and the old password works until the link is used.
    @Transactional
    public TrainerRow resetPassword(UUID id) {
        Trainer t = trainers.findByIdForUpdate(id).orElseThrow(TrainerErrors::notFound);
        if (t.getStatus() != TrainerStatus.ACTIVE) {
            throw TrainerErrors.invalidStatus(t, "reset-password");
        }
        sendLink(t, InvitePurpose.RESET);
        return rows.row(t);
    }

    @Transactional
    public TrainerRow disable(UUID id, boolean confirm) {
        return rows.row(access.disable(id, confirm));
    }

    @Transactional
    public TrainerRow enable(UUID id) {
        return rows.row(access.enable(id));
    }

    // Removes the trainer so their phone and email are free for a corrected invite.
    @Transactional
    public void cancelInvite(UUID id) {
        Trainer t = trainers.findByIdForUpdate(id).orElseThrow(TrainerErrors::notFound);
        if (t.getStatus() != TrainerStatus.INVITED) {
            throw TrainerErrors.invalidStatus(t, "cancel-invite");
        }
        if (riders.existsByTrainerId(id) || bookings.existsByAssessorId(id)) {
            throw TrainerErrors.inUse(t);
        }
        invites.deleteByTrainerId(id); // explicit, so no Invite stays in the persistence context
        trainers.delete(t);
    }

    // Also used by the row actions (resend, reset password).
    void sendLink(Trainer trainer, InvitePurpose purpose) {
        IssuedLink link = links.issue(trainer, purpose);
        events.publishEvent(new AccountLinkCreatedEvent(SubjectType.TRAINER, trainer.getId(), trainer.getName(),
                trainer.getEmail(), purpose, link.rawToken(), link.expiresAt()));
    }

    private static long count(List<Trainer> trainers, TrainerStatus status) {
        return trainers.stream().filter(t -> t.getStatus() == status).count();
    }
}
