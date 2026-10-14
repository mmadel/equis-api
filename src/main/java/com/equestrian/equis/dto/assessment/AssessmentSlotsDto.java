package com.equestrian.equis.dto.assessment;

import java.time.LocalDate;
import java.util.List;

// from and to are the range actually used, after defaults. Days without a slot are left out.
public record AssessmentSlotsDto(LocalDate from, LocalDate to, List<AssessmentSlotDayDto> days) {
}
