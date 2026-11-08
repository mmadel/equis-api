package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.support.RecordingNotificationSender;
import com.equestrian.equis.support.TwoClubFixture;
import com.equestrian.equis.support.TwoClubFixture.ClubData;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static com.equestrian.equis.support.TwoClubFixture.PASSWORD;
import static com.equestrian.equis.support.TwoClubFixture.body;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E17 · per-club sign-in, cases 2–13 (case 1 is PhoneNormalizerTest). The two clubs of TwoClubFixture: "school"
// and "cairo-eq". Added here: coach Ahmed (school) and coach Mona (cairo-eq), who share the phone 01001234567.
// Laila is cairo-eq's rider with an account (person1@cairo-eq.eg, phone 01152000001).
class PerClubSignInIT extends IntegrationTestBase {
    private static final String SHARED_PHONE = "01001234567";
    private static final String LAILA_EMAIL = "person1@cairo-eq.eg";
    private static final String LAILA_PHONE = "01152000001";

    @Autowired
    TwoClubFixture fx;
    @Autowired
    MockMvc mvc;
    @Autowired
    RecordingNotificationSender recorder;
    @Autowired
    JdbcTemplate jdbc;

    static UUID ahmed;
    static UUID mona;
    static String monaClubCode;

    @BeforeEach
    void ahmedAndMonaShareAPhone() throws Exception {
        if (ahmed != null) {
            return;
        }
        ahmed = activeTrainer(fx.a(), "Ahmed Said", "010 0123 4567", "ahmed@school.eg");
        mona = activeTrainer(fx.b(), "Mona Kamal", "+20 100 123 4567", "mona@cairo-eq.eg");
    }

