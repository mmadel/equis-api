package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.time.SchoolWeek;
import com.equestrian.equis.dto.levels.LevelRef;
import com.equestrian.equis.dto.sessions.MyWeek;
import com.equestrian.equis.dto.sessions.MyWeek.WeekDay;
import com.equestrian.equis.dto.sessions.MyWeek.WeekSession;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Read side of the trainer's Week view (screen 5): every level's sessions in one school week.
@Service
@Transactional(readOnly = true)
public class MyWeekService {
    private final LevelRepository levels;
    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final Clock clock;
    private final ZoneId schoolZone;

    public MyWeekService(LevelRepository levels, SessionRepository sessions, AssignmentRepository assignments,
                         Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.levels = levels;
        this.sessions = sessions;
        this.assignments = assignments;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // The week containing date, or this week when date is null. Three queries whatever the number of sessions.
    public MyWeek week(UUID trainerId, LocalDate date) {
        Instant now = clock.instant();
        LocalDate from = SchoolWeek.saturdayOf(date != null ? date : now.atZone(schoolZone).toLocalDate());
        LocalDate to = SchoolWeek.fridayOf(from);

        List<LevelRef> legend = levels.findAll(Sort.by("id")).stream().map(LevelRef::of).toList();
        List<Session> inWeek = sessions.findScheduledLevelSessionsBetween(from, to);
        Map<UUID, List<Assignment>> seatsBySession = inWeek.isEmpty() ? Map.of()
                : assignments.findBySessionIdIn(inWeek.stream().map(Session::getId).toList()).stream()
                        .collect(Collectors.groupingBy(seat -> seat.getSession().getId()));

        // The query returns the sessions by date, start and level, and groupingBy keeps that order within a day.
        Map<LocalDate, List<WeekSession>> byDate = inWeek.stream()
                .collect(Collectors.groupingBy(Session::getSessionDate, Collectors.mapping(session -> {
                    SessionSeats seats = SessionSeatAssembler.seats(session,
                            seatsBySession.getOrDefault(session.getId(), List.of()), trainerId, now, schoolZone);
                    return new WeekSession(session.getId(), session.getTemplate().getLevel().getId(),
                            session.getStartTime(), session.getEndTime(), seats.capacity(), seats.booked(),
                            seats.mine(), seats.past(), seats.seats());
                }, Collectors.toList())));

        List<WeekDay> days = Stream.iterate(from, day -> day.plusDays(1))
                .limit(SchoolWeek.DAYS)
                .map(day -> new WeekDay(day, byDate.getOrDefault(day, List.of())))
                .toList();
        return new MyWeek(from, to, legend, days);
    }
}
