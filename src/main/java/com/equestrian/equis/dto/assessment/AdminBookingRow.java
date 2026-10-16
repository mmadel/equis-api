package com.equestrian.equis.dto.assessment;

import com.equestrian.equis.dto.accounts.TrainerRef;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// One row of W2: a member's latest booking. status is "upcoming", "waiting", "placed", "noshow" or "cancelled";
// riderId is set only when placed. actions are what the admin may do on the row ("reschedule", "remind", ...).
public record AdminBookingRow(UUID id,
                              MemberRef member,
                              SlotRef slot,
                              TrainerRef assessor,
                              String status,
                              Integer levelId,
                              String notes,
                              Instant recordedAt,
                              UUID riderId,
                              Instant remindedAt,
                              List<String> actions,
                              Instant createdAt) {
}
