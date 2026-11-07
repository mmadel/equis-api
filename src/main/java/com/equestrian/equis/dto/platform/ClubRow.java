package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.club.ClubStatus;
import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.service.platform.AccessReason;
import com.equestrian.equis.service.platform.AccessState;

import java.time.Instant;
import java.util.UUID;

// One club on P1. The plan fields and endsAt are null when the club has no current subscription; daysLeft is then 0.
// accessReason is null when ACTIVE. A null max means unlimited (or no plan).
public record ClubRow(UUID id, String code, String name, ClubStatus status,
                      AccessState accessState, AccessReason accessReason,
                      String planCode, String planName, BillingPeriod billingPeriod, Instant endsAt, int daysLeft,
                      int trainers, Integer maxTrainers, int riders, Integer maxRiders) {
}
