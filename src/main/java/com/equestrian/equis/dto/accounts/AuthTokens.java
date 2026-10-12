package com.equestrian.equis.dto.accounts;

import com.equestrian.equis.common.security.Role;

// expiresIn is the access token lifetime in seconds.
public record AuthTokens(String accessToken, long expiresIn, String refreshToken, Role role) {
    // Keeps both tokens out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "AuthTokens[expiresIn=" + expiresIn + ", role=" + role + "]";
    }
}
