package com.equestrian.equis.dto.accounts;

import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.service.platform.AccessReason;
import com.equestrian.equis.service.platform.AccessState;
import com.equestrian.equis.service.platform.ClubAccess;

import java.time.Instant;
import java.util.UUID;

// expiresIn is the access token lifetime in seconds. club and access are set by the club sign-in only (null, so
// left out, for the super admin and on refresh).
public record AuthTokens(String accessToken, long expiresIn, String refreshToken, Role role, ClubRef club,
                         Access access) {
    public AuthTokens(String accessToken, long expiresIn, String refreshToken, Role role) {
        this(accessToken, expiresIn, refreshToken, role, null, null);
    }

    public AuthTokens withClub(Club club) {
        return new AuthTokens(accessToken, expiresIn, refreshToken, role, ClubRef.of(club), access);
    }

    public AuthTokens withAccess(ClubAccess clubAccess) {
        return new AuthTokens(accessToken, expiresIn, refreshToken, role, club, Access.of(clubAccess));
    }

    // Keeps both tokens out of logs if the record is ever printed.
    @Override
    public String toString() {
        return "AuthTokens[expiresIn=" + expiresIn + ", role=" + role + ", club=" + club + ", access=" + access + "]";
    }

    public record ClubRef(UUID id, String code, String name) {
        static ClubRef of(Club club) {
            return new ClubRef(club.getId(), club.getCode(), club.getName());
        }
    }

    // The club's state at sign-in, so the app can show a banner straight away. reason is null when ACTIVE;
    // endsAt is null without a subscription.
    public record Access(AccessState state, AccessReason reason, Instant endsAt, int daysLeft) {
        static Access of(ClubAccess access) {
            return new Access(access.state(), access.reason(), access.endsAt(), access.daysLeft());
        }
    }
}
