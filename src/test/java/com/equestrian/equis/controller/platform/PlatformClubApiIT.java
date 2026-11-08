package com.equestrian.equis.controller.platform;

import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.equestrian.equis.support.TestMembers;
import com.equestrian.equis.support.TrainerAccountsFixture;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_EMAIL;
import static com.equestrian.equis.support.TrainerAccountsFixture.ADMIN_PASSWORD;
import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · club onboarding: the super admin creates a club with its first admin and a trial, lists and opens clubs,
// and suspends or reactivates them. The new admin sets a password from the emailed link and signs in with the code.
// Clock from Sat 7 Nov 2026, 15:00 Cairo; the four plans come from ops/upsert-plan.sql.
@ExtendWith(OutputCaptureExtension.class)
class PlatformClubApiIT extends IntegrationTestBase {
    private static final String CLUBS = "/platform/clubs";
    private static final Path UPSERT_PLAN = Path.of("ops", "upsert-plan.sql");
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    private static final Instant NOV_7 = ZonedDateTime.of(2026, 11, 7, 15, 0, 0, 0, CAIRO).toInstant();
    private static final Instant DEC_7 = ZonedDateTime.of(2026, 12, 7, 15, 0, 0, 0, CAIRO).toInstant();
    private static final String HANY = "hany@cairoeq.com";

    @Autowired
    TrainerAccountsFixture fx;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JsonMapper json;
    @Autowired
    JwtDecoder jwtDecoder;

    private String superToken;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ((MutableClock) clock).set(NOV_7);
        // Only the default club stays. Other clubs' rows are hidden from the default club, so this runs in root mode.
        asRoot(() -> {
            jdbc.update("delete from invite where club_id <> ?", Club.DEFAULT_ID);
            jdbc.update("delete from admin_account where club_id <> ?", Club.DEFAULT_ID);
            jdbc.update("delete from subscription");
            jdbc.update("delete from club where id <> ?", Club.DEFAULT_ID);
            jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null");
            jdbc.update("delete from plan");
            jdbc.update("delete from super_admin");
            return null;
        });
        upsertPlan("TRIAL", "Trial", "NULL", "NULL", "30", "BLOCK");
        upsertPlan("BASIC", "Basic", "2", "50", "NULL", "READ_ONLY");
        upsertPlan("PRO", "Pro", "10", "300", "NULL", "READ_ONLY");
        upsertPlan("MAX", "Max", "NULL", "NULL", "NULL", "READ_ONLY");
        jdbc.update("insert into super_admin (email, password_hash) values (?, ?)",
                SUPER_EMAIL, new BCryptPasswordEncoder().encode(SUPER_PASSWORD));
        superToken = tokens(fx.perform(post("/platform/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk())).accessToken();
    }

    // ---- create

    // 1
    @Test
    void creatingAClubStartsATrialInvitesItsAdminAndEmailsTheLink() throws Exception {
        String body = createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith(CLUBS + "/" + clubId("cairo-eq"))))
                .andExpect(jsonPath("$.code").value("cairo-eq"))
                .andExpect(jsonPath("$.name").value("Cairo Equestrian"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessState").value("ACTIVE"))
                .andExpect(jsonPath("$.accessReason").value(nullValue()))
                .andExpect(jsonPath("$.planCode").value("TRIAL"))
                .andExpect(jsonPath("$.billingPeriod").value("TRIAL"))
                .andExpect(jsonPath("$.endsAt").value(DEC_7.toString()))
                .andExpect(jsonPath("$.daysLeft").value(30))
                .andExpect(jsonPath("$.trainers").value(0))
                .andExpect(jsonPath("$.maxTrainers").value(nullValue()))
                .andExpect(jsonPath("$.riders").value(0))
                .andExpect(jsonPath("$.maxRiders").value(nullValue()))
                .andExpect(jsonPath("$.admins", hasSize(1)))
                .andExpect(jsonPath("$.admins[0].name").value("Hany"))
                .andExpect(jsonPath("$.admins[0].email").value(HANY))
                .andExpect(jsonPath("$.admins[0].status").value("INVITED"))
                .andReturn().getResponse().getContentAsString();
        UUID club = UUID.fromString(JsonPath.read(body, "$.id"));

        Map<String, Object> admin = asRoot(() -> jdbc.queryForMap(
                "select id, club_id, name, status, password_hash from admin_account where email = ?", HANY));
        assertThat(admin.get("club_id")).isEqualTo(club);
        assertThat(admin.get("status")).isEqualTo("INVITED");
        assertThat(admin.get("password_hash")).isNull();

        Map<String, Object> trial = asRoot(() -> jdbc.queryForMap(
                "select s.billing_period, s.starts_at, s.ends_at, s.created_by, p.code from subscription s "
                        + "join plan p on p.id = s.plan_id where s.club_id = ?", club));
        assertThat(trial.get("code")).isEqualTo("TRIAL");
        assertThat(trial.get("billing_period")).isEqualTo("TRIAL");
        assertThat(((Timestamp) trial.get("starts_at")).toInstant()).isEqualTo(NOV_7);
        assertThat(((Timestamp) trial.get("ends_at")).toInstant()).isEqualTo(DEC_7);
        assertThat(trial.get("created_by")).isEqualTo(superAdminId());

        List<Map<String, Object>> links = asRoot(() -> jdbc.queryForList(
                "select club_id, purpose, expires_at, accepted_at from invite where admin_account_id = ?",
                admin.get("id")));
        assertThat(links).singleElement().satisfies(link -> {
            assertThat(link.get("club_id")).isEqualTo(club);
            assertThat(link.get("purpose")).isEqualTo("invite");
            assertThat(((Timestamp) link.get("expires_at")).toInstant()).isEqualTo(NOV_7.plus(Duration.ofHours(72)));
            assertThat(link.get("accepted_at")).isNull();
        });

        assertThat(fx.recorder().all()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo(HANY);
            assertThat(email.subject()).isEqualTo("Your club admin account at Riding Schedule");
            assertThat(email.text())
                    .contains("Hi Hany,")
                    .contains("You're the admin of Cairo Equestrian on Riding Schedule. Set a password to start.")
                    .contains("http://admin.test/invite/" + RecordingNotificationSender.tokenFrom(email))
                    .contains("Your club code: cairo-eq")
                    .contains("After that, sign in on the web console with this email and your club code.");
            assertThat(email.html()).contains("Your club code: cairo-eq");
        });
    }

    // 2
    @Test
    void theAdminSetsAPasswordFromTheLinkAndSignsInWithTheClubCode() throws Exception {
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());
        String token = fx.tokenFromLastEmail();

        fx.acceptInvite(token, "Club@2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubCode").value("cairo-eq"));

