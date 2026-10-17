package com.equestrian.equis.dto.overview;

import com.equestrian.equis.dto.accounts.TrainerRef;

import java.time.Instant;

// Something a trainer did. kind is "placed", "noshow", "promoted" or "demoted"; actor is the assessor for a result,
// the trainer who changed the level otherwise; subject is the member's or rider's name.
// toLevel is set for placed, promoted and demoted; fromLevel only for promoted and demoted.
public record ActivityItem(String kind, Instant at, TrainerRef actor, String subject, Integer fromLevel,
                           Integer toLevel) {
}
