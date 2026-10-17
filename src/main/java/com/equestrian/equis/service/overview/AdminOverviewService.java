package com.equestrian.equis.service.overview;

import com.equestrian.equis.common.time.SchoolWeek;
import com.equestrian.equis.dto.accounts.TrainerRef;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.overview.ActivityItem;
import com.equestrian.equis.dto.overview.Attention;
import com.equestrian.equis.dto.overview.OverviewResponse;
import com.equestrian.equis.dto.overview.PendingInvite;
import com.equestrian.equis.dto.overview.TodaySession;
import com.equestrian.equis.dto.overview.TrackFill;
import com.equestrian.equis.dto.overview.WeekFill;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.levels.Levels;
import com.equestrian.equis.entity.riders.LevelChange;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.riders.LevelChangeRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SeatHolder;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.service.assessment.AdminBookingService;
import com.equestrian.equis.service.riders.AdminRiderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// The admin Overview screen: today's sessions, the week's fill, what needs attention and the latest trainer activity.
// Read-only and computed on each request, in a fixed number of queries whatever the data size.
@Service
@Transactional(readOnly = true)
public class AdminOverviewService {
    static final int OVERDUE_SHOWN = 5;
    static final int ACTIVITY_SHOWN = 10;
    private static final String WAITING = "waiting";
    // groupingBy key for the assessment slots, which have no level.
    private static final int ASSESSMENT = 0;
    private static final Comparator<TodaySession> TODAY_ORDER = Comparator
            .comparing(TodaySession::start)
            .thenComparing(TodaySession::levelId, Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Comparator<AdminBookingRow> OLDEST_SLOT_FIRST = Comparator
            .<AdminBookingRow, LocalDate>comparing(r -> r.slot().date())
            .thenComparing(r -> r.slot().start());

    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final AssessmentBookingRepository bookings;
    private final LevelChangeRepository levelChanges;
    private final TrainerRepository trainers;
    private final InviteRepository invites;
    private final AdminBookingService adminBookings;
    private final AdminRiderService adminRiders;
    private final Clock clock;
    private final ZoneId schoolZone;

    public AdminOverviewService(SessionRepository sessions, AssignmentRepository assignments,
                                AssessmentBookingRepository bookings, LevelChangeRepository levelChanges,
                                TrainerRepository trainers, InviteRepository invites,
                                AdminBookingService adminBookings, AdminRiderService adminRiders,
                                Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.sessions = sessions;
        this.assignments = assignments;
        this.bookings = bookings;
        this.levelChanges = levelChanges;
        this.trainers = trainers;
        this.invites = invites;
        this.adminBookings = adminBookings;
        this.adminRiders = adminRiders;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    public OverviewResponse get() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, schoolZone);
        LocalDate from = SchoolWeek.saturdayOf(today);
        LocalDate to = SchoolWeek.fridayOf(from);

        List<Session> week = sessions.findActiveBetween(from, to);
        Map<UUID, List<SeatHolder>> seats = seatHolders(week);

        List<TodaySession> sessionsToday = week.stream()
                .filter(s -> s.getSessionDate().equals(today))
                .map(s -> todaySession(s, seats.getOrDefault(s.getId(), List.of()), now))
                .sorted(TODAY_ORDER)
                .toList();
        return new OverviewResponse(today, sessionsToday, weekFill(from, to, week, seats), attention(now),
                activity());
    }

    // Level sessions hold one seat per assignment, assessment slots one per booking that isn't cancelled.
    // Two queries at most, whatever the number of sessions.
    private Map<UUID, List<SeatHolder>> seatHolders(List<Session> week) {
        List<UUID> levelSessions = week.stream().filter(s -> !isAssessment(s)).map(Session::getId).toList();
        List<UUID> slots = week.stream().filter(AdminOverviewService::isAssessment).map(Session::getId).toList();
        return Stream.concat(
                        levelSessions.isEmpty() ? Stream.empty() : assignments.seatHolders(levelSessions).stream(),
                        slots.isEmpty() ? Stream.empty() : bookings.seatHolders(slots).stream())
                .collect(Collectors.groupingBy(SeatHolder::sessionId));
    }

