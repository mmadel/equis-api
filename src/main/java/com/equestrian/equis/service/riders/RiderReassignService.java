package com.equestrian.equis.service.riders;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riders.AdminRiderRow;
import com.equestrian.equis.dto.riders.ReassignResult;
import com.equestrian.equis.dto.riders.ReassignRidersRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.AdminRiderView;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

// W3: gives riders to another trainer, or back to the assessor who placed them. All or nothing. Only the rider's
// trainer changes: the level stays, and seats in upcoming sessions stay as they are (a seat belongs to the trainer
// who gave it). No history is kept and no notice is sent.
@Service
public class RiderReassignService {
    private final RiderRepository riders;
    private final TrainerRepository trainers;
    private final AdminRiderRowAssembler rows;

    public RiderReassignService(RiderRepository riders, TrainerRepository trainers, AdminRiderRowAssembler rows) {
        this.riders = riders;
        this.trainers = trainers;
        this.rows = rows;
    }

    // Locks trainers first (the target, or the assessors in id order), then the riders in id order. Disabling a
    // trainer locks the same trainer row, so a rider is never given to a trainer being disabled at that moment.
    @Transactional
    public ReassignResult reassign(ReassignRidersRequest req) {
        boolean reset = Boolean.TRUE.equals(req.resetToAssessor());
        if (reset == (req.trainerId() != null)) {
            throw invalidField("trainerId", ErrorCodes.TRAINER_OR_RESET_REQUIRED);
        }
        Set<UUID> requested = new LinkedHashSet<>(req.riderIds());
        List<UUID> ids = new ArrayList<>(new TreeSet<>(requested));

        Trainer target = reset ? null : lockActive(req.trainerId());
        Map<UUID, Trainer> assessorOf = reset ? lockActiveAssessors(ids) : Map.of();

        List<Rider> locked = riders.findAllByIdInForUpdate(ids);
        requireAllFound(ids, locked.stream().map(Rider::getId).toList());

        int updated = 0;
        for (Rider r : locked) {
            if (r.assignTrainer(reset ? assessorOf.get(r.getId()) : target)) {
                updated++;
            }
        }
        // Built after the flush, so the rows show the new trainer and howAssigned.
        riders.flush();
        return new ReassignResult(updated, ids.size() - updated, inOrder(requested, rows.rowsByIds(ids)));
    }

    private Trainer lockActive(UUID trainerId) {
        Trainer t = trainers.findByIdForUpdate(trainerId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.TRAINER_NOT_FOUND, "Trainer not found.",
                Map.of("fieldErrors", List.of(new ApiFieldError("trainerId", ErrorCodes.TRAINER_NOT_FOUND)))));
        if (t.getStatus() != TrainerStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.TRAINER_NOT_ACTIVE,
                    t.getName() + " is not an active trainer.",
                    Map.of("fieldErrors", List.of(new ApiFieldError("trainerId", ErrorCodes.TRAINER_NOT_ACTIVE))));
        }
        return t;
    }

    // Each rider's assessor by rider id, locked in id order and checked active. The riders are read as a projection,
    // not loaded as entities, so the rider lock that follows reads them fresh.
    private Map<UUID, Trainer> lockActiveAssessors(List<UUID> ids) {
        List<AdminRiderView> views = riders.findAdminViewByIds(BookingStatus.PLACED, ids);
        requireAllFound(ids, views.stream().map(AdminRiderView::id).toList());

        Map<UUID, UUID> assessorIdOf = new HashMap<>();
        views.stream().filter(v -> v.assessorId() != null).forEach(v -> assessorIdOf.put(v.id(), v.assessorId()));
        Map<UUID, Trainer> assessors = new HashMap<>();
        for (UUID a : new TreeSet<>(assessorIdOf.values())) {
            assessors.put(a, trainers.findByIdForUpdate(a).orElseThrow());
        }

        Map<UUID, Trainer> assessorOf = new HashMap<>();
        List<UUID> bad = new ArrayList<>();
        for (UUID id : ids) {
            UUID assessorId = assessorIdOf.get(id);
            Trainer a = assessorId == null ? null : assessors.get(assessorId);
            if (a == null || a.getStatus() != TrainerStatus.ACTIVE) {
                bad.add(id);
            } else {
                assessorOf.put(id, a);
            }
        }
        if (!bad.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.NO_ACTIVE_ASSESSOR,
                    bad.size() + (bad.size() == 1 ? " rider has" : " riders have")
                            + " no active assessor to go back to.",
                    Map.of("riderIds", bad));
        }
        return assessorOf;
    }

    private static void requireAllFound(List<UUID> ids, Collection<UUID> found) {
        Set<UUID> present = Set.copyOf(found);
        List<UUID> missing = ids.stream().filter(id -> !present.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND,
                    missing.size() + (missing.size() == 1 ? " rider" : " riders") + " not found.",
                    Map.of("riderIds", missing));
        }
    }

    // The rows in the order the ids were sent.
    private static List<AdminRiderRow> inOrder(Set<UUID> requested, List<AdminRiderRow> found) {
        Map<UUID, AdminRiderRow> byId = new HashMap<>();
        found.forEach(r -> byId.put(r.id(), r));
        return requested.stream().map(byId::get).toList();
    }

    // Answered in the same 400 shape as a failed @Valid.
    private static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError(field, message))));
    }
}
