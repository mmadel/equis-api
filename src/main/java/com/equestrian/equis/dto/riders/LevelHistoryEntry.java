package com.equestrian.equis.dto.riders;

import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.riders.LevelChange;

import java.time.Instant;

// One line of the 4a level history. type is "promoted", "demoted" or "placed";
// fromLevelId and reason are null only for "placed". byName is the trainer who made the change.
public record LevelHistoryEntry(String type, Integer fromLevelId, int toLevelId, String reason,
                                String byName, Instant at) {
    public static final String PROMOTED = "promoted";
    public static final String DEMOTED = "demoted";
    public static final String PLACED = "placed";

    // The trainer must be loaded; the levels may be references.
    public static LevelHistoryEntry of(LevelChange c) {
        return new LevelHistoryEntry(c.isPromotion() ? PROMOTED : DEMOTED,
                (int) c.getFromLevel().getId(), c.getToLevel().getId(), c.getReason(),
                c.getByTrainer().getName(), c.getCreatedAt());
    }

    // The assessor and level of a placed booking must be loaded.
    public static LevelHistoryEntry placed(AssessmentBooking b) {
        return new LevelHistoryEntry(PLACED, null, b.getLevel().getId(), null,
                b.getAssessor().getName(), b.getRecordedAt());
    }
}