    // Case 2
    @Test
    void monaSignsInToHerClubWithHerPhoneInInternationalForm() throws Exception {
        ResultActions signedIn = fx.loginRequest("cairo-eq", "+20 100 123 4567", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TRAINER"))
                .andExpect(jsonPath("$.club.id").value(fx.b().clubId().toString()))
                .andExpect(jsonPath("$.club.code").value("cairo-eq"))
                .andExpect(jsonPath("$.club.name").value("Cairo Equestrian"));

        String access = JsonPath.read(body(signedIn), "$.accessToken");
        assertThat(claim(access, "cid")).isEqualTo(fx.b().clubId().toString());
        assertThat(claim(access, "sub")).isEqualTo(mona.toString());
    }

    // Case 3
    @Test
    void ahmedSignsInToHisClubWithTheSamePhone() throws Exception {
        ResultActions signedIn = fx.loginRequest("school", SHARED_PHONE, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.club.code").value("school"));

        String access = JsonPath.read(body(signedIn), "$.accessToken");
        assertThat(claim(access, "sub")).isEqualTo(ahmed.toString());
        assertThat(claim(access, "cid")).isEqualTo(fx.a().clubId().toString());
    }

    // Case 4
    @Test
    void theClubCodeIgnoresCaseAndSpaces() throws Exception {
        fx.loginRequest(" Cairo-EQ ", SHARED_PHONE, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.club.code").value("cairo-eq"));
    }

    // Case 5
    @Test
    void anUnknownCodeAndAnotherClubsCodeGiveTheWrongPasswordAnswer() throws Exception {
        String wrongPassword = problem(fx.loginRequest("cairo-eq", SHARED_PHONE, "not-her-password"));
        String unknownCode = problem(fx.loginRequest("cairoeq", SHARED_PHONE, PASSWORD));
        String monaInSchool = problem(fx.loginRequest("school", "mona@cairo-eq.eg", PASSWORD));

        assertThat(unknownCode).isEqualTo(wrongPassword).isEqualTo(monaInSchool)
                .isEqualTo("401 invalid_credentials Wrong email, phone or password.");
    }

    // Case 6
    @Test
    void theClubCodeIsRequired() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"login\": \"%s\", \"password\": \"%s\", \"rememberMe\": false }"
                                .formatted(SHARED_PHONE, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("clubCode"));
    }

    // Case 7
    @Test
    void lailaSignsInWithHerPhoneOrHerEmail() throws Exception {
        String byPhone = JsonPath.read(body(fx.loginRequest("cairo-eq", "+20 115 200 0001", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RIDER"))), "$.accessToken");
        String byEmail = JsonPath.read(body(fx.loginRequest("cairo-eq", LAILA_EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RIDER"))), "$.accessToken");

        assertThat(claim(byPhone, "sub")).isEqualTo(fx.b().rider1().toString()).isEqualTo(claim(byEmail, "sub"));
        // Her phone means nothing in the other club.
        fx.loginRequest("school", LAILA_PHONE, PASSWORD).andExpect(status().isUnauthorized());
    }

    // Case 8
    @Test
    void eachClubsAdminSignsInToTheirOwnClubAndFailedTriesCountPerAdmin() throws Exception {
        fx.loginRequest("school", "admin@school.eg", PASSWORD)
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.club.code").value("school"));
        fx.loginRequest("cairo-eq", "admin@cairo-eq.eg", PASSWORD)
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.club.code").value("cairo-eq"));

        fx.loginRequest("school", "admin@school.eg", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.attemptsLeft").value(4));
        // The school admin's email in the other club is no admin there, and touches no counter.
        fx.loginRequest("cairo-eq", "admin@school.eg", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.attemptsLeft").doesNotExist());

        assertThat(failedAttempts("admin@school.eg")).isEqualTo(1);
        assertThat(failedAttempts("admin@cairo-eq.eg")).isZero();
        // A good sign-in clears the counter again.
        fx.loginRequest("school", "admin@school.eg", PASSWORD).andExpect(status().isOk());
        assertThat(failedAttempts("admin@school.eg")).isZero();
    }

    // Case 9
    @Test
    void anotherClubsTrainerEmailCanBeInvited() throws Exception {
        fx.perform(fx.b().adminToken(), post("/admin/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        { "name": "Second Life", "phone": "01077770009", "email": "trainer1@school.eg" }"""))
                .andExpect(status().isCreated());
    }

    // Case 10
    @Test
    void emailsAreStillUniqueInsideOneClub() throws Exception {
        fx.perform(fx.b().adminToken(), post("/admin/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        { "name": "Twin", "phone": "01077770010", "email": "trainer1@cairo-eq.eg" }"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"));
        fx.perform(fx.b().adminToken(), patch("/admin/members/" + fx.b().pendingMember())
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"trainer1@cairo-eq.eg\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_in_use"));
    }

    // Case 11
    @Test
    void aTrainersPhoneIsStoredInTheLocalForm() throws Exception {
        fx.perform(fx.a().adminToken(), post("/admin/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        { "name": "Omar Nabil", "phone": "+20 100 999 8888", "email": "omar@school.eg" }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value("01009998888"));
    }

    // Case 12
    @Test
    void aPhoneThatIsNotAnEgyptianMobileIsRefused() throws Exception {
        fx.perform(fx.a().adminToken(), post("/admin/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        { "name": "Short Number", "phone": "12345", "email": "short@school.eg" }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));
    }

    // Case 13: Mona's link was accepted in the set-up.
    @Test
    void acceptingALinkAnswersTheLinksClubCode() {
        assertThat(monaClubCode).isEqualTo("cairo-eq");
        assertThat(TenantContext.runAsRoot(() -> jdbc.queryForObject(
                "SELECT club_id FROM trainer WHERE id = ?", UUID.class, mona))).isEqualTo(fx.b().clubId());
    }

    // ---- helpers

    // Invited by the club's admin; the password is set through the emailed link.
    private UUID activeTrainer(ClubData club, String name, String phone, String email) throws Exception {
        recorder.clear();
        ResultActions invited = fx.perform(club.adminToken(), post("/admin/trainers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"%s\", \"phone\": \"%s\", \"email\": \"%s\" }".formatted(name, phone, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value(SHARED_PHONE));
        String token = RecordingNotificationSender.tokenFrom(recorder.last());
        ResultActions accepted = mvc.perform(post("/auth/invite/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"%s\", \"password\": \"%s\" }".formatted(token, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubCode").value(club.code()));
        if (club.code().equals("cairo-eq")) {
            monaClubCode = JsonPath.read(body(accepted), "$.clubCode");
        }
        return UUID.fromString(JsonPath.read(body(invited), "$.id"));
    }

    private int failedAttempts(String email) {
        return TenantContext.runAsRoot(() -> jdbc.queryForObject(
                "SELECT failed_attempts FROM admin_account WHERE email = ?", Integer.class, email));
    }

    private static String problem(ResultActions result) throws Exception {
        String json = body(result);
        return result.andReturn().getResponse().getStatus() + " " + JsonPath.read(json, "$.code") + " "
                + JsonPath.read(json, "$.detail");
    }

    private static String claim(String jwt, String name) {
        String payload = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        return JsonPath.read(payload, "$." + name);
    }
}
