package com.equestrian.equis.dto.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// The constructor cleans the input before validation runs, so a blank phone means "no phone".
public record NewMemberRequest(@NotBlank @Size(max = 100) String name,
                               @Pattern(regexp = "^0[0-9]{10}$") String phone,   // null passes
                               @NotNull UUID slotId) {
    public NewMemberRequest {
        name = name == null ? null : name.trim();
        phone = phone == null || phone.isBlank() ? null : phone.trim();
    }
}
