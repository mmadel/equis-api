package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.accounts.AdminStatus;
import com.equestrian.equis.entity.club.ClubStatus;
import com.equestrian.equis.entity.platform.BillingPeriod;
import com.equestrian.equis.service.platform.AccessReason;
import com.equestrian.equis.service.platform.AccessState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// One club, opened from P1: the row's fields plus contact details, suspension and the club's admins.
public record ClubDetail(UUID id, String code, String name, ClubStatus status,
                         AccessState accessState, AccessReason accessReason,
                         String planCode, String planName, BillingPeriod billingPeriod, Instant endsAt, int daysLeft,
                         int trainers, Integer maxTrainers, int riders, Integer maxRiders,
                         String contactPhone, String contactEmail, String address, Instant createdAt,
                         Instant suspendedAt, String suspendedReason, List<Admin> admins) {

    public record Admin(String name, String email, AdminStatus status) {
    }
}
