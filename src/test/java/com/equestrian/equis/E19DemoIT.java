package com.equestrian.equis;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.service.reports.ReportExportService;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TestSubscriptions;
import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E19 · the epic's Demo, in date order through the API, reading every downloaded file back with POI. One dataset, the
// epic page's Al Forsan sample, written with SQL so the demo stays short. Two clubs: "school" (Al Forsan Riding Club),
// admins Sara Adel and Karim Nabil, trainers Ahmed Samir, Mona Fathy and Karim Saad; "cairo-eq" (Cairo Equestrian)
// with one rider, Karim Adel.
// - Mon 16 Nov 2026, 20:30 Cairo: the admin exports Rider balances. The epic's five riders show the epic's numbers;
//   Mariam Khaled is a rider too, and her Sep-Oct package (16 bought, 11 used, valid until 30 Nov) still counts that
//   day, so the file has six riders and totals 56 / 37 / 19 (the epic's 40 / 26 / 14 plus Mariam's 16 / 11 / 5).
// - 25 Nov: Nour Ibrahim joins and pays for December to February.
// - Tue 1 Dec, 09:12: Payments for 1-30 Nov, five rows, Laila's 4,500 voided, total 16,800.00 EGP / 56 sessions.
// - 09:15: Expired sessions for 1-30 Nov, Mariam 5 and Youssef 1, total 6.
// - The same three exports as the cairo-eq admin hold none of school's rows.
class E19DemoIT extends IntegrationTestBase {
    private static final Instant NOV_16_EVENING = cairo(2026, 11, 16, 20, 30);
    private static final Instant DEC_1_PAYMENTS = cairo(2026, 12, 1, 9, 12);
    private static final Instant DEC_1_EXPIRED = cairo(2026, 12, 1, 9, 15);
    private static final String PAYMENTS = "/admin/reports/payments/export?from=2026-11-01&to=2026-11-30";
    private static final String RIDER_BALANCES = "/admin/reports/rider-balances/export";
    private static final String EXPIRED_SESSIONS = "/admin/reports/expired-sessions/export?from=2026-11-01&to=2026-11-30";
    private static final String PASSWORD = "Demo@2026";
    private static final List<String> SCHOOL_RIDERS = List.of("Omar Hassan", "Laila Mostafa", "Youssef Ali",
            "Hana Tarek", "Ziad Fouad", "Mariam Khaled", "Nour Ibrahim");
    // POI's 0-based index of the header row (Excel row 6).
    private static final int HEADER_ROW = 5;

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;

    UUID cairoEq;
    UUID sara;
    UUID karimSaad;

    @BeforeEach
    void alForsanUpToTheSixteenthOfNovember() {
        CLOCK.set(NOV_16_EVENING);
        String hash = passwordEncoder.encode(PASSWORD);
        root(() -> {
            // Start from no school data at all (the seeded trainers of V4 go too).
            jdbc.execute("truncate table club cascade");
            jdbc.update("INSERT INTO club (id, code, name) VALUES (?, 'school', 'Al Forsan Riding Club')",
                    Club.DEFAULT_ID);
            cairoEq = jdbc.queryForObject(
                    "INSERT INTO club (code, name) VALUES ('cairo-eq', 'Cairo Equestrian') RETURNING id", UUID.class);
            TestSubscriptions.validFor(jdbc, Club.DEFAULT_ID);
            TestSubscriptions.validFor(jdbc, cairoEq);

            UUID a = Club.DEFAULT_ID;
            sara = admin(a, "Sara Adel", "admin@school.eg", hash);
            UUID karimNabil = admin(a, "Karim Nabil", "karim@school.eg", hash);
            UUID ahmed = trainer(a, "Ahmed Samir", "01054000001", hash);
            UUID mona = trainer(a, "Mona Fathy", "01054000002", hash);
            karimSaad = trainer(a, "Karim Saad", "01054000003", hash);
            List<UUID> seats = sessions(a, 11);

            UUID omar = rider(a, "Omar Hassan", ahmed);
            UUID laila = rider(a, "Laila Mostafa", mona);
            UUID youssef = rider(a, "Youssef Ali", ahmed);
            UUID hana = rider(a, "Hana Tarek", karimSaad);
            rider(a, "Ziad Fouad", mona);
            UUID mariam = rider(a, "Mariam Khaled", mona);

            // Bought before November: they are not in November's payments.
            use(a, mariam, pay(a, mariam, sara, "4800", "cash", month(9), 2, 16, cairo(2026, 9, 1, 10, 0)), seats, 11);
            use(a, youssef, pay(a, youssef, sara, "2400", "cash", month(10), 1, 8, cairo(2026, 10, 2, 9, 0)), seats, 7);
            use(a, hana, pay(a, hana, sara, "2400", "cash", month(11), 1, 8, cairo(2026, 10, 28, 17, 0)), seats, 8);
            // November's payments up to the 16th.
            use(a, omar, pay(a, omar, sara, "2400", "cash", month(11), 1, 8, cairo(2026, 11, 2, 10, 15)), seats, 5);
            UUID wrong = pay(a, laila, sara, "4500", "bank_transfer", month(11), 2, 16, cairo(2026, 11, 3, 18, 40));
            jdbc.update("UPDATE payment SET voided_at = ?, voided_by = ?, void_reason = 'Wrong amount' WHERE id = ?",
                    Timestamp.from(cairo(2026, 11, 3, 18, 44)), sara, wrong);
            use(a, laila, pay(a, laila, sara, "4800", "bank_transfer", month(11), 2, 16, cairo(2026, 11, 3, 18, 45)),
                    seats, 6);
            pay(a, youssef, karimNabil, "2400", "mobile_wallet", month(12), 1, 8, cairo(2026, 11, 10, 12, 5));

            // cairo-eq: Karim Adel lost 5 of an October package, and paid for November on the 15th.
            UUID bAdmin = admin(cairoEq, "Hany Admin", "admin@cairo-eq.eg", hash);
            UUID karimAdel = rider(cairoEq, "Karim Adel", trainer(cairoEq, "Coach Hany", "01054000004", hash));
            use(cairoEq, karimAdel, pay(cairoEq, karimAdel, bAdmin, "2400", "cash", month(10), 1, 8,
                    cairo(2026, 10, 1, 9, 0)), sessions(cairoEq, 3), 3);
            pay(cairoEq, karimAdel, bAdmin, "3000", "cash", month(11), 1, 8, cairo(2026, 11, 15, 9, 0));
            return null;
        });
    }