        AuthTokens tokens = tokens(clubLogin("cairo-eq", HANY, "Club@2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.club.code").value("cairo-eq")));
        assertThat(jwtDecoder.decode(tokens.accessToken()).getClaimAsString("cid"))
                .isEqualTo(clubId("cairo-eq").toString());
        assertThat(asRoot(() -> jdbc.queryForObject("select status from admin_account where email = ?",
                String.class, HANY))).isEqualTo("ACTIVE");
        // The link works once.
        fx.acceptInvite(token, "Other@2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_link"));
    }

    // 3
    @Test
    void anInvitedAdminCannotSignInBeforeSettingAPassword() throws Exception {
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());

        clubLogin("cairo-eq", HANY, "Club@2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
        // Not counted as a wrong try: the account has no password to guess.
        assertThat(asRoot(() -> jdbc.queryForObject("select failed_attempts from admin_account where email = ?",
                Integer.class, HANY))).isZero();
    }

    // 4
    @Test
    void aTakenCodeIsAConflictAndABadOrReservedCodeIsAValidationError() throws Exception {
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());

        createClub("Cairo Again", " Cairo-EQ ", "Mona", "mona@cairoeq.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("club_code_taken"))
                .andExpect(jsonPath("$.detail").value("This club code is already taken."));
        createClub("Riding School", "school", "Mona", "mona@cairoeq.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("club_code_taken"));
        for (String code : List.of("platform", "ab", "-cairo", "cairo eq")) {
            createClub("Bad", code, "Mona", "mona@cairoeq.com")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
        }
        assertThat(clubCount()).isEqualTo(2);
    }

    @Test
    void missingFieldsAreAValidationError() throws Exception {
        fx.perform(asSuperAdmin(post(CLUBS)).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Cairo\", \"code\": \"cairo-eq\", \"admin\": { \"name\": \"Hany\" } }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("admin.email"));
        createClub("Cairo", "cairo-eq", "Hany", "not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("admin.email"));
        assertThat(clubCount()).isEqualTo(1);
    }

    // 5
    @Test
    void withoutATrialPlanNothingIsCreated() throws Exception {
        jdbc.update("delete from plan where trial_days is not null");

        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("no_plans_configured"))
                .andExpect(jsonPath("$.detail").value("No plans configured. Run ops/upsert-plan.sql first."));

        assertThat(clubCount()).isEqualTo(1);
        assertThat(asRoot(() -> jdbc.queryForObject("select count(*) from admin_account where email = ?",
                Integer.class, HANY))).isZero();
        assertThat(fx.recorder().all()).isEmpty();
    }

    // 6
    @Test
    void aFailedEmailStillLeavesTheClubCreated(CapturedOutput output) throws Exception {
        fx.recorder().failWith(new IllegalStateException("smtp down"));

        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());

        assertThat(clubCount()).isEqualTo(2);
        assertThat(asRoot(() -> jdbc.queryForObject("select count(*) from invite where club_id = ?",
                Integer.class, clubId("cairo-eq")))).isOne();
        assertThat(output).contains("Account link email failed ADMIN=").contains("smtp down")
                .doesNotContain("/invite/");
    }

    // 7
    @Test
    void ifTheAdminCannotBeSavedTheClubAndTrialAreRemoved() throws Exception {
        // A trigger refuses this one admin, standing in for any failure after the club was committed.
        jdbc.execute("""
                reset role;
                create function refuse_admin() returns trigger language plpgsql as $$
                begin
                    if new.email = 'fail@cairoeq.com' then raise exception 'forced admin failure'; end if;
                    return new;
                end $$;
                create trigger refuse_admin before insert on admin_account
                    for each row execute function refuse_admin();""");
        try {
            createClub("Cairo Equestrian", "cairo-eq", "Hany", "fail@cairoeq.com")
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("internal_error"));
        } finally {
            jdbc.execute("reset role; drop trigger refuse_admin on admin_account; drop function refuse_admin();");
        }

        assertThat(clubCount()).isEqualTo(1);
        assertThat(asRoot(() -> jdbc.queryForObject("select count(*) from subscription", Integer.class))).isZero();
        assertThat(fx.recorder().all()).isEmpty();
        // The code is free again.
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());
    }

    // ---- list and detail

    // 10
    @Test
    void theListShowsEveryClubWithItsPlanStateAndUsage() throws Exception {
        // The school admin sets up below, which needs a subscription; it is removed again before the list.
        paidSubscription(Club.DEFAULT_ID, "BASIC", NOV_7.minus(Duration.ofDays(1)), NOV_7.plus(Duration.ofDays(20)));
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());
        UUID mona = fx.invite("Mona Fathy", "01000000001", "mona@example.com").id();
        fx.invite("Omar Adel", "01000000002", "omar@example.com");
        fx.invite("Sara Nabil", "01000000003", "sara@example.com");
        jdbc.update("update trainer set status = 'disabled' where id = ?", mona);
        for (int i = 0; i < 4; i++) {
            jdbc.update("insert into member (name, phone, email) values (?, ?, ?)",
                    "Person " + i, TestMembers.nextPhone(), TestMembers.nextEmail());
        }
        jdbc.update("update member set closed_at = now() where name = 'Person 0'");
        jdbc.update("delete from subscription where club_id = ?", Club.DEFAULT_ID);

        list("")
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                // By name: Cairo Equestrian, Riding School.
                .andExpect(jsonPath("$.items[0].code").value("cairo-eq"))
                .andExpect(jsonPath("$.items[0].accessState").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].planName").value("Trial"))
                .andExpect(jsonPath("$.items[0].daysLeft").value(30))
                .andExpect(jsonPath("$.items[0].trainers").value(0))
                .andExpect(jsonPath("$.items[0].riders").value(0))
                .andExpect(jsonPath("$.items[1].code").value("school"))
                .andExpect(jsonPath("$.items[1].accessState").value("BLOCKED"))
                .andExpect(jsonPath("$.items[1].accessReason").value("NO_SUBSCRIPTION"))
                .andExpect(jsonPath("$.items[1].planCode").value(nullValue()))
                .andExpect(jsonPath("$.items[1].endsAt").value(nullValue()))
                .andExpect(jsonPath("$.items[1].daysLeft").value(0))
                .andExpect(jsonPath("$.items[1].trainers").value(2))
                .andExpect(jsonPath("$.items[1].riders").value(3));
    }

    @Test
    void usageShowsThePlansLimits() throws Exception {
        paidSubscription(Club.DEFAULT_ID, "BASIC", NOV_7.minus(Duration.ofDays(10)), NOV_7.plus(Duration.ofDays(20)));

        list("?q=school")
                .andExpect(jsonPath("$.items[0].accessState").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].planCode").value("BASIC"))
                .andExpect(jsonPath("$.items[0].billingPeriod").value("MONTHLY"))
                .andExpect(jsonPath("$.items[0].maxTrainers").value(2))
                .andExpect(jsonPath("$.items[0].maxRiders").value(50));
    }

    // 11
    @Test
    void theListFiltersByTextStateAndExpiry() throws Exception {
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());

        list("?q=CAIRO").andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].code").value("cairo-eq"));
        list("?q=hool").andExpect(jsonPath("$.items[0].code").value("school"));
        list("?state=BLOCKED").andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].code").value("school"));
        list("?expiringWithinDays=7").andExpect(jsonPath("$.totalItems").value(0));

        // The school's Basic month ends in 5 days; the trial in 30.
        paidSubscription(Club.DEFAULT_ID, "BASIC", NOV_7.minus(Duration.ofDays(25)), NOV_7.plus(Duration.ofDays(5)));

        list("?expiringWithinDays=7").andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].code").value("school"))
                .andExpect(jsonPath("$.items[0].daysLeft").value(5));
        list("?expiringWithinDays=30").andExpect(jsonPath("$.totalItems").value(2));
        list("?state=BLOCKED").andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void theListSortsAndPages() throws Exception {
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());
        createClub("Alex Riders", "alex", "Mona", "mona@alex.com").andExpect(status().isCreated());

        list("?sort=code&dir=desc").andExpect(jsonPath("$.items[*].code").value(
                org.hamcrest.Matchers.contains("school", "cairo-eq", "alex")));
        // The school has no subscription, so it comes last by end date either way.
        list("?sort=endsAt&dir=desc").andExpect(jsonPath("$.items[2].code").value("school"));
        list("?size=2&page=1").andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].code").value("school"));
        fx.perform(asSuperAdmin(get(CLUBS + "?sort=plan&size=0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("sort", "size")));
    }

    // 12
    @Test
    void theDetailShowsTheClubAndItsAdmins() throws Exception {
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());

        fx.perform(asSuperAdmin(get(CLUBS + "/" + clubId("cairo-eq"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("cairo-eq"))
                .andExpect(jsonPath("$.createdAt").value(NOV_7.toString()))
                .andExpect(jsonPath("$.suspendedAt").value(nullValue()))
                .andExpect(jsonPath("$.admins", hasSize(1)))
                .andExpect(jsonPath("$.admins[0].name").value("Hany"))
                .andExpect(jsonPath("$.admins[0].status").value("INVITED"));
        // The default club's seeded admin is ACTIVE, named Admin by the migration.
        fx.perform(asSuperAdmin(get(CLUBS + "/" + Club.DEFAULT_ID)))
                .andExpect(jsonPath("$.admins[0].email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.admins[0].name").value("Admin"))
                .andExpect(jsonPath("$.admins[0].status").value("ACTIVE"));
    }

    // ---- suspend and reactivate

    // 13
    @Test
    void suspendingBlocksTheClubAndReactivatingClearsTheReason() throws Exception {
        createClub("Cairo Equestrian", "cairo-eq", "Hany", HANY).andExpect(status().isCreated());
        UUID club = clubId("cairo-eq");

        changeStatus(club, "{ \"status\": \"SUSPENDED\", \"reason\": \"  Unpaid invoice  \" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.accessState").value("BLOCKED"))
                .andExpect(jsonPath("$.accessReason").value("SUSPENDED"))
                .andExpect(jsonPath("$.suspendedAt").value(NOV_7.toString()))
                .andExpect(jsonPath("$.suspendedReason").value("Unpaid invoice"));
        changeStatus(club, "{ \"status\": \"SUSPENDED\", \"reason\": \"Again please\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_club_status"));
        list("?state=BLOCKED").andExpect(jsonPath("$.totalItems").value(2));

        changeStatus(club, "{ \"status\": \"ACTIVE\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessState").value("ACTIVE"))
                .andExpect(jsonPath("$.suspendedAt").value(nullValue()))
                .andExpect(jsonPath("$.suspendedReason").value(nullValue()));
        changeStatus(club, "{ \"status\": \"ACTIVE\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_club_status"));
    }

    // 14
    @Test
    void suspendingNeedsAReasonOfFiveCharacters() throws Exception {
        changeStatus(Club.DEFAULT_ID, "{ \"status\": \"SUSPENDED\", \"reason\": \"no\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
        changeStatus(Club.DEFAULT_ID, "{ \"status\": \"SUSPENDED\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
        changeStatus(Club.DEFAULT_ID, "{ \"reason\": \"Unpaid invoice\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        assertThat(jdbc.queryForObject("select status from club where id = ?", String.class, Club.DEFAULT_ID))
                .isEqualTo("ACTIVE");
    }

    // ---- access

    // 15
    @Test
    void onlyTheSuperAdminReachesClubsAndAnUnknownClubIsNotFound() throws Exception {
        // The school admin must be able to sign in (a club without a subscription is blocked).
        paidSubscription(Club.DEFAULT_ID, "BASIC", NOV_7.minus(Duration.ofDays(1)), NOV_7.plus(Duration.ofDays(20)));
        fx.admin(get(CLUBS))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        fx.admin(post(CLUBS).contentType(MediaType.APPLICATION_JSON).content(createBody("X", "xclub", "X", "x@x.com")))
                .andExpect(status().isForbidden());
        fx.perform(get(CLUBS)).andExpect(status().isUnauthorized());

        UUID unknown = UUID.randomUUID();
        fx.perform(asSuperAdmin(get(CLUBS + "/" + unknown)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("club_not_found"));
        changeStatus(unknown, "{ \"status\": \"ACTIVE\" }")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("club_not_found"));
    }

    // 16
    @Test
    void theExistingDefaultClubAdminStillSignsIn() throws Exception {
        // With a subscription: since E17 a club without one is blocked.
        paidSubscription(Club.DEFAULT_ID, "BASIC", NOV_7.minus(Duration.ofDays(1)), NOV_7.plus(Duration.ofDays(20)));
        fx.login(ADMIN_EMAIL, ADMIN_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ---- helpers

    private ResultActions createClub(String name, String code, String adminName, String adminEmail) throws Exception {
        return fx.perform(asSuperAdmin(post(CLUBS)).contentType(MediaType.APPLICATION_JSON)
                .content(createBody(name, code, adminName, adminEmail)));
    }

    private static String createBody(String name, String code, String adminName, String adminEmail) {
        return """
                { "name": "%s", "code": "%s", "admin": { "name": "%s", "email": "%s" } }"""
                .formatted(name, code, adminName, adminEmail);
    }

    private ResultActions list(String query) throws Exception {
        return fx.perform(asSuperAdmin(get(CLUBS + query))).andExpect(status().isOk());
    }

    private ResultActions changeStatus(UUID club, String body) throws Exception {
        return fx.perform(asSuperAdmin(put(CLUBS + "/" + club + "/status"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions clubLogin(String clubCode, String login, String password) throws Exception {
        return fx.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"clubCode\": \"%s\", \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(clubCode, login, password)));
    }

    private MockHttpServletRequestBuilder asSuperAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + superToken);
    }

    private void paidSubscription(UUID club, String plan, Instant start, Instant end) {
        jdbc.update("""
                        insert into subscription (club_id, plan_id, billing_period, starts_at, ends_at, created_by)
                        select ?, id, 'MONTHLY', ?, ?, ? from plan where code = ?""",
                club, Timestamp.from(start), Timestamp.from(end), superAdminId(), plan);
    }

    private UUID clubId(String code) {
        return jdbc.queryForObject("select id from club where code = ?", UUID.class, code);
    }

    private int clubCount() {
        return jdbc.queryForObject("select count(*) from club", Integer.class);
    }

    private UUID superAdminId() {
        return jdbc.queryForObject("select id from super_admin", UUID.class);
    }

    // Runs the real ops script with its psql variables filled in, as psql -v would.
    private void upsertPlan(String code, String name, String trainers, String riders, String trialDays,
                            String onExpiry) throws Exception {
        jdbc.execute(Files.readString(UPSERT_PLAN)
                .replace(":'code'", literal(code))
                .replace(":'name'", literal(name))
                .replace(":'on_expiry'", literal(onExpiry))
                .replace(":trainers", trainers)
                .replace(":riders", riders)
                .replace(":trial_days", trialDays));
    }

    private static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    // Other clubs' school rows are hidden from the test's default club; root mode sees every club.
    private static <T> T asRoot(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }

    private AuthTokens tokens(ResultActions result) throws Exception {
        return json.readValue(result.andReturn().getResponse().getContentAsString(), AuthTokens.class);
    }
}
