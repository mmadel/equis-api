package com.equestrian.equis.dto.accounts;

import jakarta.validation.constraints.NotBlank;

// No 8–72 rule here: an old or seeded password must still sign in.
public record LoginRequest(@NotBlank String login, @NotBlank String password, boolean rememberMe) {
    // Keeps the password out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", rememberMe=" + rememberMe + "]";
    }
}
