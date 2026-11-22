package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TwoClubFixture;
import com.equestrian.equis.support.TwoClubFixture.ClubData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TwoClubFixture.PASSWORD;
import static com.equestrian.equis.support.TwoClubFixture.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E22 · Account activity API, cases 1-7 of the ticket with the approved contract, plus paging edges, the newest-first
// tie on id, isolation both ways and read-only. Contract: doneByName is the super admin's email (super_admin has no
// name), the page carries totalItems, page < 0 is a 400. Actions of cases 1-3 come from the real reset endpoint at the
// ticket's times (Sat 21 Nov 2026, Cairo); bulk and tie rows are inserted with SQL. Every test starts with no action
// rows, admin Hany Saeed and rider Laila Mostafa on cairo-eq.
class PlatformAccountActivityIT extends IntegrationTestBase {
    private static final Instant AT_1105 = ZonedDateTime.of(2026, 11, 21, 11, 5, 0, 0, CAIRO).toInstant();
    private static final Instant AT_1740 = ZonedDateTime.of(2026, 11, 21, 17, 40, 0, 0, CAIRO).toInstant();
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    private static final String HANY_EMAIL = "hany@cairo-eq.com";
    private static final String OMAR_PHONE = "01052000001";
    private static final String LAILA_EMAIL = "person1@cairo-eq.eg";

    @Autowired
    TwoClubFixture fx;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    MockMvc mvc;

    ClubData school;
    ClubData cairoEq;
    String superToken;
    UUID superId;
    UUID hany;

    @BeforeEach
    void cairoEqWithNoActions() throws Exception {
        school = fx.a();
        cairoEq = fx.b();
        CLOCK.set(AT_1105);
        root(() -> {
            jdbc.update("update subscription set ends_at = TIMESTAMPTZ '2200-01-01 00:00:00+00'");
            jdbc.update("update club set status = 'ACTIVE', suspended_at = null, suspended_reason = null");
            jdbc.update("delete from invite where purpose = 'reset'");
            jdbc.update("delete from platform_account_action");
            jdbc.update("delete from refresh_token where club_id = ?", cairoEq.clubId());
            // Case 3 renames Hany; put him back.
            jdbc.update("update admin_account set name = 'Hany Saeed', email = ?, password_hash = ?, "
                    + "failed_attempts = 0, locked_until = null where club_id = ?",
                    HANY_EMAIL, passwordEncoder.encode(PASSWORD), cairoEq.clubId());
            jdbc.update("update rider set name = 'Laila Mostafa' where id = ?", cairoEq.rider1());
            jdbc.update("""
                    insert into super_admin (email, password_hash) select ?, ?
                    where not exists (select 1 from super_admin where email = ?)""",
                    SUPER_EMAIL, passwordEncoder.encode(SUPER_PASSWORD), SUPER_EMAIL);
            return null;
        });
        hany = root(() -> jdbc.queryForObject("select id from admin_account where club_id = ?", UUID.class,
                cairoEq.clubId()));
        superId = root(() -> jdbc.queryForObject("select id from super_admin where email = ?", UUID.class,
                SUPER_EMAIL));
        superToken = superLogin();
    }

