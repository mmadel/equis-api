package com.equestrian.equis.dto.assessment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// levelId is checked against attended in AssessmentRules.requireConsistentResult:
// a cross-field @AssertTrue getter on a record clashes with Jackson's property names.
public record RecordResultRequest(@NotNull Boolean attended,
                                  Integer levelId,
                                  @Size(max = 500) String notes) {
    public RecordResultRequest {
        notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
}
