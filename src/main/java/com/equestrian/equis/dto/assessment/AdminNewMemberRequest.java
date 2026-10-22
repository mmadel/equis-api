package com.equestrian.equis.dto.assessment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

// W2's New person: the person's details, their first assessment slot and the trainer who runs it.
// The constructor cleans the input before validation runs.
public record AdminNewMemberRequest(@NotBlank @Size(max = 100) String name,
                                    @NotBlank @Pattern(regexp = "^0[0-9]{10}$") String phone,
                                    @NotBlank @Email @Size(max = 254) String email,
                                    @NotNull UUID slotId,
                                    @NotNull UUID assessorId) {
    public AdminNewMemberRequest {
        name = name == null ? null : name.trim();
        phone = phone == null ? null : phone.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
