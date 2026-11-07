package com.equestrian.equis.common.security;

import java.util.UUID;

// clubId is the club the person belongs to (token claim "cid"); every request runs in it (TenantFilter).
public record AuthUser(UUID id, Role role, UUID clubId) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
