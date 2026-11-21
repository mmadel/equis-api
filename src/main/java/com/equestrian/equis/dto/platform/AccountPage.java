package com.equestrian.equis.dto.platform;

import java.util.List;

// One page of a club's accounts. page starts at 0. Same shape as ClubPage.
public record AccountPage(List<PlatformAccountRow> items, int page, int size, long totalItems) {
}
