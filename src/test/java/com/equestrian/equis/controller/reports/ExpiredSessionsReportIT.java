package com.equestrian.equis.controller.reports;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TwoClubFixture;
import com.equestrian.equis.support.TwoClubFixture.ClubData;
import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TwoClubFixture.PASSWORD;
import static com.equestrian.equis.support.TwoClubFixture.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E19 · Expired sessions report, cases 1-9 of the ticket (10 is the rest of the suite). The clock is 1 Dec 2026 09:15
// in Cairo. "school" packages: Youssef Oct x1 (8, 7 used, valid until 30 Nov), Mariam Sep x2 (16, 11 used, 30 Nov),
// Tamer Oct x1 (8, 8 used, 30 Nov), Laila Nov x2 (16, 6 used, 31 Jan 2027), Omar Sep x1 (8, 2 used, 31 Oct) and
// Hana's voided Oct x1 (8, none used, 30 Nov). "cairo-eq": Karim Oct x1 (8, 3 used, 30 Nov).
// TwoClubFixture's own October packages would also expire on 30 Nov: the sample voids them first, so the files hold
// only the ticket's packages.
class ExpiredSessionsReportIT extends IntegrationTestBase {
    private static final Instant DEC_1_MORNING = ZonedDateTime.of(2026, 12, 1, 9, 15, 0, 0, CAIRO).toInstant();
    private static final String EXPIRED_SESSIONS = "/admin/reports/expired-sessions/export";
    private static final String NOVEMBER = EXPIRED_SESSIONS + "?from=2026-11-01&to=2026-11-30";
    private static final List<String> HEADERS = List.of("Rider", "Trainer", "Package start", "Months", "Expired on",
            "Bought", "Used", "Expired");
    // POI's 0-based index of the header row (Excel row 6).
    private static final int HEADER_ROW = 5;

    @Autowired
    TwoClubFixture fx;
    @Autowired
    JdbcTemplate jdbc;

    ClubData school;
    ClubData cairoEq;

    @BeforeEach
    void packagesInBothClubs() {
        school = fx.a();
        cairoEq = fx.b();
        CLOCK.set(DEC_1_MORNING);
        root(() -> {
            jdbc.update("update subscription set ends_at = TIMESTAMPTZ '2200-01-01 00:00:00+00'");
            // The fixture is built once per class: add the sample once too.
            if (jdbc.queryForObject("SELECT count(*) FROM rider WHERE name = 'Mariam'", Integer.class) == 0) {
                addSample();
            }
            return null;
        });
    }

