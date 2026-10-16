package com.equestrian.equis.dto.riders;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

// W3's trainer dropdown, bulk bar and Reset to assessor. Exactly one of trainerId and resetToAssessor: true;
// the service checks that. Duplicate ids are ignored.
public record ReassignRidersRequest(@NotEmpty @Size(max = 200) List<@NotNull UUID> riderIds,
                                    UUID trainerId,
                                    Boolean resetToAssessor) {
}
