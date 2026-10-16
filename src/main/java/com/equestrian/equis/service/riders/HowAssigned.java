package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.accounts.TrainerRef;

import java.util.Locale;

// How a rider came to their trainer, as W3 shows it. Worked out from the trainer and the assessor, never stored,
// so a rider moved away and back to the assessor shows FROM_ASSESSMENT again. The only place the rule lives.
public enum HowAssigned {
    FROM_ASSESSMENT,
    CHANGED_MANUALLY,
    SET_MANUALLY,
    UNASSIGNED;

    // trainer: null when the rider has none. assessor: null for seed riders (no placed booking).
    // Unassigned is the same rule as RiderRepository.findNeedingReassignment: no trainer, or a disabled one.
    public static HowAssigned of(TrainerRef trainer, TrainerRef assessor) {
        if (trainer == null || "disabled".equals(trainer.status())) {
            return UNASSIGNED;
        }
        if (assessor == null) {
            return SET_MANUALLY;
        }
        return trainer.id().equals(assessor.id()) ? FROM_ASSESSMENT : CHANGED_MANUALLY;
    }

    // The form sent to the console: "from_assessment", ...
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
