package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssignmentService {
    private final SessionRepository sessions;
    private final AssignmentRepository assignments;
    private final TrainerRepository trainers;
    private final AssignmentRules rules;

    public AssignmentService(SessionRepository sessions, AssignmentRepository assignments,
                             TrainerRepository trainers, AssignmentRules rules) {
        this.sessions = sessions;
        this.assignments = assignments;
        this.trainers = trainers;
        this.rules = rules;
    }

    // riderIds becomes the trainer's whole list for the session; seats given by other trainers are not touched.
    @Transactional
    public Session replaceMine(UUID trainerId, UUID sessionId, List<UUID> riderIds) {
        // The lock makes the seat count below safe against a second trainer saving at the same time.
        Session session = sessions.findByIdForUpdate(sessionId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "This session does not exist."));

        List<Assignment> seats = assignments.findBySessionIdIn(List.of(sessionId));
        Set<UUID> requested = new LinkedHashSet<>(riderIds);
        List<Rider> riders = rules.check(session, trainerId, requested, seats);

        List<Assignment> mine = seats.stream()
                .filter(seat -> seat.getAssignedBy().getId().equals(trainerId))
                .toList();
        Set<UUID> seated = mine.stream().map(seat -> seat.getRider().getId()).collect(Collectors.toSet());

        assignments.deleteAll(mine.stream()
                .filter(seat -> !requested.contains(seat.getRider().getId()))
                .toList());

        Trainer trainer = trainers.getReferenceById(trainerId);
        assignments.saveAll(riders.stream()
                .filter(rider -> !seated.contains(rider.getId()))
                .map(rider -> Assignment.of(session, rider, trainer))
                .toList());

        return session;
    }
}