    // 1
    @Test
    void theAdminDownloadsTheNovemberExpiredPackagesByDateThenName() throws Exception {
        MvcResult result = export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"expired-sessions_2026-11-01_2026-11-30.xlsx\"");
        List<List<Object>> sheet = sheet(result);
        assertThat(sheet.get(0)).isEqualTo(List.copyOf(HEADERS));
        assertThat(dataRows(sheet)).extracting(row -> row.get(0)).containsExactly("Mariam", "Youssef");
        assertThat(titleLine(result, 1)).isEqualTo("Expired sessions");
    }

    // 2
    @Test
    void eachRowShowsThePackageAndTheSessionsLost() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn()));

        assertThat(rows.get(0)).containsExactly("Mariam", "Trainer One A", midnight(2026, 9, 1), 2.0,
                midnight(2026, 11, 30), 16.0, 11.0, 5.0);
        assertThat(rows.get(1)).containsExactly("Youssef", "Trainer One A", midnight(2026, 10, 1), 1.0,
                midnight(2026, 11, 30), 8.0, 7.0, 1.0);
    }

    // 3
    @Test
    void theTotalAddsUpTheSessionsLost() throws Exception {
        List<List<Object>> sheet = sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn());

        List<Object> total = sheet.get(sheet.size() - 1);
        assertThat(total.get(0)).isEqualTo("Total");
        assertThat(total.get(7)).isEqualTo(6.0);
        assertThat(total.get(5)).isEqualTo("");
    }

    // 4
    @Test
    void fullyUsedStillValidAndVoidedPackagesAreNotListed() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school"),
                EXPIRED_SESSIONS + "?from=2026-06-01&to=2027-03-31").andExpect(status().isOk()).andReturn()));

        assertThat(rows).extracting(row -> row.get(0)).containsExactly("Omar", "Mariam", "Youssef")
                .doesNotContain("Tamer", "Laila", "Hana");
    }

    // 5
    @Test
    void octoberHoldsOnlyOmar() throws Exception {
        List<List<Object>> sheet = sheet(export(admin("school"), EXPIRED_SESSIONS + "?from=2026-10-01&to=2026-10-31")
                .andExpect(status().isOk()).andReturn());

        List<List<Object>> rows = dataRows(sheet);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get(0)).isEqualTo("Omar");
        assertThat(rows.get(0).subList(5, 8)).containsExactly(8.0, 2.0, 6.0);
        assertThat(sheet.get(sheet.size() - 1).get(7)).isEqualTo(6.0);
    }

    // 6: a package is usable until the end of its last day.
    @Test
    void onTheLastValidDayNothingHasExpiredYet() throws Exception {
        CLOCK.set(ZonedDateTime.of(2026, 11, 30, 12, 0, 0, 0, CAIRO).toInstant());

        List<List<Object>> sheet = sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn());

        assertThat(sheet).hasSize(2);
        assertThat(sheet.get(0)).isEqualTo(List.copyOf(HEADERS));
        assertThat(sheet.get(1).get(0)).isEqualTo("Total");
        assertThat(sheet.get(1).get(7)).isEqualTo(0.0);
    }

    // 7
    @Test
    void theOtherClubsAdminSeesOnlyTheirOwnPackage() throws Exception {
        List<List<Object>> sheet = sheet(export(admin("cairo-eq"), NOVEMBER).andExpect(status().isOk()).andReturn());

        List<List<Object>> rows = dataRows(sheet);
        assertThat(rows).extracting(row -> row.get(0)).containsExactly("Karim");
        assertThat(sheet.get(sheet.size() - 1).get(7)).isEqualTo(5.0);
    }

    // 8
    @Test
    void fromAfterToIsRefused() throws Exception {
        export(admin("school"), EXPIRED_SESSIONS + "?from=2026-11-30&to=2026-11-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"));
    }

    // 9: the view keeps the row-level security of payment, even for SQL that names no club.
    @Test
    void theViewShowsOnlyTheCurrentClubsPackages() {
        UUID cairo = cairoEq.clubId();
        List<UUID> clubs = TenantContext.runAs(cairo,
                () -> jdbc.queryForList("SELECT DISTINCT club_id FROM report_expired_package_v", UUID.class));
        List<String> riders = TenantContext.runAs(cairo,
                () -> jdbc.queryForList("SELECT rider_name FROM report_expired_package_v", String.class));

        assertThat(clubs).containsExactly(cairo);
        assertThat(riders).containsExactly("Karim");
        assertThat(TenantContext.runAs(cairo, () -> jdbc.queryForObject("SELECT current_user", String.class)))
                .isEqualTo("equis_app");
    }

    // ---- data

    private void addSample() {
        // Every package so far is the fixture's.
        jdbc.update("""
                UPDATE payment SET voided_at = ?, voided_by = recorded_by, void_reason = 'Fixture package'
                WHERE voided_at IS NULL""", Timestamp.from(DEC_1_MORNING));
        UUID a = school.clubId();
        UUID admin = adminOf(a);
        List<UUID> sessions = sessions(a, 11);
        package_(a, rider(a, "Youssef", school.trainer1()), admin, LocalDate.of(2026, 10, 1), 1, 8, sessions, 7);
        package_(a, rider(a, "Mariam", school.trainer1()), admin, LocalDate.of(2026, 9, 1), 2, 16, sessions, 11);
        package_(a, rider(a, "Tamer", school.trainer1()), admin, LocalDate.of(2026, 10, 1), 1, 8, sessions, 8);
        package_(a, rider(a, "Laila", school.trainer1()), admin, LocalDate.of(2026, 11, 1), 2, 16, sessions, 6);
        package_(a, rider(a, "Omar", school.trainer1()), admin, LocalDate.of(2026, 9, 1), 1, 8, sessions, 2);
        voidPackage(package_(a, rider(a, "Hana", school.trainer1()), admin, LocalDate.of(2026, 10, 1), 1, 8,
                sessions, 0), admin);

        UUID b = cairoEq.clubId();
        UUID bAdmin = adminOf(b);
        package_(b, rider(b, "Karim", cairoEq.trainer1()), bAdmin, LocalDate.of(2026, 10, 1), 1, 8, sessions(b, 3), 3);
    }

    private UUID adminOf(UUID club) {
        return jdbc.queryForObject("SELECT id FROM admin_account WHERE club_id = ?", UUID.class, club);
    }

    private void voidPackage(UUID payment, UUID admin) {
        jdbc.update("UPDATE payment SET voided_at = ?, voided_by = ?, void_reason = 'Recorded twice' WHERE id = ?",
                Timestamp.from(DEC_1_MORNING), admin, payment);
    }

    // count past sessions of one template, one per day from 1 Sep 2026, to seat the riders in.
    private List<UUID> sessions(UUID club, int count) {
        UUID template = jdbc.queryForObject("""
                INSERT INTO session_template (club_id, level_id, day_of_week, start_time, end_time, capacity, valid_from)
                VALUES (?, 1, 1, '07:00', '08:00', 12, '2026-01-01') RETURNING id""", UUID.class, club);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(jdbc.queryForObject("""
                    INSERT INTO session (club_id, template_id, session_date, start_time, end_time, capacity)
                    VALUES (?, ?, ?, '07:00', '08:00', 12) RETURNING id""", UUID.class,
                    club, template, LocalDate.of(2026, 9, 1).plusDays(i)));
        }
        return ids;
    }

    private UUID rider(UUID club, String name, UUID trainer) {
        return jdbc.queryForObject("INSERT INTO rider (club_id, name, level_id, trainer_id) VALUES (?, ?, 1, ?) RETURNING id",
                UUID.class, club, name, trainer);
    }

    // A package of sessions, of which the rider used the first `used`, one per session.
    private UUID package_(UUID club, UUID rider, UUID admin, LocalDate startMonth, int months, int sessions,
                          List<UUID> seats, int used) {
        UUID payment = jdbc.queryForObject("""
                        INSERT INTO payment (club_id, rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_at, recorded_by)
                        VALUES (?, ?, 2400, 'cash', ?, ?, ?, ?, ?, ?) RETURNING id""",
                UUID.class, club, rider, startMonth, months, sessions,
                startMonth.plusMonths(months + 1).minusDays(1), Timestamp.from(startMonth.atStartOfDay(CAIRO).toInstant()),
                admin);
        for (int i = 0; i < used; i++) {
            jdbc.update("INSERT INTO session_usage (club_id, rider_id, payment_id, session_id) VALUES (?, ?, ?, ?)",
                    club, rider, payment, seats.get(i));
        }
        return payment;
    }

    private static LocalDateTime midnight(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    // ---- helpers

    private ResultActions export(String token, String path) throws Exception {
        return fx.perform(token, get(path));
    }

    private String admin(String clubCode) throws Exception {
        return signIn(clubCode, "admin@" + clubCode + ".eg");
    }

    private String signIn(String clubCode, String login) throws Exception {
        ResultActions result = fx.loginRequest(clubCode, login, PASSWORD).andExpect(status().isOk());
        return JsonPath.read(body(result), "$.accessToken");
    }

    // The header, the data rows and the total row. A text cell is a String (blank ""), a date a LocalDateTime, any
    // other number a Double.
    private static List<List<Object>> sheet(MvcResult result) throws Exception {
        try (XSSFWorkbook book = workbook(result)) {
            Sheet sheet = book.getSheetAt(0);
            List<List<Object>> rows = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() < HEADER_ROW) {
                    continue;
                }
                List<Object> cells = new ArrayList<>();
                for (int c = 0; c < HEADERS.size(); c++) {
                    cells.add(value(row.getCell(c)));
                }
                rows.add(cells);
            }
            return rows;
        }
    }

    private static Object value(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue()
                    : (Object) cell.getNumericCellValue();
            case BLANK -> "";
            default -> cell.getStringCellValue();
        };
    }

    // Without the header and the total row.
    private static List<List<Object>> dataRows(List<List<Object>> sheet) {
        return sheet.subList(1, sheet.size() - 1);
    }

    private static String titleLine(MvcResult result, int index) throws Exception {
        try (XSSFWorkbook book = workbook(result)) {
            return book.getSheetAt(0).getRow(index).getCell(0).getStringCellValue();
        }
    }

    private static XSSFWorkbook workbook(MvcResult result) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
