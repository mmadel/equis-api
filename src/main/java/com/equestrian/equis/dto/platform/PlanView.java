package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.platform.OnExpiry;

// One plan for the P3 plan picker. Null limits mean unlimited; trialDays is set only on the trial plan.
public record PlanView(String code, String name, Integer maxTrainers, Integer maxRiders, Integer trialDays,
                       OnExpiry onExpiry) {
}
