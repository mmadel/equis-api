package com.equestrian.equis.dto.riders;

import com.equestrian.equis.entity.riders.RiderAccountStatus;

import java.time.Instant;
import java.util.UUID;

// The answer to W3's Send app invite and Reset app password. linkPurpose is "invite" or "reset";
// linkExpiresAt is when the link just sent stops working.
public record RiderAccount(UUID riderId, RiderAccountStatus accountStatus, String linkPurpose, Instant linkExpiresAt) {
}