    @Test
    void theEpicDemo() throws Exception {
        String school = signIn("school", "admin@school.eg");

        // 1. 16 Nov, 20:30: Rider balances as of today, by rider name.
        MvcResult balances = export(school, RIDER_BALANCES).andExpect(status().isOk()).andReturn();
        assertThat(balances.getResponse().getContentType()).isEqualTo(ReportExportService.XLSX);
        assertThat(balances.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"rider-balances_2026-11-16.xlsx\"");
        assertThat(titleBlock(balances)).containsExactly("Al Forsan Riding Club", "Rider balances",
                "As of 16 Nov 2026", "Generated 16 Nov 2026 20:30 (Cairo)");
        List<List<Object>> sheet = sheet(balances, 7);
        assertThat(sheet.get(0)).containsExactly("Rider", "Trainer", "Status", "Bought", "Used", "Remaining",
                "Next expiry");
        assertThat(dataRows(sheet)).containsExactly(
                List.of("Hana Tarek", "Karim Saad", "Awaiting payment", 8.0, 8.0, 0.0, ""),
                List.of("Laila Mostafa", "Mona Fathy", "Active", 16.0, 6.0, 10.0, day(2027, 1, 31)),
                List.of("Mariam Khaled", "Mona Fathy", "Active", 16.0, 11.0, 5.0, day(2026, 11, 30)),
                List.of("Omar Hassan", "Ahmed Samir", "Active", 8.0, 5.0, 3.0, day(2026, 12, 31)),
                List.of("Youssef Ali", "Ahmed Samir", "Active", 8.0, 7.0, 1.0, day(2026, 11, 30)),
                List.of("Ziad Fouad", "Mona Fathy", "Awaiting payment", 0.0, 0.0, 0.0, ""));
        // The epic's 40 / 26 / 14 for its five riders, plus Mariam's 16 / 11 / 5.
        assertThat(total(sheet).get(0)).isEqualTo("Total");
        assertThat(total(sheet).subList(3, 6)).containsExactly(56.0, 37.0, 19.0);

        // 2. 25 Nov: Nour joins and pays for December to February.
        root(() -> {
            UUID nour = rider(Club.DEFAULT_ID, "Nour Ibrahim", karimSaad);
            return pay(Club.DEFAULT_ID, nour, sara, "7200", "card", month(12), 3, 24, cairo(2026, 11, 25, 17, 30));
        });

        // 3. 1 Dec, 09:12: November's payments in recorded order; the voided 4,500 stays but is not in the total.
        // A new day: the admin signs in again (yesterday's access token has expired).
        CLOCK.set(DEC_1_PAYMENTS);
        school = signIn("school", "admin@school.eg");
        MvcResult payments = export(school, PAYMENTS).andExpect(status().isOk()).andReturn();
        assertThat(payments.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"payments_2026-11-01_2026-11-30.xlsx\"");
        assertThat(titleBlock(payments)).containsExactly("Al Forsan Riding Club", "Payments",
                "1 Nov 2026 – 30 Nov 2026", "Generated 1 Dec 2026 09:12 (Cairo)");
        sheet = sheet(payments, 11);
        assertThat(sheet.get(0)).containsExactly("Recorded at", "Rider", "Trainer", "Amount", "Method",
                "Start month", "Months", "Sessions", "Status", "Void reason", "Recorded by");
        assertThat(dataRows(sheet)).containsExactly(
                List.of(at(2, 10, 15), "Omar Hassan", "Ahmed Samir", 2400.0, "Cash", day(2026, 11, 1), 1.0, 8.0,
                        "Valid", "", "Sara Adel"),
                List.of(at(3, 18, 40), "Laila Mostafa", "Mona Fathy", 4500.0, "Bank transfer", day(2026, 11, 1), 2.0,
                        16.0, "Voided", "Wrong amount", "Sara Adel"),
                List.of(at(3, 18, 45), "Laila Mostafa", "Mona Fathy", 4800.0, "Bank transfer", day(2026, 11, 1), 2.0,
                        16.0, "Valid", "", "Sara Adel"),
                List.of(at(10, 12, 5), "Youssef Ali", "Ahmed Samir", 2400.0, "Mobile wallet", day(2026, 12, 1), 1.0,
                        8.0, "Valid", "", "Karim Nabil"),
                List.of(at(25, 17, 30), "Nour Ibrahim", "Karim Saad", 7200.0, "Card", day(2026, 12, 1), 3.0, 24.0,
                        "Valid", "", "Sara Adel"));
        assertThat(total(sheet).get(0)).isEqualTo("Total (valid)");
        assertThat(total(sheet).get(3)).isEqualTo(16800.0);
        assertThat(total(sheet).get(7)).isEqualTo(56.0);

        // 4. 1 Dec, 09:15: the sessions lost to expiry in November, by expiry date then name.
        CLOCK.set(DEC_1_EXPIRED);
        MvcResult expired = export(school, EXPIRED_SESSIONS).andExpect(status().isOk()).andReturn();
        assertThat(expired.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"expired-sessions_2026-11-01_2026-11-30.xlsx\"");
        assertThat(titleBlock(expired)).containsExactly("Al Forsan Riding Club", "Expired sessions",
                "1 Nov 2026 – 30 Nov 2026", "Generated 1 Dec 2026 09:15 (Cairo)");
        sheet = sheet(expired, 8);
        assertThat(sheet.get(0)).containsExactly("Rider", "Trainer", "Package start", "Months", "Expired on",
                "Bought", "Used", "Expired");
        assertThat(dataRows(sheet)).containsExactly(
                List.of("Mariam Khaled", "Mona Fathy", day(2026, 9, 1), 2.0, day(2026, 11, 30), 16.0, 11.0, 5.0),
                List.of("Youssef Ali", "Ahmed Samir", day(2026, 10, 1), 1.0, day(2026, 11, 30), 8.0, 7.0, 1.0));
        assertThat(total(sheet).get(0)).isEqualTo("Total");
        assertThat(total(sheet).get(7)).isEqualTo(6.0);

        // 5. The cairo-eq admin's three files hold only Karim Adel, under their own club's name.
        // (Balances are as of today, so that file is taken back on 16 Nov; each day starts with a fresh sign-in.)
        CLOCK.set(NOV_16_EVENING);
        String cairo = signIn("cairo-eq", "admin@cairo-eq.eg");
        assertOnlyCairoEq(export(cairo, RIDER_BALANCES).andExpect(status().isOk()).andReturn(), 7);
        CLOCK.set(DEC_1_PAYMENTS);
        cairo = signIn("cairo-eq", "admin@cairo-eq.eg");
        MvcResult cairoPayments = export(cairo, PAYMENTS).andExpect(status().isOk()).andReturn();
        assertOnlyCairoEq(cairoPayments, 11);
        assertThat(total(sheet(cairoPayments, 11)).get(3)).isEqualTo(3000.0);
        CLOCK.set(DEC_1_EXPIRED);
        MvcResult cairoExpired = export(cairo, EXPIRED_SESSIONS).andExpect(status().isOk()).andReturn();
        assertOnlyCairoEq(cairoExpired, 8);
        assertThat(total(sheet(cairoExpired, 8)).get(7)).isEqualTo(5.0);
    }

