package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TestPostgres;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_EMAIL;
import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_PASSWORD;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · the super admin: created by the ops script, signs in on /platform/auth/login, reaches only /platform/**.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Import({TrainerAccountsFixture.Config.class, PlatformSignInJourneyTest.PlatformPingController.class})
class PlatformSignInJourneyTest {
    private static final Path SCRIPT = Path.of("ops", "create-super-admin.sql");
    private static final String EMAIL = "ops@equis.app";
    private static final String PASSWORD = "Platform@2026";

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
        TrainerAccountsFixture.adminAccount(registry);
    }

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(START);
    }

    @Autowired
    TrainerAccountsFixture fx;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JwtDecoder jwtDecoder;
    @Autowired
    JsonMapper json;

    // Only super admin tokens reach it; stands in for the platform endpoints of later tickets.
    @RestController
    static class PlatformPingController {
        @GetMapping("/platform/ping")
        String ping() {
            return "root=" + TenantContext.isRoot();
        }
    }

    @BeforeEach
    void setUp() {
        fx.reset();
        jdbc.execute("truncate table super_admin");
    }

    // 1
    @Test
    void theScriptStoresOneLowercaseBcryptRowAndCanRunTwice() throws Exception {
        runScript("OPS@Equis.app", PASSWORD);
        runScript("OPS@Equis.app", PASSWORD);

        List<Map<String, Object>> rows = jdbc.queryForList("select email, password_hash from super_admin");
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("email")).isEqualTo(EMAIL);
            assertThat((String) row.get("password_hash")).startsWith("$2a$10$");
        });
    }

    // 2
    @Test
    void signInGivesASuperAdminTokenWithoutAClub() throws Exception {
        runScript(EMAIL, PASSWORD);

        AuthTokens tokens = tokens(platformLogin(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN")));

        Jwt jwt = jwtDecoder.decode(tokens.accessToken());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("SUPER_ADMIN");
        assertThat(jwt.hasClaim("cid")).isFalse();
        assertThat(jdbc.queryForList("select club_id, subject_type from refresh_token")).singleElement()
                .satisfies(row -> {
                    assertThat(row.get("club_id")).isNull();
                    assertThat(row.get("subject_type")).isEqualTo("super_admin");
                });
    }

    // 3
    @Test
    void refreshKeepsTheTokenWithoutAClub() throws Exception {
        runScript(EMAIL, PASSWORD);
        AuthTokens tokens = superAdminTokens();

        AuthTokens refreshed = tokens(fx.refresh(tokens)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN")));

        assertThat(jwtDecoder.decode(refreshed.accessToken()).hasClaim("cid")).isFalse();
        asSuperAdmin(refreshed, "/platform/ping").andExpect(status().isOk());
    }

    // 4
    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        runScript(EMAIL, PASSWORD);

        String wrong = platformLogin(EMAIL, "Wrong@2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andReturn().getResponse().getContentAsString();
        String unknown = platformLogin("nobody@equis.app", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(unknown).get("detail")).isEqualTo(json.readTree(wrong).get("detail"));
    }

    // 5
    @Test
    void fiveWrongPasswordsLockTheAccountFor15Minutes() throws Exception {
        runScript(EMAIL, PASSWORD);

        for (int i = 1; i < 5; i++) {
            platformLogin(EMAIL, "Wrong@2026").andExpect(status().isUnauthorized());
        }
        platformLogin(EMAIL, "Wrong@2026")
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("account_locked"));
        platformLogin(EMAIL, PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("account_locked"));

        fx.advance(Duration.ofMinutes(15).plusSeconds(1));

        platformLogin(EMAIL, PASSWORD).andExpect(status().isOk());
    }

    // 6
    @Test
    void aClubAdminCannotSignInOnThePlatform() throws Exception {
        platformLogin(ADMIN_EMAIL, ADMIN_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    // 7
    @Test
    void theSuperAdminCannotSignInOnTheClubSignIn() throws Exception {
        runScript(EMAIL, PASSWORD);

        fx.login(EMAIL, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    // 8
    @Test
    void aSuperAdminTokenIsRefusedOnEveryClubPath() throws Exception {
        runScript(EMAIL, PASSWORD);
        AuthTokens tokens = superAdminTokens();

        for (String path : List.of("/admin/riders", "/me/riders", "/rider/me", "/assessment/slots")) {
            asSuperAdmin(tokens, path)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
    }

    // 9
    @Test
    void aClubAdminTokenIsRefusedOnThePlatform() throws Exception {
        fx.admin(get("/platform/ping"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    // 10
    @Test
    void platformRequestsRunInRootMode() throws Exception {
        runScript(EMAIL, PASSWORD);

        asSuperAdmin(superAdminTokens(), "/platform/ping")
                .andExpect(status().isOk())
                .andExpect(content().string("root=true"));
    }

    // 11
    @Test
    void aDeletedSuperAdminIsSignedOut() throws Exception {
        runScript(EMAIL, PASSWORD);
        AuthTokens tokens = superAdminTokens();

        jdbc.update("delete from super_admin");

        asSuperAdmin(tokens, "/platform/ping").andExpect(status().isUnauthorized());
        fx.refresh(tokens)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
    }

    @Test
    void missingOrMalformedFieldsAreAValidationError() throws Exception {
        platformLogin("not-an-email", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        platformLogin(EMAIL, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    // Runs the real ops script, with the psql variables filled in, as the database owner (CREATE EXTENSION).
    private static void runScript(String email, String password) throws Exception {
        String sql = Files.readString(SCRIPT)
                .replace(":'email'", literal(email))
                .replace(":'pwd'", literal(password));
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrlSeeingAllClubs(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private ResultActions platformLogin(String email, String password) throws Exception {
        return fx.perform(post("/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(email, password)));
    }

    private AuthTokens superAdminTokens() throws Exception {
        return tokens(platformLogin(EMAIL, PASSWORD).andExpect(status().isOk()));
    }

    private ResultActions asSuperAdmin(AuthTokens tokens, String path) throws Exception {
        return fx.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()));
    }

    private AuthTokens tokens(ResultActions result) throws Exception {
        return json.readValue(result.andReturn().getResponse().getContentAsString(), AuthTokens.class);
    }
}
