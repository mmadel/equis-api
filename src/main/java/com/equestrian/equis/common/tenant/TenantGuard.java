package com.equestrian.equis.common.tenant;

import jakarta.persistence.PrePersist;

// Entity listener on every school entity: a new row needs a club. Without this, root mode would save the ROOT id
// and fail later on the foreign key, with a less clear message.
public class TenantGuard {
    @PrePersist
    void requireClub(Object entity) {
        if (TenantContext.current() == null) {
            throw new IllegalStateException("No club for tenant write: " + entity.getClass().getSimpleName());
        }
    }
}
