package com.equestrian.equis.dto.accounts;

import jakarta.validation.constraints.NotBlank;

// clubCode is the club the person signs in to; it is compared trimmed and lowercased.
// No 8–72 rule here: an old or seeded password must still sign in.
public record LoginRequest(@NotBlank String clubCode, @NotBlank String login, @NotBlank String password,
                           boolean rememberMe) {
    // Keeps the password out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "LoginRequest[clubCode=" + clubCode + ", login=" + login + ", rememberMe=" + rememberMe + "]";
    }
}
