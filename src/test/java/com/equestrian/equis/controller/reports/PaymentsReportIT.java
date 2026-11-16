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

// E19 · Payments report, cases 1-12 of the ticket (13 is the rest of the suite). The clock is 1 Dec 2026 09:12 in
// Cairo. "school" gets the epic sample, recorded in November 2026 by its admin "Mona Hassan": Omar 2,400 cash (2 Nov),
// Laila 4,500 bank transfer (3 Nov, voided "Wrong amount"), Laila 4,800 bank transfer (3 Nov), Youssef 2,400 mobile
// wallet for December (10 Nov), Nour 7,200 card for December to February (25 Nov). "cairo-eq" gets one November
// payment (Karim). The boundary cases use November 2025 (rider "Edge"), so they don't change the November 2026 file.
// TwoClubFixture's own payments are recorded on 16 Oct 2026, outside every range used here.
class PaymentsReportIT extends IntegrationTestBase {
    private static final Instant DEC_1_MORNING = ZonedDateTime.of(2026, 12, 1, 9, 12, 0, 0, CAIRO).toInstant();
    private static final String PAYMENTS = "/admin/reports/payments/export";
    private static final String NOVEMBER = PAYMENTS + "?from=2026-11-01&to=2026-11-30";
    private static final List<String> HEADERS = List.of("Recorded at", "Rider", "Trainer", "Amount", "Method",
            "Start month", "Months", "Sessions", "Status", "Void reason", "Recorded by");
    // POI's 0-based index of the header row (Excel row 6).
    private static final int HEADER_ROW = 5;

    @Autowired
    TwoClubFixture fx;
    @Autowired
    JdbcTemplate jdbc;

    ClubData school;
    ClubData cairoEq;

    @BeforeEach
    void novemberPaymentsInBothClubs() {
        school = fx.a();
        cairoEq = fx.b();
        CLOCK.set(DEC_1_MORNING);
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
    void theAdminDownloadsTheNovemberPaymentsInRecordedOrder() throws Exception {
        MvcResult result = export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"payments_2026-11-01_2026-11-30.xlsx\"");
        List<List<Object>> sheet = sheet(result);
        assertThat(sheet.get(0)).isEqualTo(List.copyOf(HEADERS));
        List<List<Object>> rows = dataRows(sheet);
        assertThat(rows).extracting(row -> row.get(1)).containsExactly("Omar", "Laila", "Laila", "Youssef", "Nour");
        assertThat(rows).extracting(row -> row.get(0)).containsExactly(
                LocalDateTime.of(2026, 11, 2, 10, 0), LocalDateTime.of(2026, 11, 3, 11, 0),
                LocalDateTime.of(2026, 11, 3, 11, 5), LocalDateTime.of(2026, 11, 10, 18, 0),
                LocalDateTime.of(2026, 11, 25, 12, 0));
        assertThat(rows).extracting(row -> row.get(2)).containsOnly("Trainer One A");
    }

