package com.equestrian.equis.dto.platform;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A new club's first admin. No phone: club admins sign in with their email.
public record ClubAdminInput(@NotBlank @Size(max = 120) String name,
                             @NotBlank @Email @Size(max = 254) String email) {
}
