package com.equestrian.equis.dto.riders;

import com.equestrian.equis.dto.accounts.TrainerRef;

import java.time.Instant;
import java.util.UUID;

// One row of W3. trainer and assessor are null when missing (seed riders have no assessor). howAssigned is
// "from_assessment", "changed_manually", "set_manually" or "unassigned"; riderSince is when the rider was created.
public record AdminRiderRow(UUID id, String name, String phone, int levelId, TrainerRef trainer, TrainerRef assessor,
                            String howAssigned, boolean canResetToAssessor, Instant riderSince) {
}
