package com.equestrian.equis.dto.riderapp;

import java.util.List;

// One page of rides, newest first. page starts at 0.
public record RiderSessionPage(List<RiderSession> items, int page, int size, long totalItems) {
}
