package com.equestrian.equis.dto.platform;

import java.util.List;

// One page of P1's club list. page starts at 0.
public record ClubPage(List<ClubRow> items, int page, int size, long totalItems) {
}
