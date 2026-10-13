package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AssignmentRules {
    private final RiderRepository riders;
    private final Clock clock;
    private final ZoneId schoolZone;

    public AssignmentRules(RiderRepository riders, Clock clock, @Value("${app.time-zone}") ZoneId schoolZone) {
        this.riders = riders;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // riderIds is the full list the trainer wants seated; seats are all the session's current assignments.
    // The first failing rule throws. Returns the requested riders, in the order asked.
    public List<Rider> check(Session session, UUID trainerId, Set<UUID> riderIds, List<Assignment> seats) {
        if (session.getStatus() != SessionStatus.SCHEDULED || !session.isUpcoming(clock.instant(), schoolZone)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.SESSION_CLOSED,
                    "This session is cancelled or has already started.");
        }

        Map<UUID, Rider> found = riders.findAllById(riderIds).stream()
                .collect(Collectors.toMap(Rider::getId, Function.identity()));

        // An unknown id is answered like someone else's rider, so ids cannot be probed.
        List<UUID> notMine = riderIds.stream()
                .filter(id -> !isTrainedBy(found.get(id), trainerId))
                .toList();
        if (!notMine.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.NOT_YOUR_RIDER,
                    "You can only assign your own riders.", fieldErrors(notMine, "This rider is not yours."));
        }

        // An assessment slot has no level, so no rider matches it.
        Level level = session.getTemplate().getLevel();
        List<UUID> wrongLevel = riderIds.stream()
                .filter(id -> level == null || !found.get(id).getLevel().getId().equals(level.getId()))
                .toList();
        if (!wrongLevel.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.LEVEL_OUT_OF_RANGE,
                    "Some riders are not in this session's level.",
                    fieldErrors(wrongLevel, "This rider is not in the session's level."));
        }

        List<Assignment> othersSeats = seats.stream()
                .filter(seat -> !seat.getAssignedBy().getId().equals(trainerId))
                .toList();

        Set<UUID> seatedByOthers = othersSeats.stream()
                .map(seat -> seat.getRider().getId())
                .collect(Collectors.toSet());
        List<UUID> alreadyBooked = riderIds.stream().filter(seatedByOthers::contains).toList();
        if (!alreadyBooked.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.ALREADY_BOOKED,
                    "Some riders already have a seat in this session.",
                    fieldErrors(alreadyBooked, "This rider already has a seat in this session."));
        }

        int free = session.getCapacity() - othersSeats.size();
        if (riderIds.size() > free) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.CAPACITY_EXCEEDED,
                    "This session has " + Math.max(free, 0) + " seats left for your riders.",
                    Map.of("capacity", session.getCapacity(), "free", Math.max(free, 0)));
        }

        return riderIds.stream().map(found::get).toList();
    }

    private static boolean isTrainedBy(Rider rider, UUID trainerId) {
        return rider != null && rider.getTrainer() != null && rider.getTrainer().getId().equals(trainerId);
    }

    // One entry per rider; field is the rider id.
    private static Map<String, Object> fieldErrors(Collection<UUID> riderIds, String message) {
        return Map.of("fieldErrors", riderIds.stream()
                .map(id -> new ApiFieldError(id.toString(), message))
                .toList());
    }
}
