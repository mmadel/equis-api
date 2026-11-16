package com.equestrian.equis;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.service.reports.RangeSampleReport;
import com.equestrian.equis.service.reports.ReportExportService;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TestSubscriptions;
import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.PaneInformation;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E18 · the epic's Demo, step by step through the API, reading every downloaded file back with POI.
// Two clubs, written with SQL so the demo stays short: "school" (Riding School) with the trainers Coach Ahmed and
// Coach Mona, on a Basic subscription that ends on 31 Dec 2026; "cairo-eq" (Cairo Equestrian) with Coach Hany.
// Sat 14 Nov 2026, 17:46 Cairo: each admin exports Trainers and sees only their own club; the test-only
// "test-range" report checks the date range and the 12-month rule; a trainer is refused. Tue 5 Jan 2027: school's
// subscription has ended, the club is read-only, and its admin can still export.
class E18DemoIT extends IntegrationTestBase {
    private static final Instant NOV_14_2026 = ZonedDateTime.of(2026, 11, 14, 17, 46, 0, 0, CAIRO).toInstant();
    private static final Instant JAN_5_2027 = ZonedDateTime.of(2027, 1, 5, 10, 0, 0, 0, CAIRO).toInstant();
    private static final Instant SCHOOL_ENDS = ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, CAIRO).toInstant();
    private static final String TRAINERS = "/admin/reports/trainers/export";
    private static final String RANGE = "/admin/reports/" + RangeSampleReport.NAME + "/export";
    private static final String PASSWORD = "Demo@2026";
    private static final String AHMED_PHONE = "01053000001";
    // POI's 0-based index of the header row (Excel row 6).
    private static final int HEADER_ROW = 5;

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void twoClubsWithTheirTrainers() {
        CLOCK.set(NOV_14_2026);
        String hash = passwordEncoder.encode(PASSWORD);
        root(() -> {
            // Start from no school data at all (the seeded trainers of V4 go too).
            jdbc.execute("truncate table club cascade");
            jdbc.update("INSERT INTO club (id, code, name) VALUES (?, 'school', 'Riding School')", Club.DEFAULT_ID);
            UUID cairoEq = jdbc.queryForObject(
                    "INSERT INTO club (code, name) VALUES ('cairo-eq', 'Cairo Equestrian') RETURNING id", UUID.class);
            admin(Club.DEFAULT_ID, "admin@school.eg", hash);
            admin(cairoEq, "admin@cairo-eq.eg", hash);
            trainer(Club.DEFAULT_ID, "Coach Ahmed", AHMED_PHONE, "ahmed@school.eg", hash);
            trainer(Club.DEFAULT_ID, "Coach Mona", "01053000002", "mona@school.eg", hash);
            trainer(cairoEq, "Coach Hany", "01053000003", "hany@cairo-eq.eg", hash);

            jdbc.update("""
                    INSERT INTO plan (code, name, on_expiry) SELECT 'BASIC', 'Basic', 'READ_ONLY'
                    WHERE NOT EXISTS (SELECT 1 FROM plan WHERE code = 'BASIC')""");
            jdbc.update("""
                    INSERT INTO subscription (club_id, plan_id, billing_period, starts_at, ends_at, note)
                    SELECT ?, id, 'ANNUAL', TIMESTAMPTZ '2026-01-01 00:00:00+02', ?, 'E18 demo'
                    FROM plan WHERE code = 'BASIC'""", Club.DEFAULT_ID, Timestamp.from(SCHOOL_ENDS));
            TestSubscriptions.validFor(jdbc, cairoEq);
            return null;
        });
    }

    @Test
    void theEpicDemo() throws Exception {
        // 1. The school admin exports Trainers: title block, bold frozen filtered header, Ahmed and Mona.
        String school = signIn("school", "admin@school.eg");
        MvcResult trainers = export(school, TRAINERS).andExpect(status().isOk()).andReturn();
        assertThat(trainers.getResponse().getContentType()).isEqualTo(ReportExportService.XLSX);
        assertThat(trainers.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"trainers_2026-11-14.xlsx\"");
        try (XSSFWorkbook book = workbook(trainers)) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(titleBlock(sheet)).containsExactly("Riding School", "Trainers", "As of 14 Nov 2026",
                    "Generated 14 Nov 2026 17:46 (Cairo)");
            assertThat(rows(sheet)).containsExactly(
                    List.of("Name", "Email", "Phone", "Status"),
                    List.of("Coach Ahmed", "ahmed@school.eg", AHMED_PHONE, "Active"),
                    List.of("Coach Mona", "mona@school.eg", "01053000002", "Active"));

            Row header = sheet.getRow(HEADER_ROW);
            for (Cell cell : header) {
                assertThat(book.getFontAt(cell.getCellStyle().getFontIndex()).getBold())
                        .as("header " + cell.getStringCellValue()).isTrue();
            }
            PaneInformation pane = sheet.getPaneInformation();
            assertThat(pane.isFreezePane()).isTrue();
            assertThat(pane.getHorizontalSplitPosition()).isEqualTo((short) 6);
            assertThat(sheet.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A6:D8");
        }

        // 2. The cairo-eq admin gets only Hany, under their own club's name.
        String cairoEq = signIn("cairo-eq", "admin@cairo-eq.eg");
        try (XSSFWorkbook book = workbook(export(cairoEq, TRAINERS).andExpect(status().isOk()).andReturn())) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(titleBlock(sheet).get(0)).isEqualTo("Cairo Equestrian");
            assertThat(rows(sheet)).extracting(row -> row.get(0)).containsExactly("Name", "Coach Hany");
        }

        // 3. A range report for November: the period in row 3, one row per day.
        MvcResult november = export(school, RANGE + "?from=2026-11-01&to=2026-11-30")
                .andExpect(status().isOk()).andReturn();
        assertThat(november.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"test-range_2026-11-01_2026-11-30.xlsx\"");
        try (XSSFWorkbook book = workbook(november)) {
            XSSFSheet sheet = book.getSheetAt(0);
            assertThat(titleBlock(sheet).get(2)).isEqualTo("1 Nov 2026 – 30 Nov 2026");
            List<List<String>> rows = rows(sheet);
            assertThat(rows).hasSize(1 + 30);
            assertThat(rows.get(1).get(1)).isEqualTo("1");
            assertThat(rows.get(30).get(1)).isEqualTo("30");
        }

        // 4. Twelve months and a day is too long.
        export(school, RANGE + "?from=2026-01-01&to=2027-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("report_range_too_long"))
                .andExpect(jsonPath("$.maxMonths").value(12));

        // 5. A trainer may not export.
        export(signIn("school", AHMED_PHONE), TRAINERS).andExpect(status().isForbidden());

        // 6. Past the end of school's subscription the club is read-only, and the export still works.
        CLOCK.set(JAN_5_2027);
        String token = token(login("school", "admin@school.eg")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access.state").value("READ_ONLY"))
                .andExpect(jsonPath("$.access.reason").value("SUBSCRIPTION_ENDED")));
        MvcResult readOnly = export(token, TRAINERS).andExpect(status().isOk()).andReturn();
        assertThat(readOnly.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"trainers_2027-01-05.xlsx\"");
        try (XSSFWorkbook book = workbook(readOnly)) {
            assertThat(titleBlock(book.getSheetAt(0)).get(2)).isEqualTo("As of 5 Jan 2027");
        }
    }

    // ---- helpers

    private void admin(UUID club, String email, String hash) {
        jdbc.update("INSERT INTO admin_account (club_id, name, email, password_hash) VALUES (?, 'Admin', ?, ?)",
                club, email, hash);
    }

    private void trainer(UUID club, String name, String phone, String email, String hash) {
        jdbc.update("""
                INSERT INTO trainer (club_id, name, phone, email, password_hash, status)
                VALUES (?, ?, ?, ?, ?, 'active')""", club, name, phone, email, hash);
    }

    private ResultActions login(String clubCode, String login) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"%s\", \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(clubCode, login, PASSWORD)));
    }

    private String signIn(String clubCode, String login) throws Exception {
        return token(login(clubCode, login).andExpect(status().isOk()));
    }

    private ResultActions export(String token, String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    private static String token(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private static XSSFWorkbook workbook(MvcResult result) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
    }

    // Excel rows 1-4: club, report, period, generated at.
    private static List<String> titleBlock(XSSFSheet sheet) {
        List<String> lines = new ArrayList<>();
        for (int r = 0; r < 4; r++) {
            lines.add(sheet.getRow(r).getCell(0).getStringCellValue());
        }
        return lines;
    }

    // The header and every data row, as text (a number without decimals; a date cell is not read here).
    private static List<List<String>> rows(XSSFSheet sheet) {
        List<List<String>> rows = new ArrayList<>();
        for (Row row : sheet) {
            if (row.getRowNum() < HEADER_ROW) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            row.forEach(cell -> cells.add(cell.getCellType() == CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getStringCellValue()));
            rows.add(cells);
        }
        return rows;
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
