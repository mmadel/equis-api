package com.equestrian.equis.common.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.UUID;

// Hibernate asks this when a session opens: @TenantId entities are then read with "club_id = <club>" and saved
// with that club. Root mode (and no club at all) returns ROOT, which turns the filter off; RLS still applies
// in the database, so with no club nothing is visible there.
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {
    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID club = TenantContext.current();
        return club == null ? TenantContext.ROOT : club;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return TenantContext.ROOT.equals(tenantId);
    }
}
