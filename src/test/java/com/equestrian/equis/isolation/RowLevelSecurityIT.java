package com.equestrian.equis.isolation;

import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.support.TwoClubFixture;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E17 · cases 9 and 10: PostgreSQL itself keeps the clubs apart on every school table, whatever SQL runs.
class RowLevelSecurityIT extends IntegrationTestBase {
    // Tables without club rows (global) or found before the club is known (auth lookups), per the V23
    // classification. Platform tables of later tickets join this list when they exist.
    private static final Set<String> NOT_TENANT = Set.of(
            "level", "club", "flyway_schema_history", "invite", "refresh_token", "plan", "subscription", "super_admin");

    @Autowired
    TwoClubFixture fx;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    TransactionTemplate tx;

    // Case 9: a new table without RLS fails here, by name.
    @Test
    void everyTenantTableHasForcedRowLevelSecurityAndThePolicy() {
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename", String.class)
                .stream().filter(table -> !NOT_TENANT.contains(table)).toList();

        assertThat(tables).containsExactlyInAnyOrderElementsOf(TwoClubFixture.TENANT_TABLES);
        for (String table : tables) {
            Map<String, Object> rls = jdbc.queryForMap(
                    "SELECT relrowsecurity, relforcerowsecurity FROM pg_class WHERE oid = ?::regclass", table);
            assertThat(rls).as(table).containsEntry("relrowsecurity", true).containsEntry("relforcerowsecurity", true);
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM pg_policies WHERE tablename = ? AND policyname = 'tenant_isolation'",
                    Integer.class, table)).as(table + " policy").isEqualTo(1);
        }
    }

    // Case 9: the app works as a role RLS applies to (it never applies to a superuser or BYPASSRLS).
    @Test
    void theAppsConnectionsUseARoleThatCannotSkipRowLevelSecurity() {
        Map<String, Object> role = jdbc.queryForMap("""
                SELECT current_user AS name, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = current_user""");

        assertThat(role).containsEntry("name", "equis_app").containsEntry("rolsuper", false)
                .containsEntry("rolbypassrls", false);
    }

    // Case 10
    @TestFactory
    Stream<DynamicTest> nativeSqlOnEveryTenantTableSeesOnlyTheConnectionsClub() {
        UUID a = fx.a().clubId();
        UUID b = fx.b().clubId();
        return TwoClubFixture.TENANT_TABLES.stream().map(table -> DynamicTest.dynamicTest(table, () -> {
            String count = "SELECT count(*) FROM " + table;
            long aRows = fx.count(table, a);
            long bRows = fx.count(table, b);
            assertThat(aRows).isPositive();
            assertThat(bRows).isPositive();

            assertThat(TenantContext.runAs(a, () -> jdbc.queryForObject(count, Long.class))).isEqualTo(aRows);
            assertThat(TenantContext.runAs(b, () -> jdbc.queryForObject(count, Long.class))).isEqualTo(bRows);
            assertThat(TenantContext.runAs(null, () -> jdbc.queryForObject(count, Long.class))).isZero();
            assertThat(TenantContext.runAsRoot(() -> jdbc.queryForObject(count, Long.class))).isEqualTo(aRows + bRows);
            // An explicit filter on the other club finds nothing either.
            assertThat(TenantContext.runAs(a, () -> jdbc.queryForObject(
                    count + " WHERE club_id = ?", Long.class, b))).isZero();

            // A copy of one of B's rows, written while A is set, is refused by the policy.
            assertThatThrownBy(() -> TenantContext.runAsRoot(() -> tx.execute(status -> {
                jdbc.execute("CREATE TEMP TABLE copy ON COMMIT DROP AS SELECT * FROM " + table
                        + " WHERE club_id = '" + b + "' LIMIT 1");
                jdbc.execute("UPDATE copy SET id = gen_random_uuid()");
                jdbc.queryForList("SELECT set_config('app.bypass_rls', 'off', true), set_config('app.club_id', ?, true)",
                        a.toString());
                return jdbc.update("INSERT INTO " + table + " SELECT * FROM copy");
            }))).isInstanceOf(DataAccessException.class)
                    .rootCause().hasMessageContaining("new row violates row-level security policy for table \"" + table + "\"");
            assertThat(fx.count(table, b)).isEqualTo(bRows);
        }));
    }
}
