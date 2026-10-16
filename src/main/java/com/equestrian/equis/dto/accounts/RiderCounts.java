package com.equestrian.equis.dto.accounts;

import java.util.List;

// byLevel lists only levels with at least one rider, by level id.
public record RiderCounts(long total, List<LevelCount> byLevel) {
}
