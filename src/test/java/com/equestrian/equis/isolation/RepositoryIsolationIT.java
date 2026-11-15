package com.equestrian.equis.isolation;

import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.service.sessions.SessionGenerator;
import com.equestrian.equis.support.TwoClubFixture;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

// E17 · cases 1 and 2: every repository of a school entity, run as club A, returns only club A's rows and can't
// find club B's rows by id. The repositories are found in the application context, so a new one is covered as
// soon as it exists. The read-only report views (@Immutable entities on report_*_v, E18) are checked through the
// EntityManager instead: their repositories are not CrudRepositories.
class RepositoryIsolationIT extends IntegrationTestBase {

    @Autowired
    TwoClubFixture fx;
    @Autowired
    ApplicationContext context;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    SessionGenerator generator;
    @Autowired
    EntityManager em;

    // Every school table has an entity with @TenantId and a repository, so none can be missed below.
    @Test
    void everySchoolTableHasATenantEntityWithARepository() {
        assertThat(tenantEntities()).extracting(RepositoryIsolationIT::table)
                .containsExactlyInAnyOrderElementsOf(TwoClubFixture.TENANT_TABLES);
    }

    // Cases 1 and 2
    @TestFactory
    Stream<DynamicTest> everyTenantRepositorySeesOnlyItsClub() {
        UUID a = fx.a().clubId();
        UUID b = fx.b().clubId();
        Repositories repositories = new Repositories(context);
        return tenantEntities().stream().map(type -> DynamicTest.dynamicTest(type.getSimpleName(), () -> {
            @SuppressWarnings("unchecked")
            CrudRepository<Object, UUID> repository = (CrudRepository<Object, UUID>) repositories
                    .getRepositoryFor(type).orElseThrow();
            String table = table(type);
            assertThat(fx.count(table, a)).as("club A has %s rows", table).isPositive();
            assertThat(fx.count(table, b)).as("club B has %s rows", table).isPositive();

            // Case 1: findAll and count.
            List<Object> all = TenantContext.runAs(a, () -> list(repository.findAll()));
            assertThat(all).extracting(RepositoryIsolationIT::clubOf).containsOnly(a);
            assertThat(all).hasSize((int) fx.count(table, a));
            assertThat(TenantContext.runAs(a, repository::count)).isEqualTo(fx.count(table, a));

            // Case 2: club B's row by id, as A and (as a check of the test) as B.
            UUID bRow = TenantContext.runAsRoot(() -> jdbc.queryForObject(
                    "SELECT id FROM " + table + " WHERE club_id = ? LIMIT 1", UUID.class, b));
            assertThat(TenantContext.runAs(a, () -> repository.findById(bRow))).isEmpty();
            assertThat(TenantContext.runAs(a, () -> repository.existsById(bRow))).isFalse();
            Optional<Object> asB = TenantContext.runAs(b, () -> repository.findById(bRow));
            assertThat(asB).isPresent();
        }));
    }

    // E18 · every report view, run as club A, shows only club A's rows and can't find club B's row by id.
    @TestFactory
    Stream<DynamicTest> everyReportViewSeesOnlyItsClub() {
        UUID a = fx.a().clubId();
        UUID b = fx.b().clubId();
        assertThat(reportViews()).as("report views").isNotEmpty();
        return reportViews().stream().map(type -> DynamicTest.dynamicTest(type.getSimpleName(), () -> {
            String view = table(type);
            assertThat(fx.count(view, a)).as("club A has %s rows", view).isPositive();
            assertThat(fx.count(view, b)).as("club B has %s rows", view).isPositive();

            List<?> all = TenantContext.runAs(a, () ->
                    em.createQuery("from " + type.getSimpleName(), type).getResultList());
            assertThat(all).extracting(RepositoryIsolationIT::clubOf).containsOnly(a);
            assertThat(all).hasSize((int) fx.count(view, a));

            UUID bRow = TenantContext.runAsRoot(() -> jdbc.queryForObject(
                    "SELECT id FROM " + view + " WHERE club_id = ? LIMIT 1", UUID.class, b));
            assertThat(TenantContext.runAs(a, () -> byId(type, bRow))).isEmpty();
            assertThat(TenantContext.runAs(b, () -> byId(type, bRow))).hasSize(1);
        }));
    }

    // Case 11: the only native query, SessionRepository.insertIfAbsent (the session generator), run as club A,
    // adds sessions for club A's templates only, each in club A. It joins nothing, and club_id comes from the
    // connection's club (V24 default).
    @Test
    void generatingSessionsAsClubATouchesOnlyClubA() {
        UUID a = fx.a().clubId();
        UUID b = fx.b().clubId();
        TenantContext.runAsRoot(() -> jdbc.update("DELETE FROM session s WHERE s.session_date > DATE '2026-10-16'"
                + " AND NOT EXISTS (SELECT 1 FROM assignment x WHERE x.session_id = s.id)"
                + " AND NOT EXISTS (SELECT 1 FROM assessment_booking x WHERE x.session_id = s.id)"));
        long bSessions = fx.count("session", b);
        long aSessions = fx.count("session", a);

        TenantContext.runAs(a, generator::generateAll);

        assertThat(fx.count("session", b)).isEqualTo(bSessions);
        assertThat(fx.count("session", a)).isGreaterThan(aSessions);
        assertThat(TenantContext.runAsRoot(() -> jdbc.queryForObject("""
                SELECT count(*) FROM session s JOIN session_template t ON t.id = s.template_id
                WHERE s.club_id <> t.club_id""", Integer.class))).as("sessions of another club's template").isZero();
    }

    // The school tables' entities: @TenantId, not a read-only report view.
    private List<Class<?>> tenantEntities() {
        return withTenantId(false);
    }

    // The report views' entities: @TenantId and @Immutable.
    private List<Class<?>> reportViews() {
        return withTenantId(true);
    }

    private List<Class<?>> withTenantId(boolean immutable) {
        List<Class<?>> types = new ArrayList<>();
        for (Class<?> type : new Repositories(context)) {
            if (tenantField(type) != null && type.isAnnotationPresent(Immutable.class) == immutable) {
                types.add(type);
            }
        }
        return types;
    }

    private List<?> byId(Class<?> type, UUID id) {
        return em.createQuery("from " + type.getSimpleName() + " where id = :id", type)
                .setParameter("id", id).getResultList();
    }

    private static Field tenantField(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(TenantId.class))
                .findFirst().orElse(null);
    }

    private static UUID clubOf(Object entity) {
        try {
            Field field = tenantField(entity.getClass());
            field.setAccessible(true);
            return (UUID) field.get(entity);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String table(Class<?> type) {
        return type.getAnnotation(Table.class).name();
    }

    private static List<Object> list(Iterable<Object> rows) {
        return StreamSupport.stream(rows.spliterator(), false).toList();
    }
}