    // 2
    @Test
    void aVoidedPaymentShowsItsReasonAndAValidOneNone() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn()));

        List<Object> voided = rows.get(1);
        assertThat(voided.get(3)).isEqualTo(4500.0);
        assertThat(voided.get(8)).isEqualTo("Voided");
        assertThat(voided.get(9)).isEqualTo("Wrong amount");
        List<Object> omar = rows.get(0);
        assertThat(omar.get(8)).isEqualTo("Valid");
        assertThat(omar.get(9)).isEqualTo("");
    }

    // 3
    @Test
    void theTotalCountsTheValidPaymentsOnly() throws Exception {
        List<List<Object>> sheet = sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn());

        List<Object> total = sheet.get(sheet.size() - 1);
        assertThat(total.get(0)).isEqualTo("Total (valid)");
        assertThat(total.get(3)).isEqualTo(16800.0);
        assertThat(total.get(7)).isEqualTo(56.0);
    }

    // 4: from and to are whole days in Cairo.
    @Test
    void onlyPaymentsRecordedInsideTheCairoDaysAreIn() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school"), PAYMENTS + "?from=2025-11-01&to=2025-11-30")
                .andExpect(status().isOk()).andReturn()));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get(1)).isEqualTo("Edge");
        assertThat(rows.get(0).get(0)).isEqualTo(LocalDateTime.of(2025, 11, 30, 23, 59));
    }

    // 5
    @Test
    void aPackageForLaterMonthsShowsItsStartMonthMonthsAndSessions() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn()));

        List<Object> nour = rows.get(4);
        assertThat(nour.get(1)).isEqualTo("Nour");
        assertThat(nour.get(5)).isEqualTo(LocalDateTime.of(2026, 12, 1, 0, 0));
        assertThat(nour.get(6)).isEqualTo(3.0);
        assertThat(nour.get(7)).isEqualTo(24.0);
    }

    // 6
    @Test
    void methodsAreShownAsLabels() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn()));

        assertThat(rows).extracting(row -> row.get(4))
                .containsExactly("Cash", "Bank transfer", "Bank transfer", "Mobile wallet", "Card");
    }

    // 7
    @Test
    void recordedByIsTheAdminsName() throws Exception {
        List<List<Object>> rows = dataRows(sheet(export(admin("school"), NOVEMBER).andExpect(status().isOk()).andReturn()));

        assertThat(rows).extracting(row -> row.get(10)).containsOnly("Mona Hassan");
    }

    // 8
    @Test
    void theOtherClubsAdminSeesOnlyTheirOwnPayment() throws Exception {
        List<List<Object>> sheet = sheet(export(admin("cairo-eq"), NOVEMBER).andExpect(status().isOk()).andReturn());

        List<List<Object>> rows = dataRows(sheet);
        assertThat(rows).extracting(row -> row.get(1)).containsExactly("Karim");
        assertThat(sheet.get(sheet.size() - 1).get(3)).isEqualTo(3000.0);
    }

    // 9
    @Test
    void aRangeWithoutPaymentsHasTheHeaderAndAZeroTotal() throws Exception {
        MvcResult result = export(admin("school"), PAYMENTS + "?from=2026-08-01&to=2026-08-31")
                .andExpect(status().isOk()).andReturn();

        List<List<Object>> sheet = sheet(result);
        assertThat(sheet).hasSize(2);
        assertThat(sheet.get(0)).isEqualTo(List.copyOf(HEADERS));
        assertThat(sheet.get(1).get(0)).isEqualTo("Total (valid)");
        assertThat(sheet.get(1).get(3)).isEqualTo(0.0);
        assertThat(sheet.get(1).get(7)).isEqualTo(0.0);
        assertThat(titleLine(result, 1)).isEqualTo("Payments");
    }

    // 10
    @Test
    void fromIsRequired() throws Exception {
        export(admin("school"), PAYMENTS + "?to=2026-11-30")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("from"));
    }

    // 11
    @Test
    void aTrainerMayNotExport() throws Exception {
        export(signIn("school", "01051000001"), NOVEMBER).andExpect(status().isForbidden());
    }

    // 12: the view keeps the row-level security of payment, even for SQL that names no club.
    @Test
    void theViewShowsOnlyTheCurrentClubsPayments() {
        UUID cairo = cairoEq.clubId();
        List<UUID> clubs = TenantContext.runAs(cairo,
                () -> jdbc.queryForList("SELECT DISTINCT club_id FROM report_payment_v", UUID.class));
        Integer seen = TenantContext.runAs(cairo,
                () -> jdbc.queryForObject("SELECT count(*) FROM report_payment_v", Integer.class));

        assertThat(clubs).containsExactly(cairo);
        assertThat(seen).isEqualTo((int) fx.count("payment", cairo));
        assertThat(TenantContext.runAs(cairo, () -> jdbc.queryForObject("SELECT current_user", String.class)))
                .isEqualTo("equis_app");
    }

    // ---- data

    private void addSample() {
        UUID a = school.clubId();
        UUID mona = jdbc.queryForObject("UPDATE admin_account SET name = 'Mona Hassan' WHERE club_id = ? RETURNING id",
                UUID.class, a);
        UUID omar = rider(a, "Omar", school.trainer1());
        UUID laila = rider(a, "Laila", school.trainer1());
        UUID youssef = rider(a, "Youssef", school.trainer1());
        UUID nour = rider(a, "Nour", school.trainer1());
        pay(a, omar, mona, "2400", "cash", LocalDate.of(2026, 11, 1), 1, 8, cairo(2026, 11, 2, 10, 0));
        UUID wrong = pay(a, laila, mona, "4500", "bank_transfer", LocalDate.of(2026, 11, 1), 1, 16,
                cairo(2026, 11, 3, 11, 0));
        jdbc.update("UPDATE payment SET voided_at = ?, voided_by = ?, void_reason = 'Wrong amount' WHERE id = ?",
                Timestamp.from(cairo(2026, 11, 3, 11, 4)), mona, wrong);
        pay(a, laila, mona, "4800", "bank_transfer", LocalDate.of(2026, 11, 1), 1, 16, cairo(2026, 11, 3, 11, 5));
        pay(a, youssef, mona, "2400", "mobile_wallet", LocalDate.of(2026, 12, 1), 1, 8, cairo(2026, 11, 10, 18, 0));
        pay(a, nour, mona, "7200", "card", LocalDate.of(2026, 12, 1), 3, 24, cairo(2026, 11, 25, 12, 0));

        UUID edge = rider(a, "Edge", school.trainer1());
        pay(a, edge, mona, "100", "other", LocalDate.of(2025, 11, 1), 1, 1, cairo(2025, 10, 31, 23, 30));
        pay(a, edge, mona, "100", "other", LocalDate.of(2025, 11, 1), 1, 1, cairo(2025, 11, 30, 23, 59));
        pay(a, edge, mona, "100", "other", LocalDate.of(2025, 12, 1), 1, 1, cairo(2025, 12, 1, 0, 0));

        UUID b = cairoEq.clubId();
        UUID bAdmin = jdbc.queryForObject("SELECT id FROM admin_account WHERE club_id = ?", UUID.class, b);
        UUID karim = rider(b, "Karim", cairoEq.trainer1());
        pay(b, karim, bAdmin, "3000", "cash", LocalDate.of(2026, 11, 1), 1, 8, cairo(2026, 11, 15, 9, 0));
    }

    private UUID rider(UUID club, String name, UUID trainer) {
        return jdbc.queryForObject("INSERT INTO rider (club_id, name, level_id, trainer_id) VALUES (?, ?, 1, ?) RETURNING id",
                UUID.class, club, name, trainer);
    }

    private UUID pay(UUID club, UUID rider, UUID admin, String amount, String method, LocalDate startMonth, int months,
                     int sessions, Instant recordedAt) {
        return jdbc.queryForObject("""
                        INSERT INTO payment (club_id, rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_at, recorded_by)
                        VALUES (?, ?, ?::numeric, ?, ?, ?, ?, ?, ?, ?) RETURNING id""",
                UUID.class, club, rider, amount, method, startMonth, months, sessions,
                startMonth.plusMonths(months + 1).minusDays(1), Timestamp.from(recordedAt), admin);
    }

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
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

    // The header, the data rows and the total row. A text cell is a String (blank ""), a date or date-time a
    // LocalDateTime, any other number a Double.
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
