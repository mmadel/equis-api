package com.equestrian.equis.service.sessions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// Seats taken in an assessment template's sessions. Implemented by service.assessment,
// so the session rules can count bookings without reaching into its repositories.
public interface AssessmentSeatCounts {
    // The UPCOMING booking count of each scheduled session after today/nowTime; sessions without bookings are absent.
    List<Long> countPerUpcomingSession(UUID templateId, LocalDate today, LocalTime nowTime);
}
