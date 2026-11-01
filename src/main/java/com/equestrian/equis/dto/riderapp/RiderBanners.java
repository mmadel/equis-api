package com.equestrian.equis.dto.riderapp;

// The Home warnings. expiringSoon is null when no sessions expire within 7 days.
public record RiderBanners(ExpiringSoon expiringSoon, boolean zeroBalance) {
}
