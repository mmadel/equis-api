package com.equestrian.equis.dto.platform;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// The super admin's sign-in: email only, no club code. No 8–72 rule, as for LoginRequest.
public record PlatformLoginRequest(@NotBlank @Email String email, @NotBlank String password, boolean rememberMe) {
    // Keeps the password out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "PlatformLoginRequest[email=" + email + ", rememberMe=" + rememberMe + "]";
    }
}