    // 1
    @Test
    void theTwoResetsComeBackNewestFirstWithEveryField() throws Exception {
        resetLailaAndHany();

        activity(cairoEq.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").isString())
                .andExpect(jsonPath("$.items[0].doneAt").value("2026-11-21T15:40:00Z"))
                .andExpect(jsonPath("$.items[0].accountType").value("ADMIN"))
                .andExpect(jsonPath("$.items[0].accountId").value(hany.toString()))
                .andExpect(jsonPath("$.items[0].accountName").value("Hany Saeed"))
                .andExpect(jsonPath("$.items[0].accountEmail").value(HANY_EMAIL))
                .andExpect(jsonPath("$.items[0].action").value("RESET_PASSWORD"))
                .andExpect(jsonPath("$.items[0].doneByName").value(SUPER_EMAIL))
                .andExpect(jsonPath("$.items[1].id").isString())
                .andExpect(jsonPath("$.items[1].doneAt").value("2026-11-21T09:05:00Z"))
                .andExpect(jsonPath("$.items[1].accountType").value("RIDER"))
                .andExpect(jsonPath("$.items[1].accountId").value(cairoEq.rider1().toString()))
                .andExpect(jsonPath("$.items[1].accountName").value("Laila Mostafa"))
                .andExpect(jsonPath("$.items[1].accountEmail").value(LAILA_EMAIL))
                .andExpect(jsonPath("$.items[1].action").value("RESET_PASSWORD"))
                .andExpect(jsonPath("$.items[1].doneByName").value(SUPER_EMAIL));
        // The ids are the saved rows' ids.
        List<String> ids = JsonPath.read(body(activity(cairoEq.clubId(), "")), "$.items[*].id");
        List<String> saved = root(() -> jdbc.queryForList(
                "select id::text from platform_account_action where club_id = ? order by done_at desc",
                String.class, cairoEq.clubId()));
        assertThat(ids).isEqualTo(saved);
    }

    // 2 (and the other way round)
    @Test
    void anotherClubsActionsNeverShowUp() throws Exception {
        resetLailaAndHany();
        fx.perform(superToken, post(resetPath(school.clubId(), "trainer", school.trainer1())))
                .andExpect(status().isOk());

        activity(cairoEq.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].accountId")
                        .value(contains(hany.toString(), cairoEq.rider1().toString())));
        activity(school.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].accountType").value("TRAINER"))
                .andExpect(jsonPath("$.items[0].accountId").value(school.trainer1().toString()));
    }

    // 3
    @Test
    void aRenamedAccountKeepsTheNameAndEmailOfTheMoment() throws Exception {
        resetLailaAndHany();
        root(() -> jdbc.update("update admin_account set name = 'Hany Mahmoud', email = 'new-hany@cairo-eq.com' "
                + "where id = ?", hany));
        root(() -> jdbc.update("update rider set name = 'Laila M.' where id = ?", cairoEq.rider1()));

        activity(cairoEq.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].accountName").value("Hany Saeed"))
                .andExpect(jsonPath("$.items[0].accountEmail").value(HANY_EMAIL))
                .andExpect(jsonPath("$.items[1].accountName").value("Laila Mostafa"));
    }

