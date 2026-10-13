package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
public class SessionTemplateRules {
    // ck_session_template_capacity
    static final int MIN_CAPACITY = 1;
    static final int MAX_CAPACITY = 12;

    private final LevelRepository levelRepository;
    private final SessionTemplateRepository sessionTemplateRepository;
    private final BookedSeats bookedSeats;

    public SessionTemplateRules(LevelRepository levelRepository, SessionTemplateRepository sessionTemplateRepository,
                                BookedSeats bookedSeats) {
        this.levelRepository = levelRepository;
        this.sessionTemplateRepository = sessionTemplateRepository;
        this.bookedSeats = bookedSeats;
    }

    // existing is the template being edited, or null on create.
    // Errors are thrown; warnings are returned and do not stop the save.
    public List<Warning> check(SessionTemplateInput in, SessionTemplate existing) {
        Short levelId = levelId(in);

        if (!in.end().isAfter(in.start())) {
            throw invalid("end", "End time must be after the start time.");
        }

        if (in.capacity() < MIN_CAPACITY || in.capacity() > MAX_CAPACITY) {
            throw invalid("capacity", "Capacity must be between " + MIN_CAPACITY + " and " + MAX_CAPACITY + ".");
        }

        if (existing != null) {
            int booked = bookedSeats.maxBookedInUpcoming(existing.getId());
            if (in.capacity() < booked) {
                throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.CAPACITY_EXCEEDED,
                        "An upcoming session already has " + booked + " riders booked.", Map.of("booked", booked));
            }
        }

        List<OverlapRef> overlaps = overlaps(in, levelId, existing);
        return overlaps.isEmpty() ? List.of() : List.of(Warning.overlap(overlaps));
    }

    // Two time ranges overlap when each starts before the other ends; ranges that only touch do not.
    static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    // Null stays null: an assessment slot has no level to look up.
    private Short levelId(SessionTemplateInput in) {
        if (in.levelId() == null) {
            return null;
        }
        int id = in.levelId();
        if (id < Short.MIN_VALUE || id > Short.MAX_VALUE || !levelRepository.existsById((short) id)) {
            throw invalid("levelId", "This level does not exist.");
        }
        return (short) id;
    }

    // Active templates of the same level (or assessment) on the same day, other than the one being edited.
    private List<OverlapRef> overlaps(SessionTemplateInput in, Short levelId, SessionTemplate existing) {
        List<SessionTemplate> sameLevel = levelId == null
                ? sessionTemplateRepository.findByLevelIsNullAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc()
                : sessionTemplateRepository.findByLevelIdAndDeletedAtIsNullOrderByDayOfWeekAscStartTimeAsc(levelId);
        return sameLevel.stream()
                .filter(t -> existing == null || !t.getId().equals(existing.getId()))
                .filter(t -> t.getDayOfWeek() == in.dayOfWeek())
                // A template that ended before this one begins never shares a date with it.
                .filter(t -> t.getValidTo() == null || !t.getValidTo().isBefore(in.validFrom()))
                .filter(t -> overlaps(in.start(), in.end(), t.getStartTime(), t.getEndTime()))
                .map(t -> new OverlapRef(t.getId(), t.getStartTime(), t.getEndTime()))
                .toList();
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