    private TodaySession todaySession(Session s, List<SeatHolder> seats, Instant now) {
        List<String> names = seats.stream()
                .map(SeatHolder::trainerName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new TodaySession(s.getId(), levelId(s), s.getStartTime(), s.getEndTime(), s.getCapacity(),
                seats.size(), names, state(s, now));
    }

    private String state(Session s, Instant now) {
        if (now.isBefore(s.startsAt(schoolZone).toInstant())) {
            return "upcoming";
        }
        return now.isBefore(s.getSessionDate().atTime(s.getEndTime()).atZone(schoolZone).toInstant())
                ? "in_progress" : "finished";
    }

    // Levels 1–6 then assessment, every one listed even when it has no session.
    private static WeekFill weekFill(LocalDate from, LocalDate to, List<Session> week,
                                     Map<UUID, List<SeatHolder>> seats) {
        Map<Integer, List<Session>> byTrack = week.stream()
                .collect(Collectors.groupingBy(s -> Objects.requireNonNullElse(levelId(s), ASSESSMENT)));
        List<TrackFill> tracks = Stream.concat(
                        IntStream.rangeClosed(Levels.MIN_LEVEL, Levels.MAX_LEVEL).boxed(), Stream.of((Integer) null))
                .map(levelId -> {
                    List<Session> track = byTrack.getOrDefault(Objects.requireNonNullElse(levelId, ASSESSMENT),
                            List.of());
                    return new TrackFill(levelId, booked(track, seats), capacity(track));
                })
                .toList();
        return new WeekFill(from, to, booked(week, seats), capacity(week), tracks);
    }

    private Attention attention(Instant now) {
        List<AdminBookingRow> waiting = adminBookings.list(WAITING, null).bookings();
        List<PendingInvite> pending = invites.findOpenForInvitedTrainers().stream()
                .map(i -> new PendingInvite(ref(i.getTrainer()), i.getExpiresAt(), !i.isOpen(now)))
                .toList();
        return new Attention(waiting.size(),
                waiting.stream().sorted(OLDEST_SLOT_FIRST).limit(OVERDUE_SHOWN).toList(),
                adminRiders.countNeedingTrainer(),
                trainers.disabledWithRiderCounts(),
                pending);
    }

    // Results recorded and level changes, merged newest first. Admin actions have no history table, so they are
    // not listed.
    private List<ActivityItem> activity() {
        PageRequest latest = PageRequest.of(0, ACTIVITY_SHOWN);
        List<ActivityItem> items = new ArrayList<>();
        for (AssessmentBooking b : bookings.findRecentResults(latest)) {
            boolean placed = b.getStatus() == BookingStatus.PLACED;
            items.add(new ActivityItem(placed ? "placed" : "noshow", b.getRecordedAt(), ref(b.getAssessor()),
                    b.getMember().getName(), null, placed ? b.getLevel().getId().intValue() : null));
        }
        for (LevelChange c : levelChanges.findRecentChanges(latest)) {
            items.add(new ActivityItem(c.isPromotion() ? "promoted" : "demoted", c.getCreatedAt(),
                    ref(c.getByTrainer()), c.getRider().getName(), c.getFromLevel().getId().intValue(),
                    c.getToLevel().getId().intValue()));
        }
        return items.stream()
                .sorted(Comparator.comparing(ActivityItem::at).reversed())
                .limit(ACTIVITY_SHOWN)
                .toList();
    }

    private static int booked(List<Session> track, Map<UUID, List<SeatHolder>> seats) {
        return track.stream().mapToInt(s -> seats.getOrDefault(s.getId(), List.of()).size()).sum();
    }

    private static int capacity(List<Session> track) {
        return track.stream().mapToInt(Session::getCapacity).sum();
    }

    private static boolean isAssessment(Session s) {
        return s.getTemplate().isAssessment();
    }

    private static Integer levelId(Session s) {
        return isAssessment(s) ? null : s.getTemplate().getLevel().getId().intValue();
    }

    private static TrainerRef ref(Trainer t) {
        return new TrainerRef(t.getId(), t.getName(), t.getStatus().name().toLowerCase(Locale.ROOT));
    }
}
