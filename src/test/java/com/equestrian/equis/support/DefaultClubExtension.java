package com.equestrian.equis.support;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.club.Club;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// Every test runs in the default club, the one all migrations and seeds use. Row-level security (V24) hides
// every row from a connection without a club, so without this, test code that uses JdbcTemplate or a repository
// outside a request would see nothing. Requests over MockMvc still run in their token's club (TenantFilter puts
// this club back afterwards). Tests about other clubs switch with TenantContext.runAs.
// Registered for every test class through META-INF/services and junit-platform.properties (autodetection).
public class DefaultClubExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        TenantContext.set(Club.DEFAULT_ID);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        TenantContext.set(Club.DEFAULT_ID);
    }

    // Back to the default club, so @AfterAll clean-ups that run SQL still see their rows.
    @Override
    public void afterEach(ExtensionContext context) {
        TenantContext.set(Club.DEFAULT_ID);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        TenantContext.clear();
    }
}
