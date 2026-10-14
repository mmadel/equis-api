package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.time.DateRanges;
import com.equestrian.equis.dto.assessment.AssessmentSlotDayDto;
import com.equestrian.equis.dto.assessment.AssessmentSlotDto;
import com.equestrian.equis.dto.assessment.AssessmentSlotsDto;
import com.equestrian.equis.dto.assessment.BookingRowDto;
import com.equestrian.equis.dto.assessment.SlotDetailDto;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.SlotBookedCount;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Read side of the trainer's assessment screens: the slot list (A1, A2) and one slot (A3, A4).
@Service
@Transactional(readOnly = true)
public class AssessmentSlotQueryService {
    // Assessment slots are few, so the default is two weeks rather than E3's one, to give A1 an open slot to pick.
    private static final int DEFAULT_RANGE_DAYS = 14;
    // The generation horizon, as for level sessions.
    private static final int MAX_RANGE_DAYS = 56;

    private final SessionRepository sessions;
    private final AssessmentBookingRepository bookings;
    private final AssessmentRules rules;
    private final AssessmentSlotMapper mapper;
    private final Clock clock;
    private final ZoneId schoolZone;

    public AssessmentSlotQueryService(SessionRepository sessions, AssessmentBookingRepository bookings,
                                      AssessmentRules rules, AssessmentSlotMapper mapper, Clock clock,
                                      @Value("${app.time-zone}") ZoneId schoolZone) {
        this.sessions = sessions;
        this.bookings = bookings;
        this.rules = rules;
        this.mapper = mapper;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // Scheduled assessment slots from..to (both included), grouped by date. Started slots are listed, not open.
    // from may be in the past, so A2 can reach slots still waiting for results. Two queries whatever the range.
    public AssessmentSlotsDto slots(LocalDate from, LocalDate to) {
        Instant now = clock.instant();
        if (from == null) {
            from = now.atZone(schoolZone).toLocalDate();
        }
        if (to == null) {
            to = from.plusDays(DEFAULT_RANGE_DAYS - 1);
        }
        DateRanges.require(from, to, MAX_RANGE_DAYS);

        List<Session> inRange = sessions.findAssessmentSlots(from, to);
        Map<UUID, Long> bookedBySlot = inRange.isEmpty() ? Map.of()
                : bookings.countActiveBySessionIdIn(inRange.stream().map(Session::getId).toList()).stream()
                        .collect(Collectors.toMap(SlotBookedCount::getSessionId, SlotBookedCount::getBooked));

        // The query is already in date and time order.
        Map<LocalDate, List<AssessmentSlotDto>> byDate = new LinkedHashMap<>();
        for (Session slot : inRange) {
            byDate.computeIfAbsent(slot.getSessionDate(), date -> new ArrayList<>())
                    .add(mapper.toDto(slot, bookedBySlot.getOrDefault(slot.getId(), 0L), now));
        }

        List<AssessmentSlotDayDto> days = byDate.entrySet().stream()
                .map(entry -> new AssessmentSlotDayDto(entry.getKey(), entry.getValue()))
                .toList();
        return new AssessmentSlotsDto(from, to, days);
    }

    // One assessment slot with its bookings, for any trainer. Missing, level and cancelled slots are all 404.
    public SlotDetailDto slot(UUID slotId, UUID trainerId) {
        Session slot = sessions.findById(slotId).orElseThrow(rules::slotNotFound);
        rules.requireAssessmentSlot(slot);
        if (slot.getStatus() == SessionStatus.CANCELLED) {
            throw rules.slotNotFound();
        }

        List<AssessmentBooking> visible = bookings.findVisibleBySessionId(slotId);
        Instant now = clock.instant();
        List<BookingRowDto> rows = visible.stream()
                .map(b -> mapper.toRow(b, trainerId, now))
                .toList();
        return SlotDetailDto.of(mapper.toDto(slot, visible.size(), now), mapper.isStarted(slot, now), rows);
    }
}
