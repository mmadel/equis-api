package com.equestrian.equis.dto.accounts;

import com.equestrian.equis.common.phone.PhoneNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

// The constructor cleans the input before validation runs: "+20 101 234 5678" is a valid phone.
public record InviteTrainerRequest(@NotBlank @Size(max = 100) String name,
                                   @NotBlank @Pattern(regexp = PhoneNormalizer.LOCAL_FORMAT) String phone,
                                   @NotBlank @Email @Size(max = 254) String email) {
    public InviteTrainerRequest {
        name = name == null ? null : name.trim();
        phone = PhoneNormalizer.cleanForValidation(phone);
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
