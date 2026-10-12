package com.equestrian.equis.dto.accounts;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
    // Keeps the token out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "RefreshRequest[]";
    }
}
