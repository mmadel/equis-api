package com.equestrian.equis.dto.platform;

import java.util.List;

// One page of a club's account activity, newest first. page starts at 0. Same shape as AccountPage and ClubPage.
public record AccountActivityPage(List<AccountActivityRow> items, int page, int size, long totalItems) {
}
