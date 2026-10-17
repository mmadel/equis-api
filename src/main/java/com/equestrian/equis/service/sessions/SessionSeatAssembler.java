package com.equestrian.equis.service.sessions;

import com.equestrian.equis.dto.levels.LevelSessionsResponse.Seat;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// Turns a session and its assignments into the seats a trainer sees. Shared by GET /me/levels/{id}/sessions
// and GET /me/week, so both show a session the same way.
public final class SessionSeatAssembler {
    private SessionSeatAssembler() {
    }

    public static SessionSeats seats(Session session, List<Assignment> assignments, UUID trainerId,
                                     Instant now, ZoneId schoolZone) {
        List<Seat> seats = seats(assignments, trainerId);
        int mine = (int) seats.stream().filter(Seat::mine).count();
        return new SessionSeats(session.getCapacity(), assignments.size(), mine,
                !session.isUpcoming(now, schoolZone), seats);
    }

    // The caller's seats first, by initials; then one anonymous seat per rider of another trainer.
    private static List<Seat> seats(List<Assignment> assignments, UUID trainerId) {
        return assignments.stream()
                .map(seat -> seat.getAssignedBy().getId().equals(trainerId)
                        ? new Seat(true, seat.getRider().getId(), seat.getRider().initials())
                        : Seat.other())
                .sorted(Comparator.comparing(Seat::mine).reversed()
                        .thenComparing(Seat::initials, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
