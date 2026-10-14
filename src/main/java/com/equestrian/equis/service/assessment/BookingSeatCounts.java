package com.equestrian.equis.service.assessment;

import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.service.sessions.AssessmentSeatCounts;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// An assessment slot's seats are its UPCOMING bookings; placed, no-show and cancelled ones free the seat.
@Component
public class BookingSeatCounts implements AssessmentSeatCounts {
    private final AssessmentBookingRepository bookings;

    public BookingSeatCounts(AssessmentBookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    public List<Long> countPerUpcomingSession(UUID templateId, LocalDate today, LocalTime nowTime) {
        return bookings.countUpcomingPerUpcomingSession(templateId, today, nowTime);
    }
}
