package com.equestrian.equis.dto.assessment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

// No longer sent by any endpoint (POST /me/members answers 410); kept for MemberBookingService until the
// admin intake replaces both. The constructor cleans the input before validation runs.
public record NewMemberRequest(@NotBlank @Size(max = 100) String name,
                               @NotBlank @Pattern(regexp = "^0[0-9]{10}$") String phone,
                               @NotBlank @Email @Size(max = 254) String email,
                               @NotNull UUID slotId) {
    public NewMemberRequest {
        name = name == null ? null : name.trim();
        phone = phone == null ? null : phone.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
