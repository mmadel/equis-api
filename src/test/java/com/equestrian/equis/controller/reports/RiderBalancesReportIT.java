package com.equestrian.equis.controller.reports;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.service.payments.RiderBalance;
import com.equestrian.equis.service.payments.RiderBalanceService;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TestSubscriptions;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TwoClubFixture.PASSWORD;
import static com.equestrian.equis.support.TwoClubFixture.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E19 · Rider balances report, cases 1-11 of the ticket (10's call count and 12 are unit tests, 13 is the rest of
// the suite). The clock is 16 Nov 2026 20:30 in Cairo. The class first drops TwoClubFixture's riders (and with them
// their payments, seats and evaluations; FreshDatabase restores everything before the next class), so "school" has
// exactly the ticket's riders: Omar (Nov x1, 8 sessions, 5 used, plus a voided Nov package), Laila (Nov x2, 16, 6
// used), Youssef (Oct x1, 8, 7 used + Dec x1, 8, recorded 10 Nov), Hana (Nov x1, 8, 8 used) and Ziad (no payments).
// "cairo-eq" has one rider, Karim (Nov x1, 8, none used). Two more clubs: "no-riders" (none) and "big-club" (1,200
// riders without payments).
class RiderBalancesReportIT extends IntegrationTestBase {
    private static final Instant NOV_16_EVENING = ZonedDateTime.of(2026, 11, 16, 20, 30, 0, 0, CAIRO).toInstant();
    private static final String RIDER_BALANCES = "/admin/reports/rider-balances/export";
    private static final List<String> HEADERS = List.of("Rider", "Trainer", "Status", "Bought", "Used", "Remaining",
            "Next expiry");
    private static final LocalDate NOV = LocalDate.of(2026, 11, 1);
    // POI's 0-based index of the header row (Excel row 6).
    private static final int HEADER_ROW = 5;

    @Autowired
    TwoClubFixture fx;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    RiderBalanceService balanceService;

    ClubData school;
    ClubData cairoEq;

    @BeforeEach
    void theTicketsRidersInEveryClub() {
        school = fx.a();
        cairoEq = fx.b();
        CLOCK.set(NOV_16_EVENING);
        root(() -> {
            jdbc.update("update subscription set ends_at = TIMESTAMPTZ '2200-01-01 00:00:00+00'");
            // The fixture is built once per class: add the sample once too.
            if (jdbc.queryForObject("SELECT count(*) FROM rider WHERE name = 'Omar'", Integer.class) == 0) {
                addSample();
            }
            return null;
        });
    }

    // 1
    @Test
    void theAdminDownloadsEveryRiderByName() throws Exception {
        MvcResult result = export(admin("school")).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"rider-balances_2026-11-16.xlsx\"");
        assertThat(titleLine(result, 1)).isEqualTo("Rider balances");
        assertThat(titleLine(result, 2)).isEqualTo("As of 16 Nov 2026");
        List<List<Object>> sheet = sheet(result);
        assertThat(sheet.get(0)).isEqualTo(List.copyOf(HEADERS));
        assertThat(dataRows(sheet)).extracting(row -> row.get(0))
                .containsExactly("Hana", "Laila", "Omar", "Youssef", "Ziad");
    }

    // 2 (and 5: Omar's voided package adds nothing)
    @Test
    void eachRowShowsTodaysBalance() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school")).andExpect(status().isOk()).andReturn()));

        assertThat(rows).containsExactly(
                List.of("Hana", "Trainer One A", "Awaiting payment", 8.0, 8.0, 0.0, ""),
                List.of("Laila", "Trainer One A", "Active", 16.0, 6.0, 10.0, midnight(2027, 1, 31)),
                List.of("Omar", "Trainer One A", "Active", 8.0, 5.0, 3.0, midnight(2026, 12, 31)),
                List.of("Youssef", "Trainer Two A", "Active", 8.0, 7.0, 1.0, midnight(2026, 11, 30)),
                List.of("Ziad", "", "Awaiting payment", 0.0, 0.0, 0.0, ""));
    }

    // 3
    @Test
    void theTotalIsWhatTheClubStillOwes() throws Exception {
        List<List<Object>> sheet = sheet(export(admin("school")).andExpect(status().isOk()).andReturn());

        List<Object> total = sheet.get(sheet.size() - 1);
        assertThat(total.get(0)).isEqualTo("Total");
        assertThat(total.subList(3, 6)).containsExactly(40.0, 26.0, 14.0);
    }

    // 4
    @Test
    void onTheFirstOfDecemberYoussefsOctoberPackageIsGoneAndDecemberCounts() throws Exception {
        CLOCK.set(ZonedDateTime.of(2026, 12, 1, 9, 0, 0, 0, CAIRO).toInstant());

        MvcResult result = export(admin("school")).andExpect(status().isOk()).andReturn();

        assertThat(titleLine(result, 2)).isEqualTo("As of 1 Dec 2026");
        List<Object> youssef = dataRows(sheet(result)).get(3);
        assertThat(youssef).containsExactly("Youssef", "Trainer Two A", "Active", 8.0, 0.0, 8.0, midnight(2027, 1, 31));
    }

    // 5
    @Test
    void aVoidedPackageIsNotCounted() throws Exception {
        Integer voided = TenantContext.runAs(school.clubId(), () -> jdbc.queryForObject(
                "SELECT count(*) FROM payment p JOIN rider r ON r.id = p.rider_id WHERE r.name = 'Omar' "
                        + "AND p.voided_at IS NOT NULL", Integer.class));
        assertThat(voided).isEqualTo(1);

        List<Object> omar = dataRows(sheet(export(admin("school")).andExpect(status().isOk()).andReturn())).get(2);

        assertThat(omar.subList(0, 6)).containsExactly("Omar", "Trainer One A", "Active", 8.0, 5.0, 3.0);
    }

    // 6
    @Test
    void everyRowMatchesTheBalanceService() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school")).andExpect(status().isOk()).andReturn()));
        LocalDate today = LocalDate.of(2026, 11, 16);

        for (List<Object> row : rows) {
            UUID id = jdbc.queryForObject("SELECT id FROM rider WHERE name = ?", UUID.class, row.get(0));
            RiderBalance balance = balanceService.balanceOn(id, today);
            int bought = balance.packages().stream().mapToInt(p -> p.sessions()).sum();
            int used = balance.packages().stream().mapToInt(p -> p.used()).sum();
            assertThat(row.get(2)).as("status of %s", row.get(0))
                    .isEqualTo(balance.status().name().equals("ACTIVE") ? "Active" : "Awaiting payment");
            assertThat(row.subList(3, 6)).as("numbers of %s", row.get(0))
                    .containsExactly((double) bought, (double) used, (double) balance.sessionsLeft());
            assertThat(row.get(6)).as("next expiry of %s", row.get(0))
                    .isEqualTo(balance.nextExpiry() == null ? "" : balance.nextExpiry().atStartOfDay());
        }
    }

    // 7
    @Test
    void theOtherClubsAdminSeesOnlyTheirRider() throws Exception {
        List<List<Object>> sheet = sheet(export(admin("cairo-eq")).andExpect(status().isOk()).andReturn());

        assertThat(dataRows(sheet)).containsExactly(
                List.of("Karim", "Trainer One B", "Active", 8.0, 0.0, 8.0, midnight(2026, 12, 31)));
        assertThat(sheet.get(sheet.size() - 1).subList(3, 6)).containsExactly(8.0, 0.0, 8.0);
    }

    // 8
    @Test
    void fromAndToAreIgnored() throws Exception {
        MvcResult result = fx.perform(admin("school"), get(RIDER_BALANCES + "?from=2026-10-01&to=2026-10-31"))
                .andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"rider-balances_2026-11-16.xlsx\"");
        assertThat(titleLine(result, 2)).isEqualTo("As of 16 Nov 2026");
        assertThat(dataRows(sheet(result))).hasSize(5);
    }

    // 9
    @Test
    void aClubWithoutRidersHasTheHeaderAndAZeroTotal() throws Exception {
        MvcResult result = export(admin("no-riders")).andExpect(status().isOk()).andReturn();

        List<List<Object>> sheet = sheet(result);
        assertThat(titleLine(result, 0)).isEqualTo("No Riders Club");
        assertThat(titleLine(result, 1)).isEqualTo("Rider balances");
        assertThat(sheet).hasSize(2);
        assertThat(sheet.get(0)).isEqualTo(List.copyOf(HEADERS));
        assertThat(sheet.get(1).get(0)).isEqualTo("Total");
        assertThat(sheet.get(1).subList(3, 6)).containsExactly(0.0, 0.0, 0.0);
    }

    // 10 (balancesOn is called three times: RiderBalancesReportTest)
    @Test
    void twelveHundredRidersAreAllListedByName() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("big-club")).andExpect(status().isOk()).andReturn()));

        assertThat(rows).hasSize(1200);
        List<Object> names = rows.stream().map(row -> row.get(0)).toList();
        assertThat(names.get(0)).isEqualTo("Rider 0001");
        assertThat(names.get(1199)).isEqualTo("Rider 1200");
        assertThat(names).isSortedAccordingTo((x, y) -> ((String) x).compareTo((String) y));
    }

    // 11: the view keeps the row-level security of rider, even for SQL that names no club.
    @Test
    void theViewShowsOnlyTheCurrentClubsRiders() {
        UUID cairo = cairoEq.clubId();

        List<Map<String, Object>> seen = TenantContext.runAs(cairo,
                () -> jdbc.queryForList("SELECT club_id, rider_name, trainer_name FROM report_rider_v"));

        assertThat(seen).containsExactly(Map.of("club_id", cairo, "rider_name", "Karim",
                "trainer_name", "Trainer One B"));
        assertThat(TenantContext.runAs(cairo, () -> jdbc.queryForObject("SELECT current_user", String.class)))
                .isEqualTo("equis_app");
    }

    // ---- data

    private void addSample() {
        // TwoClubFixture's riders go, with everything that points at them.
        jdbc.execute("truncate table rider cascade");

        UUID a = school.clubId();
        UUID admin = adminOf(a);
        List<UUID> sessions = sessions(a, 8);
        UUID omar = rider(a, "Omar", school.trainer1());
        UUID laila = rider(a, "Laila", school.trainer1());
        UUID youssef = rider(a, "Youssef", school.trainer2());
        UUID hana = rider(a, "Hana", school.trainer1());
        rider(a, "Ziad", null);

        use(a, omar, pay(a, omar, admin, NOV, 1, 8, cairo(2026, 11, 2, 10, 0)), sessions, 5);
        UUID wrong = pay(a, omar, admin, NOV, 1, 8, cairo(2026, 11, 2, 10, 5));
        jdbc.update("UPDATE payment SET voided_at = ?, voided_by = ?, void_reason = 'Recorded twice' WHERE id = ?",
                Timestamp.from(cairo(2026, 11, 2, 10, 6)), admin, wrong);
        use(a, laila, pay(a, laila, admin, NOV, 2, 16, cairo(2026, 11, 3, 11, 0)), sessions, 6);
        use(a, youssef, pay(a, youssef, admin, LocalDate.of(2026, 10, 1), 1, 8, cairo(2026, 10, 2, 9, 0)), sessions, 7);
        pay(a, youssef, admin, LocalDate.of(2026, 12, 1), 1, 8, cairo(2026, 11, 10, 18, 0));
        use(a, hana, pay(a, hana, admin, NOV, 1, 8, cairo(2026, 11, 4, 12, 0)), sessions, 8);

        UUID b = cairoEq.clubId();
        UUID karim = rider(b, "Karim", cairoEq.trainer1());
        pay(b, karim, adminOf(b), NOV, 1, 8, cairo(2026, 11, 15, 9, 0));

        club("no-riders", "No Riders Club");
        UUID big = club("big-club", "Big Club");
        jdbc.update("""
                INSERT INTO rider (club_id, name, level_id)
                SELECT ?, 'Rider ' || lpad(n::text, 4, '0'), 1 FROM generate_series(1, 1200) AS n""", big);
    }

    // A club with its admin "admin@<code>.eg" and a subscription.
    private UUID club(String code, String name) {
        UUID club = jdbc.queryForObject("INSERT INTO club (code, name) VALUES (?, ?) RETURNING id", UUID.class,
                code, name);
        jdbc.update("INSERT INTO admin_account (club_id, name, email, password_hash) VALUES (?, 'Admin', ?, ?)",
                club, "admin@" + code + ".eg", passwordEncoder.encode(PASSWORD));
        TestSubscriptions.validFor(jdbc, club);
        return club;
    }

    private UUID adminOf(UUID club) {
        return jdbc.queryForObject("SELECT id FROM admin_account WHERE club_id = ?", UUID.class, club);
    }

    // count past sessions of one template, one per day from 1 Nov 2026, to seat the riders in.
    private List<UUID> sessions(UUID club, int count) {
        UUID template = jdbc.queryForObject("""
                INSERT INTO session_template (club_id, level_id, day_of_week, start_time, end_time, capacity, valid_from)
                VALUES (?, 1, 1, '07:00', '08:00', 12, '2026-01-01') RETURNING id""", UUID.class, club);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(jdbc.queryForObject("""
                    INSERT INTO session (club_id, template_id, session_date, start_time, end_time, capacity)
                    VALUES (?, ?, ?, '07:00', '08:00', 12) RETURNING id""", UUID.class,
                    club, template, NOV.plusDays(i)));
        }
        return ids;
    }

    private UUID rider(UUID club, String name, UUID trainer) {
        return jdbc.queryForObject("INSERT INTO rider (club_id, name, level_id, trainer_id) VALUES (?, ?, 1, ?) RETURNING id",
                UUID.class, club, name, trainer);
    }

    private UUID pay(UUID club, UUID rider, UUID admin, LocalDate startMonth, int months, int sessions,
                     Instant recordedAt) {
        return jdbc.queryForObject("""
                        INSERT INTO payment (club_id, rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_at, recorded_by)
                        VALUES (?, ?, 2400, 'cash', ?, ?, ?, ?, ?, ?) RETURNING id""",
                UUID.class, club, rider, startMonth, months, sessions,
                startMonth.plusMonths(months + 1).minusDays(1), Timestamp.from(recordedAt), admin);
    }

    // The rider used count sessions of the package, one per session.
    private void use(UUID club, UUID rider, UUID payment, List<UUID> sessions, int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("INSERT INTO session_usage (club_id, rider_id, payment_id, session_id) VALUES (?, ?, ?, ?)",
                    club, rider, payment, sessions.get(i));
        }
    }

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }

    private static LocalDateTime midnight(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    // ---- helpers

    private ResultActions export(String token) throws Exception {
        return fx.perform(token, get(RIDER_BALANCES));
    }

    private String admin(String clubCode) throws Exception {
        ResultActions result = fx.loginRequest(clubCode, "admin@" + clubCode + ".eg", PASSWORD)
                .andExpect(status().isOk());
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