    private void assertOnlyCairoEq(MvcResult result, int columns) throws Exception {
        assertThat(titleBlock(result).get(0)).isEqualTo("Cairo Equestrian");
        List<List<Object>> rows = dataRows(sheet(result, columns));
        // The rider's name is column 1 in Payments and column 0 in the other two.
        int rider = columns == 11 ? 1 : 0;
        assertThat(rows).extracting(row -> row.get(rider)).containsOnly("Karim Adel").isNotEmpty();
        assertThat(rows).flatExtracting(row -> row).doesNotContainAnyElementsOf(SCHOOL_RIDERS)
                .doesNotContain("Ahmed Samir", "Mona Fathy", "Karim Saad", "Sara Adel", "Karim Nabil");
    }

    // ---- data

    private UUID admin(UUID club, String name, String email, String hash) {
        return jdbc.queryForObject("""
                INSERT INTO admin_account (club_id, name, email, password_hash) VALUES (?, ?, ?, ?) RETURNING id""",
                UUID.class, club, name, email, hash);
    }

    private UUID trainer(UUID club, String name, String phone, String hash) {
        return jdbc.queryForObject("""
                INSERT INTO trainer (club_id, name, phone, email, password_hash, status)
                VALUES (?, ?, ?, ?, ?, 'active') RETURNING id""",
                UUID.class, club, name, phone, phone + "@trainers.eg", hash);
    }

