package com.equestrian.equis.dto.riders;

import com.equestrian.equis.dto.accounts.TrainerRef;
import com.equestrian.equis.dto.payments.BalanceDto;
import com.equestrian.equis.entity.riders.RiderAccountStatus;

import java.time.Instant;
import java.util.UUID;

// One row of W3. trainer and assessor are null when missing (seed riders have no assessor). howAssigned is
// "from_assessment", "changed_manually", "set_manually" or "unassigned"; riderSince is when the rider was created.
// balance is for today in the school time zone. accountStatus is the rider app account: none, invited or active.
public record AdminRiderRow(UUID id, String name, String phone, int levelId, TrainerRef trainer, TrainerRef assessor,
                            String howAssigned, boolean canResetToAssessor, Instant riderSince, BalanceDto balance,
                            RiderAccountStatus accountStatus) {
}
