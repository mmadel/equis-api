package com.equestrian.equis.controller.reports;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.service.reports.RangeSampleReport;
import com.equestrian.equis.service.reports.ReportExportService;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TestSubscriptions;
import com.equestrian.equis.support.TwoClubFixture;
import com.equestrian.equis.support.TwoClubFixture.ClubData;
import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
import java.time.Instant;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E18 · export engine: GET /admin/reports/{name}/export, cases 1-9, 11, 12, 14 and 15 (10 and 13 are unit tests,
// 16 is the rest of the suite). E18 · Excel writer: cases 14 and 15, the title block of a real export. The two clubs of TwoClubFixture: "school" with trainers "Trainer One A" and
// "Trainer Two A", "cairo-eq" with "Trainer One B" and "Trainer Two B". The clock is 14 Nov 2026 12:00 in Cairo.
// "test-range" is the test-only RangeSampleReport: one row per day from..to.
class ReportExportIT extends IntegrationTestBase {
    private static final Instant NOV_14_NOON = ZonedDateTime.of(2026, 11, 14, 12, 0, 0, 0, CAIRO).toInstant();
    private static final String TRAINERS = "/admin/reports/trainers/export";
    private static final String RANGE = "/admin/reports/" + RangeSampleReport.NAME + "/export";
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    // POI's 0-based index of the header row (Excel row 6).
    private static final int HEADER_ROW = 5;

    @Autowired
    TwoClubFixture fx;
    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;

    ClubData school;
    ClubData cairoEq;

    @BeforeEach
    void bothClubsActiveOn14November() {
        school = fx.a();
        cairoEq = fx.b();
        CLOCK.set(NOV_14_NOON);
        root(() -> jdbc.update("update subscription set ends_at = TIMESTAMPTZ '2200-01-01 00:00:00+00'"));
    }

    // 1
    @Test
    void theAdminDownloadsTheTrainersOfTheirClub() throws Exception {
        MvcResult result = export(admin("school"), TRAINERS).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentType()).isEqualTo(ReportExportService.XLSX);
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"trainers_2026-11-14.xlsx\"");
        List<List<String>> rows = rows(result);
        assertThat(rows).containsExactly(
                List.of("Name", "Email", "Phone", "Status"),
                List.of("Trainer One A", "trainer1@school.eg", "01051000001", "Active"),
                List.of("Trainer Two A", "trainer2@school.eg", "01051000002", "Active"));
    }

    // 2
    @Test
    void theOtherClubsAdminSeesOnlyTheirOwnTrainers() throws Exception {
        List<List<String>> rows = rows(export(admin("cairo-eq"), TRAINERS).andExpect(status().isOk()).andReturn());

        assertThat(rows).extracting(row -> row.get(0))
                .containsExactly("Name", "Trainer One B", "Trainer Two B");
    }

    // 3
    @Test
    void anUnknownReportIsNotFound() throws Exception {
        export(admin("school"), "/admin/reports/unknown/export")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("report_not_found"))
                .andExpect(jsonPath("$.name").value("unknown"));
    }

    // 4
    @Test
    void onlyTheClubAdminMayExport() throws Exception {
        export(signIn("school", "01051000001"), TRAINERS).andExpect(status().isForbidden());
        export(signIn("school", "person1@school.eg"), TRAINERS).andExpect(status().isForbidden());
        export(superAdmin(), TRAINERS).andExpect(status().isForbidden());
    }

    // 5
    @Test
    void aReadOnlyClubCanStillExport() throws Exception {
        String token = admin("school");
        root(() -> jdbc.update("update subscription set ends_at = TIMESTAMPTZ '2026-11-01 00:00:00+00' where club_id = ?",
                school.clubId()));

        export(token, TRAINERS).andExpect(status().isOk());
        // Read-only indeed: a write is refused.
        fx.perform(token, post("/admin/riders/" + school.rider1() + "/invite"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("subscription_expired"));
    }

    // 6
    @Test
    void aMissingOrBadlyFormattedFromIsAFieldError() throws Exception {
        String token = admin("school");
        export(token, RANGE + "?to=2026-11-30")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("from"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1));
        export(token, RANGE + "?from=2026-13-01&to=2026-11-30")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("from"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("invalid_date"));
    }

    // 7
    @Test
    void fromAfterToIsAFieldErrorOnTo() throws Exception {
        export(admin("school"), RANGE + "?from=2026-11-30&to=2026-11-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1));
    }

    // 8
    @Test
    void aWholeYearIsAllowed() throws Exception {
        MvcResult result = export(admin("school"), RANGE + "?from=2026-01-01&to=2026-12-31")
                .andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"test-range_2026-01-01_2026-12-31.xlsx\"");
        List<List<String>> rows = rows(result);
        assertThat(rows).hasSize(1 + 365);
        assertThat(rows.get(1).get(0)).isEqualTo("2026-01-01");
        assertThat(rows.get(365).get(0)).isEqualTo("2026-12-31");
    }

    // 9
    @Test
    void oneDayOverTwelveMonthsIsTooLong() throws Exception {
        export(admin("school"), RANGE + "?from=2026-01-01&to=2027-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("report_range_too_long"))
                .andExpect(jsonPath("$.maxMonths").value(12));
    }

    // 11 and 12
    @Test
    void aSnapshotIgnoresDatesAndUnknownParameters() throws Exception {
        MvcResult result = export(admin("school"), TRAINERS + "?from=2020-01-01&to=2030-12-31&foo=bar")
                .andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"trainers_2026-11-14.xlsx\"");
        assertThat(rows(result)).hasSize(3);
    }

    // 14
    @Test
    void aClubWithNoTrainersGetsTheHeaderOnly() throws Exception {
        String code = "empty-club";
        root(() -> {
            UUID club = jdbc.queryForObject(
                    "INSERT INTO club (code, name) VALUES (?, 'Empty Club') RETURNING id", UUID.class, code);
            jdbc.update("INSERT INTO admin_account (club_id, name, email, password_hash) VALUES (?, 'Admin', ?, ?)",
                    club, "admin@empty.eg", passwordEncoder.encode(PASSWORD));
            TestSubscriptions.validFor(jdbc, club);
            return null;
        });

        List<List<String>> rows = rows(export(signIn(code, "admin@empty.eg"), TRAINERS)
                .andExpect(status().isOk()).andReturn());

        assertThat(rows).containsExactly(List.of("Name", "Email", "Phone", "Status"));
    }

    // 15: the view keeps the trainer table's row-level security, even for SQL that names no club.
    @Test
    void theViewShowsOnlyTheCurrentClubsRows() {
        List<String> seenByCairo = TenantContext.runAs(cairoEq.clubId(),
                () -> jdbc.queryForList("SELECT name FROM report_trainer_v ORDER BY name", String.class));
        List<String> seenBySchool = TenantContext.runAs(school.clubId(),
                () -> jdbc.queryForList("SELECT name FROM report_trainer_v ORDER BY name", String.class));

        assertThat(seenByCairo).containsExactly("Trainer One B", "Trainer Two B");
        assertThat(seenBySchool).containsExactly("Trainer One A", "Trainer Two A");
        assertThat(TenantContext.runAs(cairoEq.clubId(), () -> jdbc.queryForObject("SELECT current_user", String.class)))
                .isEqualTo("equis_app");
    }

    // Excel writer 14
    @Test
    void theTitleBlockNamesTheAdminsClubAndTheDay() throws Exception {
        CLOCK.set(ZonedDateTime.of(2026, 11, 14, 17, 46, 0, 0, CAIRO).toInstant());

        List<String> title = titleBlock(export(admin("school"), TRAINERS).andExpect(status().isOk()).andReturn());

        assertThat(title).containsExactly("Riding School", "Trainers", "As of 14 Nov 2026",
                "Generated 14 Nov 2026 17:46 (Cairo)");
    }

    // Excel writer 15
    @Test
    void theOtherClubsExportCarriesItsOwnName() throws Exception {
        List<String> title = titleBlock(export(admin("cairo-eq"), TRAINERS).andExpect(status().isOk()).andReturn());

        assertThat(title.get(0)).isEqualTo("Cairo Equestrian");
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

    private String superAdmin() throws Exception {
        root(() -> {
            jdbc.update("DELETE FROM super_admin WHERE email = ?", SUPER_EMAIL);
            jdbc.update("INSERT INTO super_admin (email, password_hash) VALUES (?, ?)",
                    SUPER_EMAIL, passwordEncoder.encode(SUPER_PASSWORD));
            return null;
        });
        ResultActions result = mvc.perform(post("/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                                .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk());
        return JsonPath.read(body(result), "$.accessToken");
    }

    // The header and every data row of the file's first sheet, as text (a date as YYYY-MM-DD). The title block
    // above the header (Excel rows 1-4) is left out; titleBlock reads it.
    private static List<List<String>> rows(MvcResult result) throws Exception {
        try (XSSFWorkbook book = workbook(result)) {
            Sheet sheet = book.getSheetAt(0);
            List<List<String>> rows = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() < HEADER_ROW) {
                    continue;
                }
                List<String> cells = new ArrayList<>();
                row.forEach(cell -> cells.add(switch (cell.getCellType()) {
                    case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                            ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                            : String.valueOf((long) cell.getNumericCellValue());
                    case BLANK -> "";
                    default -> cell.getStringCellValue();
                }));
                rows.add(cells);
            }
            return rows;
        }
    }

    // Excel rows 1-4: club, report, period, generated at.
    private static List<String> titleBlock(MvcResult result) throws Exception {
        try (XSSFWorkbook book = workbook(result)) {
            Sheet sheet = book.getSheetAt(0);
            List<String> lines = new ArrayList<>();
            for (int r = 0; r < 4; r++) {
                lines.add(sheet.getRow(r).getCell(0).getStringCellValue());
            }
            return lines;
        }
    }

    private static XSSFWorkbook workbook(MvcResult result) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
