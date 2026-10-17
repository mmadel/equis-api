package com.equestrian.equis.dto.overview;

import com.equestrian.equis.dto.assessment.AdminBookingRow;

import java.util.List;

// What needs the admin. overdueResults is the oldest few of the overdueResultsCount W2 rows waiting for a result.
public record Attention(long overdueResultsCount,
                        List<AdminBookingRow> overdueResults,
                        long ridersWithoutTrainer,
                        List<TrainerRiderCount> disabledTrainersWithRiders,
                        List<PendingInvite> pendingInvites) {
}
