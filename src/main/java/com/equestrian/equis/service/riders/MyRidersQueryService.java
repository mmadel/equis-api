package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.riders.MyRiderDto;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Read side of My riders (screen 4).
@Service
@Transactional(readOnly = true)
public class MyRidersQueryService {
    private final RiderRepository riders;
    private final AssignmentRepository assignments;
    private final Clock clock;
    private final ZoneId schoolZone;

    public MyRidersQueryService(RiderRepository riders, AssignmentRepository assignments, Clock clock,
                                @Value("${app.time-zone}") ZoneId schoolZone) {
        this.riders = riders;
        this.assignments = assignments;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    // All the trainer's riders, by level then name, each with their next upcoming seat. Two queries whatever the count.
    public List<MyRiderDto> myRiders(UUID trainerId) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, schoolZone);

        List<Rider> mine = riders.findMine(trainerId);
        Map<UUID, UpcomingSeat> nextByRider = new HashMap<>();
        // The seats come in time order, so the first upcoming one per rider is the next.
        for (Assignment seat : assignments.findFromDateForTrainer(trainerId, today)) {
            if (seat.getSession().isUpcoming(now, schoolZone)) {
                nextByRider.putIfAbsent(seat.getRider().getId(), UpcomingSeat.of(seat.getSession()));
            }
        }

        return mine.stream()
                .map(rider -> new MyRiderDto(rider.getId(), rider.getName(), rider.initials(),
                        rider.getLevel().getId(), LocalDate.ofInstant(rider.getCreatedAt(), schoolZone),
                        nextByRider.get(rider.getId())))
                .toList();
    }
}
