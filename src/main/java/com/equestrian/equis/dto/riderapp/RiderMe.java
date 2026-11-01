package com.equestrian.equis.dto.riderapp;

import com.equestrian.equis.entity.payments.RiderPaymentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// The rider app's Home and Profile: who the rider is and their balance today. trainerName and nextExpiry can be null.
public record RiderMe(UUID id, String name, String phone, String email, int levelId, String trainerName,
                      RiderPaymentStatus status, int sessionsLeft, LocalDate nextExpiry,
                      List<RiderPackage> packages, RiderBanners banners) {
}
