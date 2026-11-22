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
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.equestrian.equis.support.TrainerAccountsFixture.CAIRO;
import static com.equestrian.equis.support.TwoClubFixture.PASSWORD;
import static com.equestrian.equis.support.TwoClubFixture.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E22 · Club accounts list API, cases 1-12 of the ticket with the approved contract (AccountPage, no migration:
// admin sign-in is admin_account.last_login_at). Clock Sat 21 Nov 2026, 17:40 Cairo. TwoClubFixture's cairo-eq is
// reshaped once per class into the ticket's people: admin Hany (active, last sign-in 09:12), trainers Omar (active),
// Mona (invited) and Karim (disabled), riders Laila and Youssef (active, Youssef never signed in) and Sara (no account,
// no email), applicant Nadia (a member, never placed). school keeps the fixture's admin, two trainers and two riders.
class PlatformAccountApiIT extends IntegrationTestBase {
    private static final Instant NOV_21 = ZonedDateTime.of(2026, 11, 21, 17, 40, 0, 0, CAIRO).toInstant();
    private static final Instant HANY_LAST = ZonedDateTime.of(2026, 11, 21, 9, 12, 0, 0, CAIRO).toInstant();
    private static final String SUPER_EMAIL = "ops@equis.app";
    private static final String SUPER_PASSWORD = "Platform@2026";
    private static final String HANY_EMAIL = "hany@cairo-eq.com";
    private static final String OMAR_PHONE = "01052000001";
    private static final List<String> CAIRO_ORDER =
            List.of("Hany Saeed", "Karim Adel", "Mona Fathy", "Omar Khaled", "Laila Mostafa", "Sara", "Youssef Nabil");

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

    @BeforeEach
    void theTicketsAccounts() throws Exception {
        school = fx.a();
        cairoEq = fx.b();
        CLOCK.set(NOV_21);
        root(() -> {
            jdbc.update("update subscription set ends_at = TIMESTAMPTZ '2200-01-01 00:00:00+00'");
            // The fixture is built once per class: reshape it once too.
            if (jdbc.queryForObject("select count(*) from trainer where name = 'Karim Adel'", Integer.class) == 0) {
                reshapeCairoEq();
            }
            // A test may sign Hany in; every test starts from the ticket's 09:12.
            jdbc.update("update admin_account set last_login_at = ? where club_id = ?",
                    Timestamp.from(HANY_LAST), cairoEq.clubId());
            jdbc.update("""
                    insert into super_admin (email, password_hash) select ?, ?
                    where not exists (select 1 from super_admin where email = ?)""",
                    SUPER_EMAIL, passwordEncoder.encode(SUPER_PASSWORD), SUPER_EMAIL);
            return null;
        });
        ResultActions signedIn = mvc.perform(post("/platform/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                        .formatted(SUPER_EMAIL, SUPER_PASSWORD)))
                .andExpect(status().isOk());
        superToken = JsonPath.read(body(signedIn), "$.accessToken");
    }

    // 1 (and decision 1: no applicant, no other club)
    @Test
    void theListHoldsTheClubsAdminsTrainersAndRidersInOrder() throws Exception {
        accounts(cairoEq.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").value(contains(CAIRO_ORDER.toArray())))
                .andExpect(jsonPath("$.items[*].type").value(contains(
                        "ADMIN", "TRAINER", "TRAINER", "TRAINER", "RIDER", "RIDER", "RIDER")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalItems").value(7))
                // AccountPage, not Spring's Page.
                .andExpect(jsonPath("$", not(hasKey("content"))))
                .andExpect(jsonPath("$", not(hasKey("totalElements"))));

        String json = body(accounts(cairoEq.clubId(), ""));
        assertThat(json).doesNotContain("Nadia").doesNotContain(" A\"").doesNotContain("school.eg");
        for (String schoolId : fx.idsOf(school.clubId())) {
            assertThat(json).doesNotContain(schoolId);
        }
    }

    // 2
    @Test
    void eachTypesStatusIsMappedToTheOneList() throws Exception {
        accounts(cairoEq.clubId(), "")
                .andExpect(jsonPath("$.items[*].status").value(contains(
                        "ACTIVE", "DISABLED", "INVITED", "ACTIVE", "ACTIVE", "NO_ACCOUNT", "ACTIVE")));
    }

    // 3 (and the nullable email)
    @Test
    void adminsHaveNoPhoneAndLastSignInComesFromEachAccount() throws Exception {
        UUID hany = root(() -> jdbc.queryForObject("select id from admin_account where club_id = ?",
                UUID.class, cairoEq.clubId()));
        accounts(cairoEq.clubId(), "")
                .andExpect(jsonPath("$.items[0].id").value(hany.toString()))
                .andExpect(jsonPath("$.items[0].email").value(HANY_EMAIL))
                .andExpect(jsonPath("$.items[0].phone").value(nullValue()))
                .andExpect(jsonPath("$.items[0].lastSignInAt").value(HANY_LAST.toString()))
                .andExpect(jsonPath("$.items[3].phone").value(OMAR_PHONE))
                .andExpect(jsonPath("$.items[5].name").value("Sara"))
                .andExpect(jsonPath("$.items[5].email").value(nullValue()))
                .andExpect(jsonPath("$.items[5].lastSignInAt").value(nullValue()))
                .andExpect(jsonPath("$.items[6].name").value("Youssef Nabil"))
                .andExpect(jsonPath("$.items[6].lastSignInAt").value(nullValue()));
    }

    // 4
    @Test
    void searchMatchesNameEmailOrPhoneInAnyCase() throws Exception {
        names("?q=LAILA").andExpect(jsonPath("$.items[*].name").value(contains("Laila Mostafa")));
        names("?q=" + OMAR_PHONE).andExpect(jsonPath("$.items[*].name").value(contains("Omar Khaled")));
        names("?q=cairo-eq.com").andExpect(jsonPath("$.items[*].name").value(contains("Hany Saeed")))
                .andExpect(jsonPath("$.totalItems").value(1));
        // Trimmed.
        search("  laila ").andExpect(jsonPath("$.items[*].name").value(contains("Laila Mostafa")));
        // A LIKE wildcard is matched literally.
        search("%%").andExpect(jsonPath("$.totalItems").value(0));
        names("?q=nobody-here").andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    // 5
    @Test
    void aSearchShorterThanTwoCharactersIsIgnored() throws Exception {
        for (String q : List.of("a", " a ", "   ", "")) {
            search(q).andExpect(jsonPath("$.totalItems").value(7))
                    .andExpect(jsonPath("$.items", hasSize(7)));
        }
    }

    // 6
    @Test
    void typeAndStatusFilterOnOneValueEach() throws Exception {
        names("?type=TRAINER").andExpect(jsonPath("$.items[*].name").value(
                contains("Karim Adel", "Mona Fathy", "Omar Khaled")));
        names("?status=INVITED").andExpect(jsonPath("$.items[*].name").value(contains("Mona Fathy")));
        names("?type=RIDER&status=ACTIVE").andExpect(jsonPath("$.items[*].name").value(
                contains("Laila Mostafa", "Youssef Nabil")));
        names("?type=ADMIN").andExpect(jsonPath("$.items[*].name").value(contains("Hany Saeed")));
        names("?status=NO_ACCOUNT").andExpect(jsonPath("$.items[*].name").value(contains("Sara")));
        names("?status=DISABLED").andExpect(jsonPath("$.items[*].name").value(contains("Karim Adel")));
        names("?type=ADMIN&status=DISABLED").andExpect(jsonPath("$.totalItems").value(0));
        names("?q=mo&type=RIDER").andExpect(jsonPath("$.items[*].name").value(contains("Laila Mostafa")));
    }

    // 7
    @Test
    void theListPages() throws Exception {
        names("?size=2&page=1")
                .andExpect(jsonPath("$.items[*].name").value(contains("Mona Fathy", "Omar Khaled")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(7));
        names("?size=2&page=3")
                .andExpect(jsonPath("$.items[*].name").value(contains("Youssef Nabil")));
        names("?page=99")
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItems").value(7));
        names("?size=1").andExpect(jsonPath("$.items", hasSize(1)));
        names("?size=100").andExpect(jsonPath("$.items", hasSize(7)));
        names("?size=3&status=ACTIVE&page=1")
                .andExpect(jsonPath("$.items[*].name").value(contains("Youssef Nabil")))
                .andExpect(jsonPath("$.totalItems").value(4));
    }

    // 8
    @Test
    void anUnknownValueOrABadPageIsAValidationError() throws Exception {
        for (String[] bad : List.of(
                new String[]{"?type=OWNER", "type"},
                new String[]{"?status=FOO", "status"},
                new String[]{"?size=500", "size"},
                new String[]{"?size=101", "size"},
                new String[]{"?size=0", "size"},
                new String[]{"?page=-1", "page"},
                new String[]{"?page=x", "page"})) {
            accounts(cairoEq.clubId(), bad[0])
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[*].field").value(contains(bad[1])));
        }
        accounts(cairoEq.clubId(), "?page=-1&size=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(containsInAnyOrder("page", "size")));
    }

    // 9
    @Test
    void anUnknownClubIsNotFound() throws Exception {
        accounts(UUID.randomUUID(), "")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("club_not_found"));
        // A validation error on an unknown club is still the validation error.
        accounts(UUID.randomUUID(), "?size=0").andExpect(status().isBadRequest());
        fx.perform(superToken, get("/platform/clubs/not-a-uuid/accounts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    // 10 (and 401 without a token)
    @Test
    void onlyTheSuperAdminMayList() throws Exception {
        String path = "/platform/clubs/" + cairoEq.clubId() + "/accounts";
        for (String token : List.of(clubLogin(HANY_EMAIL), clubLogin(OMAR_PHONE), clubLogin("person1@cairo-eq.eg"),
                clubLogin(school, "admin@school.eg"))) {
            fx.perform(token, get(path))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        mvcWithoutToken(path)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    // 11 (reduced: no migration, the admin's sign-in already sets last_login_at)
    @Test
    void anAdminSignInShowsAsTheLastSignIn() throws Exception {
        clubLogin(HANY_EMAIL);

        Timestamp saved = root(() -> jdbc.queryForObject(
                "select last_login_at from admin_account where club_id = ?", Timestamp.class, cairoEq.clubId()));
        assertThat(saved.toInstant()).isEqualTo(NOV_21);
        names("?type=ADMIN").andExpect(jsonPath("$.items[0].lastSignInAt").value(NOV_21.toString()));
    }

    // Decision 7: root mode, every part of the query filters its own club.
    @Test
    void eachClubSeesOnlyItsOwnAccounts() throws Exception {
        accounts(school.clubId(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.items[*].name").value(contains(
                        "Admin", "Trainer One A", "Trainer Two A", "Rider One A", "Rider Two A")));
        // Another club's people never match a search here.
        accounts(cairoEq.clubId(), "?q=admin@school.eg").andExpect(jsonPath("$.totalItems").value(0));
        search("Trainer One").andExpect(jsonPath("$.totalItems").value(0));
        accounts(school.clubId(), "?q=laila").andExpect(jsonPath("$.totalItems").value(0));
        accounts(school.clubId(), "?type=TRAINER&status=DISABLED").andExpect(jsonPath("$.totalItems").value(0));
        // A club with no accounts at all.
        UUID empty = root(() -> jdbc.queryForObject(
                "insert into club (code, name) values ('empty-club', 'Empty Club') "
                        + "on conflict (code) do update set name = excluded.name returning id", UUID.class));
        accounts(empty, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    // ---- set-up

    private void reshapeCairoEq() {
        UUID club = cairoEq.clubId();
        jdbc.update("update admin_account set name = 'Hany Saeed', email = ? where club_id = ?", HANY_EMAIL, club);
        jdbc.update("update trainer set name = 'Omar Khaled', status = 'active' where id = ?", cairoEq.trainer1());
        jdbc.update("update trainer set name = 'Mona Fathy', status = 'invited', last_active_at = null where id = ?",
                cairoEq.trainer2());
        jdbc.update("""
                insert into trainer (club_id, name, phone, email, password_hash, status, last_active_at)
                values (?, 'Karim Adel', '01052000003', 'karim@cairo-eq.eg', ?, 'disabled', ?)""",
                club, passwordEncoder.encode(PASSWORD), Timestamp.from(NOV_21.minusSeconds(86_400 * 10)));
        jdbc.update("update rider set name = 'Laila Mostafa' where id = ?", cairoEq.rider1());
        jdbc.update("update rider set name = 'Sara', email = null, account_status = 'none', last_active_at = null "
                + "where id = ?", cairoEq.rider2());
        jdbc.update("""
                insert into rider (club_id, name, phone, level_id, email, password_hash, account_status, last_active_at)
                values (?, 'Youssef Nabil', '01152000009', 1, 'youssef@cairo-eq.eg', ?, 'active', null)""",
                club, passwordEncoder.encode(PASSWORD));
        jdbc.update("update member set name = 'Nadia' where id = ?", cairoEq.pendingMember());
    }

    // ---- helpers

    private ResultActions accounts(UUID club, String query) throws Exception {
        return fx.perform(superToken, get("/platform/clubs/" + club + "/accounts" + query));
    }

    private ResultActions names(String query) throws Exception {
        return accounts(cairoEq.clubId(), query).andExpect(status().isOk());
    }

    private ResultActions search(String q) throws Exception {
        return fx.perform(superToken, get("/platform/clubs/" + cairoEq.clubId() + "/accounts").param("q", q))
                .andExpect(status().isOk());
    }

    private String clubLogin(String login) throws Exception {
        return clubLogin(cairoEq, login);
    }

    private String clubLogin(ClubData club, String login) throws Exception {
        ResultActions signedIn = fx.loginRequest(club.code(), login, PASSWORD).andExpect(status().isOk());
        return JsonPath.read(body(signedIn), "$.accessToken");
    }

    private ResultActions mvcWithoutToken(String path) throws Exception {
        return mvc.perform(get(path));
    }

    private static <T> T root(Supplier<T> work) {
        return TenantContext.runAsRoot(work);
    }
}
