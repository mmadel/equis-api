package com.equestrian.equis.dto.accounts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

// The constructor cleans the input before validation runs: "010 1234 5678" is a valid phone.
public record InviteTrainerRequest(@NotBlank @Size(max = 100) String name,
                                   @NotBlank @Pattern(regexp = "^0[0-9]{10}$") String phone,
                                   @NotBlank @Email @Size(max = 254) String email) {
    public InviteTrainerRequest {
        name = name == null ? null : name.trim();
        phone = phone == null ? null : phone.replaceAll("\\s", "");
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
