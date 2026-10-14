package com.equestrian.equis.dto.assessment;

import java.time.LocalDate;
import java.util.List;

public record AssessmentSlotDayDto(LocalDate date, List<AssessmentSlotDto> slots) {
}
