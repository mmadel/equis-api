package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.dto.evaluations.SeatEvaluation;
import com.equestrian.equis.dto.levels.LevelRef;
import com.equestrian.equis.dto.sessions.SessionDetailResponse;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.service.sessions.SessionDetail;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class SessionDetailMapper {
    private SessionDetailMapper() {
    }

    // Reads only the id of assignedBy, so the lazy trainer is never loaded.
    static SessionDetailResponse toResponse(SessionDetail detail, UUID trainerId) {
        Session session = detail.session();
        List<Rider> seatedByMe = detail.seats().stream()
                .filter(seat -> seat.getAssignedBy().getId().equals(trainerId))
                .map(Assignment::getRider)
                .sorted(Comparator.comparing(Rider::getName))
                .toList();
        Set<UUID> seatedIds = seatedByMe.stream().map(Rider::getId).collect(Collectors.toSet());

        return new SessionDetailResponse(session.getId(), LevelRef.of(session.getTemplate().getLevel()),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(), session.getCapacity(),
                session.getStatus().name().toLowerCase(Locale.ROOT), session.getFinishedAt(),
                detail.canAssign(), detail.canFinish(), detail.readOnly(),
                detail.seats().size() - seatedByMe.size(),
                seatedByMe.stream()
                        .map(rider -> new SessionDetailResponse.MySeat(rider.getId(), rider.getName(),
                                rider.initials(), detail.sessionsLeft(rider.getId()),
                                Optional.ofNullable(detail.evaluation(rider.getId())).map(SeatEvaluation::of)
                                        .orElse(null),
                                detail.canEvaluate(rider.getId())))
                        .toList(),
                detail.eligibleRiders().stream()
                        .map(rider -> new SessionDetailResponse.RiderOption(rider.getId(), rider.getName(),
                                rider.initials(), seatedIds.contains(rider.getId()),
                                detail.sessionsLeft(rider.getId())))
                        .toList());
    }
}