    private UUID rider(UUID club, String name, UUID trainer) {
        return jdbc.queryForObject(
                "INSERT INTO rider (club_id, name, level_id, trainer_id) VALUES (?, ?, 1, ?) RETURNING id",
                UUID.class, club, name, trainer);
    }

    // A package: valid until the end of the month after its last month (the grace month).
    private UUID pay(UUID club, UUID rider, UUID admin, String amount, String method, LocalDate startMonth, int months,
                     int sessions, Instant recordedAt) {
        return jdbc.queryForObject("""
                        INSERT INTO payment (club_id, rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_at, recorded_by)
                        VALUES (?, ?, ?::numeric, ?, ?, ?, ?, ?, ?, ?) RETURNING id""",
                UUID.class, club, rider, amount, method, startMonth, months, sessions,
                startMonth.plusMonths(months + 1).minusDays(1), Timestamp.from(recordedAt), admin);
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
                    club, template, month(11).plusDays(i)));
        }
        return ids;
    }

    // The rider used count sessions of the package, one per session.
    private void use(UUID club, UUID rider, UUID payment, List<UUID> seats, int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("INSERT INTO session_usage (club_id, rider_id, payment_id, session_id) VALUES (?, ?, ?, ?)",
                    club, rider, payment, seats.get(i));
        }
    }

    private static LocalDate month(int month) {
        return LocalDate.of(2026, month, 1);
    }

    private static Instant cairo(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, CAIRO).toInstant();
    }

    // A date cell, as POI reads it back.
    private static LocalDateTime day(int year, int month, int day) {
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    // A November 2026 date-time cell (Cairo time, as the file shows it).
    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 11, day, hour, minute);
    }

    // ---- helpers

    private String signIn(String clubCode, String login) throws Exception {
        ResultActions result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"%s\", \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(clubCode, login, PASSWORD)))
                .andExpect(status().isOk());
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private ResultActions export(String token, String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    private static XSSFWorkbook workbook(MvcResult result) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
    }

    // Excel rows 1-4: club, report, period, generated at.
    private static List<String> titleBlock(MvcResult result) throws Exception {
        try (XSSFWorkbook book = workbook(result)) {
            XSSFSheet sheet = book.getSheetAt(0);
            List<String> lines = new ArrayList<>();
            for (int r = 0; r < 4; r++) {
                lines.add(sheet.getRow(r).getCell(0).getStringCellValue());
            }
            return lines;
        }
    }

    // The header, the data rows and the total row. A text cell is a String (blank ""), a date or date-time a
    // LocalDateTime, any other number a Double.
    private static List<List<Object>> sheet(MvcResult result, int columns) throws Exception {
        try (XSSFWorkbook book = workbook(result)) {
            List<List<Object>> rows = new ArrayList<>();
            for (Row row : book.getSheetAt(0)) {
                if (row.getRowNum() < HEADER_ROW) {
                    continue;
                }
                List<Object> cells = new ArrayList<>();
                for (int c = 0; c < columns; c++) {
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

    private static List<Object> total(List<List<Object>> sheet) {
        return sheet.get(sheet.size() - 1);
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
