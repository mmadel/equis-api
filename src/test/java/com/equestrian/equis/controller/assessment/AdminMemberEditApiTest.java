package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PATCH /admin/members/{id} (W2's Edit) over HTTP against a real database. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=edit-person-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminMemberEditApiTest {
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Friday 16 Oct 2026, 12:00 school time.
    private static final Instant FRIDAY_NOON = ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, CAIRO).toInstant();
    private static final LocalDate SAT_24 = LocalDate.of(2026, 10, 24);
    private static final LocalDate TUE_13 = LocalDate.of(2026, 10, 13);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    static final MutableClock CLOCK = new MutableClock(FRIDAY_NOON);

    @TestBean
    Clock clock;

    static Clock clock() {
        return CLOCK;
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;

    private UUID ahmed;
    private UUID saturday;

    @BeforeEach
    void setUp() {
        CLOCK.set(FRIDAY_NOON);
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        ahmed = jdbc.queryForObject("""
                insert into trainer (name, phone, email, status, password_hash)
                values ('Ahmed Said', '01999990001', 'ahmed@school.eg', 'active', 'not-a-real-hash') returning id""",
                UUID.class);
        saturday = slot(SAT_24);
    }

    // Case 1: the misspelt name and the mistyped phone are fixed; the email stays.
    @Test
    void fixesTheNameAndPhoneOfAnUpcomingPerson() throws Exception {
        UUID sara = booked("Sarah Ali", "01011111222", "sara@mail.com", saturday, "upcoming");

        edit(sara, """
                { "name": " Sara Ali ", "phone": "01011112222" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.memberStatus").value("pending_assessment"))
                .andExpect(jsonPath("$.member.id").value(sara.toString()))
                .andExpect(jsonPath("$.member.name").value("Sara Ali"))
                .andExpect(jsonPath("$.member.phone").value("01011112222"))
                .andExpect(jsonPath("$.member.email").value("sara@mail.com"))
                .andExpect(jsonPath("$.actions", hasItem("edit")));

        assertThat(details(sara)).containsExactly("Sara Ali", "01011112222", "sara@mail.com");
    }

    // Case 2
    @Test
    void anEmptyEditIsRefused() throws Exception {
        UUID sara = booked("Sara Ali", "01011112222", "sara@mail.com", saturday, "upcoming");

        edit(sara, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("nothing_to_change"));
    }

    @Test
    void badValuesAreRefusedAndNothingChanges() throws Exception {
        UUID sara = booked("Sara Ali", "01011112222", "sara@mail.com", saturday, "upcoming");

        edit(sara, """
                { "name": "  ", "phone": "123", "email": "x" }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
        assertThat(details(sara)).containsExactly("Sara Ali", "01011112222", "sara@mail.com");
    }

    // Case 3
    @Test
    void aPhoneOrEmailInUseIsTaken() throws Exception {
        UUID sara = booked("Sara Ali", "01011112222", "sara@mail.com", saturday, "upcoming");
        UUID omar = booked("Omar Hassan", "01033334444", "omar@mail.com", saturday, "upcoming");

        edit(sara, """
                { "phone": "01033334444" }""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("phone_taken"))
                .andExpect(jsonPath("$.detail").value("This phone belongs to Omar Hassan."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"))
                .andExpect(jsonPath("$.existingMemberId").value(omar.toString()))
                .andExpect(jsonPath("$.existingMemberName").value("Omar Hassan"))
                .andExpect(jsonPath("$.existingClosed").value(false));

        for (String email : List.of("Ahmed@School.eg", "edit-person-admin@example.com")) {
            edit(sara, """
                    { "email": "%s" }""".formatted(email))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("email_in_use"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                    .andExpect(jsonPath("$.existingMemberId").doesNotExist());
        }
        // The name sent alongside is not saved either.
        edit(sara, """
                { "name": "Sara New", "email": "omar@mail.com" }""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"))
                .andExpect(jsonPath("$.existingMemberName").value("Omar Hassan"));
        assertThat(details(sara)).containsExactly("Sara Ali", "01011112222", "sara@mail.com");
    }

    // Case 4: their own phone is not a clash.
    @Test
    void theirOwnPhoneWithANewEmailIsFine() throws Exception {
        UUID sara = booked("Sara Ali", "01011112222", "sara@mail.com", saturday, "upcoming");

        edit(sara, """
                { "phone": "01011112222", "email": "sara.ali@mail.com" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.phone").value("01011112222"))
                .andExpect(jsonPath("$.member.email").value("sara.ali@mail.com"));
    }

    // Case 5
    @Test
    void aRiderOrAnUnknownPersonIsRefused() throws Exception {
        UUID pakinam = booked("Pakinam Placed", "01055556666", "pakinam@mail.com", slot(TUE_13), "placed");
        jdbc.update("insert into rider (name, phone, level_id, trainer_id, member_id) values (?, ?, 2, ?, ?)",
                "Pakinam Placed", "01055556666", ahmed, pakinam);

        edit(pakinam, """
                { "name": "Pakinam P." }""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member_is_rider"))
                .andExpect(jsonPath("$.detail").value("Pakinam Placed is a rider. Edit rider details instead."));
        edit(UUID.randomUUID(), """
                { "name": "Nobody" }""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("member_not_found"));
        assertThat(details(pakinam)).first().isEqualTo("Pakinam Placed");
    }

    // Case 6
    @Test
    void aClosedPersonCanBeEditedAndStaysClosed() throws Exception {
        UUID nader = booked("Nader Noshow", "01077778888", "nader@mail.com", slot(TUE_13), "noshow");
        jdbc.update("update member set closed_at = ? where id = ?", Timestamp.from(FRIDAY_NOON), nader);

        edit(nader, """
                { "phone": "01077779999" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("noshow"))
                .andExpect(jsonPath("$.memberStatus").value("closed"))
                .andExpect(jsonPath("$.member.phone").value("01077779999"));
        assertThat(jdbc.queryForObject("select closed_at is not null from member where id = ?", Boolean.class,
                nader)).isTrue();
    }

    // Case 7
    @Test
    void theEmailIsStoredLowercase() throws Exception {
        UUID sara = booked("Sara Ali", "01011112222", "old@mail.com", saturday, "upcoming");

        edit(sara, """
                { "email": " Sara@MAIL.com " }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.email").value("sara@mail.com"));
        assertThat(details(sara).get(2)).isEqualTo("sara@mail.com");
    }

    // The row is the person's latest booking: a no-show who was rebooked answers with the new booking.
    @Test
    void answersWithTheLatestBooking() throws Exception {
        UUID omar = booked("Omar Hassan", "01033334444", "omar@mail.com", slot(TUE_13), "noshow");
        UUID rebooked = booking(omar, saturday, "upcoming");

        edit(omar, """
                { "name": "Omar H." }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rebooked.toString()))
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.member.name").value("Omar H."));
    }

    // Case 8
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        UUID sara = booked("Sara Ali", "01011112222", "sara@mail.com", saturday, "upcoming");
        String body = """
                { "name": "Someone Else" }""";

        mvc.perform(patch("/admin/members/{id}", sara).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/admin/members/{id}", sara)
                        .header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(ahmed, Role.TRAINER)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        assertThat(details(sara)).first().isEqualTo("Sara Ali");
    }

    // ---- helpers

    private ResultActions edit(UUID id, String body) throws Exception {
        return mvc.perform(asAdmin(patch("/admin/members/{id}", id))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    // Name, phone and email as stored.
    private List<String> details(UUID member) {
        return jdbc.queryForObject("select name, phone, email from member where id = ?",
                (rs, i) -> List.of(rs.getString(1), rs.getString(2), rs.getString(3)), member);
    }

    // A person with one booking in that slot.
    private UUID booked(String name, String phone, String email, UUID slot, String status) {
        UUID member = jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, phone, email);
        booking(member, slot, status);
        return member;
    }

    // upcoming, placed (Level 2) or noshow; a result is recorded three days ago.
    private UUID booking(UUID member, UUID slot, String status) {
        Boolean attended = switch (status) {
            case "placed" -> true;
            case "noshow" -> false;
            default -> null;
        };
        return jdbc.queryForObject("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at)
                        values (?, ?, ?, ?, ?, ?, ?) returning id""",
                UUID.class, member, slot, ahmed, status, attended,
                Boolean.TRUE.equals(attended) ? (Object) 2 : null,
                attended != null ? Timestamp.from(FRIDAY_NOON.minusSeconds(86_400L * 3)) : null);
    }

    // An assessment slot (no level) on that date, 10:00 to 11:00. Saved through JPA, as the app saves them.
    private UUID slot(LocalDate date) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(TEN);
            template.setEndTime(TEN.plusHours(1));
            template.setCapacity(4);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }
}
