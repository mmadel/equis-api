package com.equestrian.equis.service.sessions;

import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

// When a trainer may add riders to a session, and when they may finish it. Shared by the session screen,
// the assignment rules and the finish endpoint, so they always agree.
public final class SessionAccess {
    private SessionAccess() {
    }

    // Riders are added on the session's day (school time zone), also after the start, until it is finished.
    public static boolean canAssign(Session session, LocalDate today) {
        return session.getStatus() == SessionStatus.SCHEDULED && session.getSessionDate().equals(today);
    }

    // After the start, by a trainer with a rider in it, or by anyone when nobody is seated.
    public static boolean canFinish(Session session, Instant now, ZoneId zone, int mySeats, int totalSeats) {
        return session.getStatus() == SessionStatus.SCHEDULED
                && session.hasStarted(now, zone)
                && (mySeats > 0 || totalSeats == 0);
    }
}
