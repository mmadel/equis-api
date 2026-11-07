package com.equestrian.equis.dto.accounts;

import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.club.Club;

import java.util.UUID;

// expiresIn is the access token lifetime in seconds. club is set by the club sign-in only (null, so left out, for
// the super admin and on refresh).
public record AuthTokens(String accessToken, long expiresIn, String refreshToken, Role role, ClubRef club) {
    public AuthTokens(String accessToken, long expiresIn, String refreshToken, Role role) {
        this(accessToken, expiresIn, refreshToken, role, null);
    }

    public AuthTokens withClub(Club club) {
        return new AuthTokens(accessToken, expiresIn, refreshToken, role, ClubRef.of(club));
    }

    // Keeps both tokens out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "AuthTokens[expiresIn=" + expiresIn + ", role=" + role + ", club=" + club + "]";
    }

    public record ClubRef(UUID id, String code, String name) {
        static ClubRef of(Club club) {
            return new ClubRef(club.getId(), club.getCode(), club.getName());
        }
    }
}
