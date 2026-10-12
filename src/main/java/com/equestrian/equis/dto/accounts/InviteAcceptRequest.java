package com.equestrian.equis.dto.accounts;

import jakarta.validation.constraints.NotBlank;

// The 8–72 rule on password comes from PasswordService.hash.
public record InviteAcceptRequest(@NotBlank String token, @NotBlank String password) {
    // Keeps the token and password out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "InviteAcceptRequest[]";
    }
}
