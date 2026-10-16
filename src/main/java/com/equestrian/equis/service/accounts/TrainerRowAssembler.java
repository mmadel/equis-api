package com.equestrian.equis.service.accounts;

import com.equestrian.equis.dto.accounts.LevelCount;
import com.equestrian.equis.dto.accounts.PendingLink;
import com.equestrian.equis.dto.accounts.RiderCounts;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.AssessorCount;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.riders.TrainerLevelCount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// The one place W4 rows are built. Three queries for the whole list, whatever the number of trainers.
@Component
public class TrainerRowAssembler {
    private final RiderRepository riders;
    private final AssessmentBookingRepository bookings;
    private final InviteRepository invites;
    private final Clock clock;
    private final ZoneId schoolZone;

    public TrainerRowAssembler(RiderRepository riders, AssessmentBookingRepository bookings, InviteRepository invites,
                               Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.riders = riders;
        this.bookings = bookings;
        this.invites = invites;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    public List<TrainerRow> rows(List<Trainer> trainers) {
        if (trainers.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = trainers.stream().map(Trainer::getId).toList();
        Instant now = clock.instant();

        Map<UUID, List<TrainerLevelCount>> levelCounts = riders.countByTrainerIdsAndLevel(ids).stream()
                .collect(Collectors.groupingBy(TrainerLevelCount::getTrainerId));

        // The current calendar month in the school time zone.
        ZonedDateTime monthStart = LocalDate.ofInstant(now, schoolZone).withDayOfMonth(1).atStartOfDay(schoolZone);
        Map<UUID, Long> recorded = bookings.countRecordedByAssessorIdsBetween(ids,
                        monthStart.toInstant(), monthStart.plusMonths(1).toInstant()).stream()
                .collect(Collectors.toMap(AssessorCount::getAssessorId, AssessorCount::getRecorded));

        Map<UUID, Invite> openLinks = invites.findByTrainerIdInAndAcceptedAtIsNull(ids).stream()
                .collect(Collectors.toMap(i -> i.getTrainer().getId(), Function.identity()));

        return trainers.stream()
                .map(t -> row(t, levelCounts.getOrDefault(t.getId(), List.of()),
                        recorded.getOrDefault(t.getId(), 0L), openLinks.get(t.getId()), now))
                .toList();
    }

    public TrainerRow row(Trainer trainer) {
        return rows(List.of(trainer)).getFirst();
    }

    private static TrainerRow row(Trainer t, List<TrainerLevelCount> levelCounts, long recorded, Invite link,
                                  Instant now) {
        List<LevelCount> byLevel = levelCounts.stream()
                .map(c -> new LevelCount(c.getLevelId(), c.getRiderCount()))
                .sorted(Comparator.comparingInt(LevelCount::levelId))
                .toList();
        long total = byLevel.stream().mapToLong(LevelCount::count).sum();
        PendingLink pending = link == null ? null
                : new PendingLink(lower(link.getPurpose().name()), link.getExpiresAt(), !link.isOpen(now));
        return new TrainerRow(t.getId(), t.getName(), t.getPhone(), t.getEmail(), lower(t.getStatus().name()),
                new RiderCounts(total, byLevel), recorded, t.getLastActiveAt(), pending, t.getCreatedAt());
    }

    private static String lower(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
