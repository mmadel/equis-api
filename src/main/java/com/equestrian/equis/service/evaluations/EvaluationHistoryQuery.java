package com.equestrian.equis.service.evaluations;

import com.equestrian.equis.dto.evaluations.RiderEvaluations;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.repository.evaluations.EvaluationHistoryRow;
import com.equestrian.equis.repository.evaluations.SessionEvaluationRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.riders.LevelChangeRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// A rider's evaluation history for Rider level (4a) and W3. Two queries: the rider, then the lines.
@Service
@Transactional(readOnly = true)
public class EvaluationHistoryQuery {
    private final RiderRepository riders;
    private final SessionEvaluationRepository evaluations;
    private final LevelChangeRules rules;

    public EvaluationHistoryQuery(RiderRepository riders, SessionEvaluationRepository evaluations,
                                  LevelChangeRules rules) {
        this.riders = riders;
        this.evaluations = evaluations;
        this.rules = rules;
    }

    // The rider's current trainer only, as the level history; the lines include earlier coaches' evaluations.
    public RiderEvaluations forTrainer(UUID riderId, UUID trainerId) {
        Rider rider = riders.findById(riderId).orElseThrow(rules::riderNotFound);
        rules.requireOwnRider(rider, trainerId);
        return build(rider);
    }

    // Any rider, closed people's included.
    public RiderEvaluations forAdmin(UUID riderId) {
        Rider rider = riders.findById(riderId).orElseThrow(rules::riderNotFound);
        return build(rider);
    }

    private RiderEvaluations build(Rider rider) {
        return new RiderEvaluations(rider.getId(), rider.getName(),
                evaluations.findHistory(rider.getId()).stream().map(EvaluationHistoryRow::toDto).toList());
    }
}
