package com.equestrian.equis.repository.club;

import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.club.ClubStatus;
import com.equestrian.equis.support.TestPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E17 · club table and club_id on every table (migration V23), against the real schema.
// Each test runs in a transaction that is rolled back, so the inserted clubs and rows don't leak.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClubRepositoryTest {
    private static final List<String> TENANT_TABLES = List.of(
            "admin_account", "trainer", "session_template", "session", "rider", "assignment", "member",
            "assessment_booking", "level_change", "notice", "payment", "session_usage", "session_evaluation");
    private static final String AHMED_PHONE = "01055556666";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    ClubRepository clubRepository;
    @Autowired
    JdbcTemplate jdbc;

    UUID cairo;

    // The tests write rows of two clubs by hand, so row-level security (V24) is bypassed for this test's
    // transaction only (set_config(..., true) ends with the rollback).
    @BeforeEach
    void secondClub() {
        jdbc.queryForObject("select set_config('app.bypass_rls', 'on', true)", String.class);
        cairo = jdbc.queryForObject(
                "INSERT INTO club (code, name) VALUES ('cairo-eq', 'Cairo Equestrian') RETURNING id", UUID.class);
    }

    // Case 1 is this class starting at all: the context runs Flyway and then ddl-auto=validate.

    // Case 2
    @Test
    void theDefaultClubIsSeededFromThePlaceholders() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, code, name, status FROM club WHERE id <> ?", cairo);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("id")).isEqualTo(Club.DEFAULT_ID);
            assertThat(row.get("code")).isEqualTo("school");
            assertThat(row.get("name")).isEqualTo("Riding School");
            assertThat(row.get("status")).isEqualTo("ACTIVE");
        });
    }

    // Case 3
    @Test
    void everyTenantAndAuthLookupTableHasANonNullClubId() {
        for (String table : concat(TENANT_TABLES, "invite")) {
            assertThat(isNullable(table)).as(table + ".club_id nullable").isEqualTo("NO");
            assertThat(nullClubIds(table)).as(table).isZero();
        }
        assertThat(isNullable("refresh_token")).isEqualTo("YES");
    }

    // Case 3: foreign key and index starting with club_id on each of them
    @Test
    void everyClubIdHasAForeignKeyAndAnIndex() {
        for (String table : concat(TENANT_TABLES, "invite", "refresh_token")) {
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM pg_constraint
                     WHERE conrelid = ?::regclass AND contype = 'f' AND confrelid = 'club'::regclass""",
                    Integer.class, table)).as(table + " FK").isEqualTo(1);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM pg_index i
                      JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = i.indkey[0]
                     WHERE i.indrelid = ?::regclass AND a.attname = 'club_id'""",
                    Integer.class, table)).as(table + " index").isPositive();
        }
    }

    // Case 4
    @Test
    void seededRowsAreInTheDefaultClubAndLevelsHaveNoClub() {
        assertThat(clubsOf("trainer")).containsExactly(Club.DEFAULT_ID);
        assertThat(clubsOf("admin_account")).containsExactly(Club.DEFAULT_ID);
        assertThat(clubsOf("invite")).containsExactly(Club.DEFAULT_ID);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'level' AND column_name = 'club_id'",
                Integer.class)).isZero();
    }

    // Case 5
    @Test
    void anotherClubMayHaveATrainerWithTheSamePhone() {
        insertTrainer(Club.DEFAULT_ID, AHMED_PHONE, "ahmed@school.com");

        insertTrainer(cairo, AHMED_PHONE, "ahmed@cairo.com");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM trainer WHERE phone = ?", Integer.class, AHMED_PHONE))
                .isEqualTo(2);
    }

    // Case 6
    @Test
    void theSameClubRefusesASecondTrainerWithThatPhone() {
        insertTrainer(Club.DEFAULT_ID, AHMED_PHONE, "ahmed@school.com");

        assertRefusedBy("uq_trainer_club_phone", () -> insertTrainer(Club.DEFAULT_ID, AHMED_PHONE, "other@school.com"));
    }

    // Case 7
    @Test
    void trainerEmailIsUniquePerClub() {
        insertTrainer(Club.DEFAULT_ID, AHMED_PHONE, "ahmed@mail.com");
        insertTrainer(cairo, "01077778888", "ahmed@mail.com");

        assertRefusedBy("uq_trainer_club_email", () -> insertTrainer(Club.DEFAULT_ID, "01099990000", "ahmed@mail.com"));
    }

    // Case 7
    @Test
    void adminEmailIsUniquePerClub() {
        insertAdmin(Club.DEFAULT_ID, "boss@mail.com");
        insertAdmin(cairo, "boss@mail.com");

        assertRefusedBy("uq_admin_account_club_email", () -> insertAdmin(Club.DEFAULT_ID, "boss@mail.com"));
    }

    // Case 7
    @Test
    void memberPhoneIsUniquePerClub() {
        insertMember(Club.DEFAULT_ID, "01011112222", "sara@mail.com");
        insertMember(cairo, "01011112222", "sara@mail.com");

        assertRefusedBy("uq_member_club_phone", () -> insertMember(Club.DEFAULT_ID, "01011112222", "x@mail.com"));
    }

    // Case 7 (a failed insert aborts the transaction, so one refusal per test)
    @Test
    void memberEmailIsUniquePerClub() {
        insertMember(Club.DEFAULT_ID, "01011112222", "sara@mail.com");
        insertMember(cairo, "01011112222", "sara@mail.com");

        assertRefusedBy("uq_member_club_email", () -> insertMember(Club.DEFAULT_ID, "01033334444", "sara@mail.com"));
    }

    // Case 7
    @Test
    void riderEmailIsUniquePerClub() {
        insertRider(Club.DEFAULT_ID, "sara@mail.com");
        insertRider(cairo, "sara@mail.com");

        assertRefusedBy("uq_rider_club_email", () -> insertRider(Club.DEFAULT_ID, "sara@mail.com"));
    }

    // Case 8
    @ParameterizedTest
    @ValueSource(strings = {"Cairo EQ", "ab", "-abc", "abc-", "abc_d"})
    void aBadClubCodeIsRefused(String code) {
        assertRefusedBy("ck_club_code", () -> jdbc.update("INSERT INTO club (code, name) VALUES (?, 'Bad')", code));
    }

    // Case 8: the edges of the rule are accepted
    @ParameterizedTest
    @ValueSource(strings = {"abc", "a-b", "club-2", "a23456789012345678901234567890"})
    void aGoodClubCodeIsAccepted(String code) {
        assertThat(jdbc.update("INSERT INTO club (code, name) VALUES (?, 'Good')", code)).isEqualTo(1);
    }

    @Test
    void aClubCodeIsUnique() {
        assertRefusedBy("uq_club_code", () -> jdbc.update("INSERT INTO club (code, name) VALUES ('cairo-eq', 'Copy')"));
    }

    @Test
    void aSuspendedClubNeedsASuspendedDate() {
        assertRefusedBy("ck_club_suspended",
                () -> jdbc.update("UPDATE club SET status = 'SUSPENDED' WHERE id = ?", cairo));
    }

    // Case 9
    @Test
    void findsTheDefaultClubByCode() {
        Club club = clubRepository.findByCode("school").orElseThrow();

        assertThat(club.getId()).isEqualTo(Club.DEFAULT_ID);
        assertThat(club.getName()).isEqualTo("Riding School");
        assertThat(club.getStatus()).isEqualTo(ClubStatus.ACTIVE);
        assertThat(club.getCreatedAt()).isNotNull();
        assertThat(clubRepository.existsByCode("school")).isTrue();
        assertThat(clubRepository.existsByCode("nowhere")).isFalse();
        assertThat(clubRepository.findByCode("nowhere")).isEmpty();
    }

    @Test
    void locksAClubById() {
        assertThat(clubRepository.findByIdForUpdate(cairo)).get()
                .extracting(Club::getCode).isEqualTo("cairo-eq");
    }

    // ---- helpers

    private void insertTrainer(UUID clubId, String phone, String email) {
        jdbc.update("INSERT INTO trainer (club_id, name, phone, email) VALUES (?, 'Ahmed Said', ?, ?)",
                clubId, phone, email);
    }

    private void insertAdmin(UUID clubId, String email) {
        jdbc.update("INSERT INTO admin_account (club_id, email, password_hash) VALUES (?, ?, 'hash')", clubId, email);
    }

    private void insertMember(UUID clubId, String phone, String email) {
        jdbc.update("INSERT INTO member (club_id, name, phone, email) VALUES (?, 'Sara', ?, ?)", clubId, phone, email);
    }

    private void insertRider(UUID clubId, String email) {
        jdbc.update("INSERT INTO rider (club_id, name, level_id, email) VALUES (?, 'Sara', 1, ?)", clubId, email);
    }

    private void assertRefusedBy(String constraint, Runnable insert) {
        // Plain JDBC, so the constraint name is only in the driver's message.
        assertThatThrownBy(insert::run)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
    }

    private String isNullable(String table) {
        return jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns WHERE table_name = ? AND column_name = 'club_id'",
                String.class, table);
    }

    private int nullClubIds(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE club_id IS NULL", Integer.class);
    }

    private List<UUID> clubsOf(String table) {
        return jdbc.queryForList("SELECT DISTINCT club_id FROM " + table, UUID.class);
    }

    private static List<String> concat(List<String> tables, String... more) {
        return java.util.stream.Stream.concat(tables.stream(), java.util.Arrays.stream(more)).toList();
    }
}