    // 4 (and page 0 holds the other 25, no row on both pages)
    @Test
    void thirtyActionsPageOneHoldsTheLastFive() throws Exception {
        List<UUID> newestFirst = insertActions(30);

        String page0 = body(activity(cairoEq.clubId(), "?page=0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(25)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalItems").value(30)));
        String page1 = body(activity(cairoEq.clubId(), "?page=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalItems").value(30)));

        List<String> all = new ArrayList<>(JsonPath.<List<String>>read(page0, "$.items[*].id"));
        all.addAll(JsonPath.<List<String>>read(page1, "$.items[*].id"));
        assertThat(new HashSet<>(all)).hasSize(30);
        assertThat(all).isEqualTo(newestFirst.stream().map(UUID::toString).toList());
        // Past the end: an empty page, same total.
        activity(cairoEq.clubId(), "?page=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItems").value(30));
        // Size 1 and the max of 100.
        activity(cairoEq.clubId(), "?size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.items[0].id").value(newestFirst.get(0).toString()));
        activity(cairoEq.clubId(), "?size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(30)))
                .andExpect(jsonPath("$.size").value(100));
    }

    // Newest first, then id desc on the same moment.
    @Test
    void theSameMomentIsOrderedByIdDescending() throws Exception {
        UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID high = UUID.fromString("ffffffff-0000-0000-0000-000000000002");
        UUID older = UUID.fromString("80000000-0000-0000-0000-000000000003");
        insertAction(low, AT_1740);
        insertAction(older, AT_1105);
        insertAction(high, AT_1740);

        activity(cairoEq.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id")
                        .value(contains(high.toString(), low.toString(), older.toString())));
    }

    // 5
    @Test
    void aClubWithNoActionsGetsAnEmptyPage() throws Exception {
        activity(cairoEq.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalItems").value(0));
        UUID emptyClub = root(() -> jdbc.queryForObject(
                "insert into club (code, name) values ('empty-club', 'Empty Club') "
                        + "on conflict (code) do update set name = excluded.name returning id", UUID.class));
        activity(emptyClub, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    // 6
    @Test
    void anUnknownClubIs404AndBadPagingIs400() throws Exception {
        activity(UUID.randomUUID(), "")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("club_not_found"));

        for (String size : List.of("500", "0", "101", "-1")) {
            activity(cairoEq.clubId(), "?size=" + size)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[*].field").value(contains("size")));
        }
        activity(cairoEq.clubId(), "?page=-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(contains("page")));
        activity(cairoEq.clubId(), "?page=-1&size=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("page", "size")));
        activity(cairoEq.clubId(), "?page=abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        fx.perform(superToken, get("/platform/clubs/not-a-uuid/account-activity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    // 7 (and 401 without a token)
    @Test
    void onlyTheSuperAdminMayReadIt() throws Exception {
        resetLailaAndHany();
        String path = activityPath(cairoEq.clubId());
        for (String token : List.of(clubLogin(cairoEq, HANY_EMAIL), clubLogin(cairoEq, OMAR_PHONE),
                clubLogin(cairoEq, LAILA_EMAIL), clubLogin(school, "admin@school.eg"))) {
            fx.perform(token, get(path))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    // Read-only: no write on the activity, rows unchanged.
    @Test
    void theActivityCannotBeWritten() throws Exception {
        resetLailaAndHany();
        String path = activityPath(cairoEq.clubId());

        fx.perform(superToken, post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
        fx.perform(superToken, put(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
        fx.perform(superToken, delete(path)).andExpect(status().is4xxClientError());
        fx.perform(superToken, delete(path + "/" + UUID.randomUUID())).andExpect(status().is4xxClientError());

        assertThat(root(() -> jdbc.queryForObject("select count(*) from platform_account_action", Integer.class)))
                .isEqualTo(2);
    }

    // ---- helpers

    // Laila at 11:05 Cairo, Hany at 17:40, through the real reset.
    private void resetLailaAndHany() throws Exception {
        CLOCK.set(AT_1105);
        fx.perform(superToken, post(resetPath(cairoEq.clubId(), "rider", cairoEq.rider1())))
                .andExpect(status().isOk());
        // The access token signed in at 11:05 has expired by 17:40: sign in again.
        CLOCK.set(AT_1740);
        superToken = superLogin();
        fx.perform(superToken, post(resetPath(cairoEq.clubId(), "admin", hany)))
                .andExpect(status().isOk());
    }

    // n rows on cairo-eq, a minute apart; returns their ids newest first.
    private List<UUID> insertActions(int n) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            UUID id = UUID.randomUUID();
            insertAction(id, AT_1105.plusSeconds(60L * i));
            ids.add(0, id);
        }
        return ids;
    }

    private void insertAction(UUID id, Instant doneAt) {
        root(() -> jdbc.update("""
                insert into platform_account_action
                    (id, club_id, account_type, account_id, account_name, account_email, action, done_by, done_at)
                values (?, ?, 'RIDER', ?, 'Laila Mostafa', ?, 'RESET_PASSWORD', ?, ?)""",
                id, cairoEq.clubId(), cairoEq.rider1(), LAILA_EMAIL, superId, Timestamp.from(doneAt)));
    }

    private String superLogin() throws Exception {
        ResultActions signedIn = mvc.perform(post("/platform/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk());
        return JsonPath.read(body(signedIn), "$.accessToken");
    }

    private ResultActions activity(UUID club, String query) throws Exception {
        return fx.perform(superToken, get(activityPath(club) + query));
    }

    private static String activityPath(UUID club) {
        return "/platform/clubs/" + club + "/account-activity";
    }

    private static String resetPath(UUID club, String type, UUID id) {
        return "/platform/clubs/" + club + "/accounts/" + type + "/" + id + "/reset-password";
    }

    private String clubLogin(ClubData club, String login) throws Exception {
        return JsonPath.read(body(fx.loginRequest(club.code(), login, PASSWORD).andExpect(status().isOk())),
                "$.accessToken");
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
