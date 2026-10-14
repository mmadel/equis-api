package com.equestrian.equis.service.assessment;

import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.service.sessions.SessionsCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Cancels the bookings of cancelled assessment slots.
// Synchronous, so it runs inside the transaction that cancelled them: if it fails, the slot delete rolls back too.
@Component
public class BookingCancellationListener {
    private static final Logger log = LoggerFactory.getLogger(BookingCancellationListener.class);

    private final AssessmentBookingRepository bookings;

    public BookingCancellationListener(AssessmentBookingRepository bookings) {
        this.bookings = bookings;
    }

    @EventListener
    public void onSessionsCancelled(SessionsCancelledEvent event) {
        List<AssessmentBooking> lost = bookings.findBySessionIdInAndStatus(event.sessionIds(), BookingStatus.UPCOMING);
        if (lost.isEmpty()) {
            return;
        }
        // One by one, not a bulk update, so @Version and updatedAt stay correct.
        for (AssessmentBooking booking : lost) {
            booking.cancel(event.cancelledAt());
        }
        // Logged until the Notice listener exists to tell these assessors.
        Set<UUID> assessorIds = assessorIds(lost);
        log.info("Cancelled {} assessment bookings in {} cancelled sessions of template {}; affected assessors: {}",
                lost.size(), event.sessionIds().size(), event.templateId(), assessorIds);
    }

    // The distinct assessors whose bookings were cancelled.
    static Set<UUID> assessorIds(List<AssessmentBooking> lost) {
        Set<UUID> assessorIds = new LinkedHashSet<>();
        for (AssessmentBooking booking : lost) {
            assessorIds.add(booking.getAssessor().getId());
        }
        return assessorIds;
    }
}
