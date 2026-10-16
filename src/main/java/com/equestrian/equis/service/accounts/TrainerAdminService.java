package com.equestrian.equis.service.accounts;

import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.dto.accounts.InviteTrainerRequest;
import com.equestrian.equis.dto.accounts.TrainerList;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.dto.accounts.TrainerTotals;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.AccountLinkService.IssuedLink;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

// W4: the admin's trainer list and invites.
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

    public TrainerAdminService(TrainerRepository trainers, AdminAccountRepository admins, AccountLinkService links,
                               TrainerRowAssembler rows, ApplicationEventPublisher events) {
        this.trainers = trainers;
        this.admins = admins;
        this.links = links;
        this.rows = rows;
        this.events = events;
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

    // Also used by the row actions (resend, reset password).
    void sendLink(Trainer trainer, InvitePurpose purpose) {
        IssuedLink link = links.issue(trainer, purpose);
        events.publishEvent(new AccountLinkCreatedEvent(trainer.getId(), trainer.getName(), trainer.getEmail(),
                purpose, link.rawToken(), link.expiresAt()));
    }

    private static long count(List<Trainer> trainers, TrainerStatus status) {
        return trainers.stream().filter(t -> t.getStatus() == status).count();
    }
}
