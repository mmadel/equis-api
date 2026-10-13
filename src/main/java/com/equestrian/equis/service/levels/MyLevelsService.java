package com.equestrian.equis.service.levels;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.levels.LevelRef;
import com.equestrian.equis.dto.levels.LevelSessionsResponse;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.Day;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.Seat;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.SessionSlot;
import com.equestrian.equis.dto.levels.MyLevelSummary;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.LevelRiderCount;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.LevelSeatCount;
import com.equestrian.equis.repository.sessions.LevelSessionCount;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Read side of the trainer's level screens.
@Service
@Transactional(readOnly = true)
public class MyLevelsService {
    // The school week runs Saturday to Friday.
    private static final DayOfWeek WEEK_START = DayOfWeek.SATURDAY;
    private static final int MAX_RANGE_DAYS = 56;

    private final LevelRepository levels;
    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final RiderRepository riders;
    private final Clock clock;
    private final ZoneId schoolZone;

    public MyLevelsService(LevelRepository levels, SessionRepository sessions, AssignmentRepository assignments,
                           RiderRepository riders, Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.levels = levels;
        this.sessions = sessions;
        this.assignments = assignments;
        this.riders = riders;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // Every level, in order, with this week's numbers. Three grouped queries, merged here; not one per level.
    public List<MyLevelSummary> summaries(UUID trainerId) {
        LocalDate from = clock.instant().atZone(schoolZone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(WEEK_START));
        LocalDate to = from.plusDays(6);

        Map<Short, LevelSessionCount> sessionCounts = sessions.countScheduledPerLevel(from, to).stream()
                .collect(Collectors.toMap(LevelSessionCount::getLevelId, Function.identity()));
        Map<Short, Long> riderCounts = riders.countByTrainerPerLevel(trainerId).stream()
                .collect(Collectors.toMap(LevelRiderCount::getLevelId, LevelRiderCount::getRiderCount));
        Map<Short, Long> seatCounts = assignments.countScheduledPerLevel(from, to).stream()
                .collect(Collectors.toMap(LevelSeatCount::getLevelId, LevelSeatCount::getSeatCount));

        return levels.findAll(Sort.by("id")).stream()
                .map(level -> {
                    LevelSessionCount inWeek = sessionCounts.get(level.getId());
                    return new MyLevelSummary(level.getId(), level.getName(), level.getColour(),
                            inWeek == null ? 0 : inWeek.getSessionCount(),
                            riderCounts.getOrDefault(level.getId(), 0L),
                            seatCounts.getOrDefault(level.getId(), 0L),
                            inWeek == null ? 0 : inWeek.getSeatsTotal());
                })
                .toList();
    }

    // The level's scheduled sessions from..to (both included), grouped by date. Two queries whatever the range.
    public LevelSessionsResponse sessions(UUID trainerId, short levelId, LocalDate from, LocalDate to) {
        Level level = levels.findById(levelId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.LEVEL_NOT_FOUND, "This level does not exist."));
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE,
                    "The dates must be in order and cover at most " + MAX_RANGE_DAYS + " days.");
        }

        List<Session> inRange = sessions.findScheduledByLevel(levelId, from, to);
        Map<UUID, List<Assignment>> seatsBySession = inRange.isEmpty() ? Map.of()
                : assignments.findBySessionIdIn(inRange.stream().map(Session::getId).toList()).stream()
                        .collect(Collectors.groupingBy(seat -> seat.getSession().getId()));

        Instant now = clock.instant();
        Map<LocalDate, List<SessionSlot>> byDate = new LinkedHashMap<>();
        for (Session session : inRange) {
            List<Assignment> seats = seatsBySession.getOrDefault(session.getId(), List.of());
            byDate.computeIfAbsent(session.getSessionDate(), date -> new ArrayList<>())
                    .add(new SessionSlot(session.getId(), session.getStartTime(), session.getEndTime(),
                            session.getCapacity(), seats.size(), !session.isUpcoming(now, schoolZone),
                            seats(seats, trainerId)));
        }

        List<Day> days = byDate.entrySet().stream()
                .map(entry -> new Day(entry.getKey(), entry.getValue()))
                .toList();
        return new LevelSessionsResponse(LevelRef.of(level), from, to, days);
    }

    // The caller's seats first, by initials; then one anonymous seat per rider of another trainer.
    private static List<Seat> seats(List<Assignment> seats, UUID trainerId) {
        return seats.stream()
                .map(seat -> seat.getAssignedBy().getId().equals(trainerId)
                        ? new Seat(true, seat.getRider().getId(), seat.getRider().initials())
                        : Seat.other())
                .sorted(Comparator.comparing(Seat::mine).reversed()
                        .thenComparing(Seat::initials, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
