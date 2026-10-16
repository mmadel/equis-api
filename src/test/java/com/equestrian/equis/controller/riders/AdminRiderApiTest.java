package com.equestrian.equis.controller.riders;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /admin/riders (W3) over HTTP against a real database. Rows go in with SQL so each rider's trainer,
// member and placed booking can be set exactly. The container belongs to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=riders-api-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash",
        "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@Testcontainers
public class AdminRiderApiTest {
    private static final String PATH = "/admin/riders";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManagerFactory entityManagerFactory;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    RiderRepository riderRepository;

    private UUID ahmed;
    private UUID mona;
    private UUID karim;
    private UUID slot;
    private int seq;

    @BeforeEach
    void setUp() {
        // Also drops the seeded trainers and sample riders.
        jdbc.execute("truncate table trainer, refresh_token, member, session_template cascade");
        seq = 0;
        ahmed = trainer("Ahmed Said", "active");
        mona = trainer("Mona Adel", "active");
        karim = trainer("Karim Nabil", "disabled");
        slot = slot(LocalDate.of(2026, 10, 14));
    }

    // Case 1
    @Test
    void eachRiderSaysHowTheyWereAssignedAndTheTotalsCountThem() throws Exception {
        fiveRiders();

        list()
                .andExpect(status().isOk())
                .andExpect(jsonPath(row("Sara Ali", "howAssigned"), contains("from_assessment")))
                .andExpect(jsonPath(row("Omar Hassan", "howAssigned"), contains("changed_manually")))
                .andExpect(jsonPath(row("Nour Seed", "howAssigned"), contains("set_manually")))
                .andExpect(jsonPath(row("Hana Karim", "howAssigned"), contains("unassigned")))
                .andExpect(jsonPath(row("Youssef Alone", "howAssigned"), contains("unassigned")))
                .andExpect(jsonPath("$.totals.total").value(5))
                .andExpect(jsonPath("$.totals.fromAssessment").value(1))
                .andExpect(jsonPath("$.totals.changedManually").value(1))
                .andExpect(jsonPath("$.totals.setManually").value(1))
                .andExpect(jsonPath("$.totals.unassigned").value(2));
    }

    // Case 2
    @Test
    void theAssessorIsThePlacedBookingsAssessorAndNullForASeedRider() throws Exception {
        fiveRiders();

        list()
                .andExpect(jsonPath(row("Sara Ali", "assessor.id"), contains(ahmed.toString())))
                .andExpect(jsonPath(row("Sara Ali", "assessor.name"), contains("Ahmed Said")))
                .andExpect(jsonPath(row("Sara Ali", "assessor.status"), contains("active")))
                .andExpect(jsonPath(row("Omar Hassan", "assessor.id"), contains(ahmed.toString())))
                .andExpect(jsonPath(row("Omar Hassan", "trainer.name"), contains("Mona Adel")))
                .andExpect(jsonPath(row("Hana Karim", "assessor.status"), contains("disabled")))
                .andExpect(jsonPath(row("Hana Karim", "trainer.status"), contains("disabled")))
                .andExpect(jsonPath(row("Youssef Alone", "trainer"), contains((Object) null)))
                .andExpect(jsonPath(row("Nour Seed", "assessor"), contains((Object) null)))
                .andExpect(jsonPath(row("Nour Seed", "trainer.name"), contains("Ahmed Said")));
    }

    // Case 3
    @Test
    void resetToAssessorOnlyToAnActiveAssessorWhoIsNotTheTrainer() throws Exception {
        fiveRiders();
        // Assessed by Karim, now disabled, and moved to Mona.
        rider("Laila Moved", "01077778888", 2, mona, placedMember("Laila Moved", karim));

        list()
                .andExpect(jsonPath(row("Sara Ali", "canResetToAssessor"), contains(false)))
                .andExpect(jsonPath(row("Omar Hassan", "canResetToAssessor"), contains(true)))
                .andExpect(jsonPath(row("Nour Seed", "canResetToAssessor"), contains(false)))
                .andExpect(jsonPath(row("Hana Karim", "canResetToAssessor"), contains(false)))
                .andExpect(jsonPath(row("Youssef Alone", "canResetToAssessor"), contains(true)))
                .andExpect(jsonPath(row("Laila Moved", "howAssigned"), contains("changed_manually")))
                .andExpect(jsonPath(row("Laila Moved", "canResetToAssessor"), contains(false)));
    }

    // Case 4
    @Test
    void unassignedKeepsExactlyTheRidersNeedingReassignment() throws Exception {
        fiveRiders();

        Set<UUID> expected = riderRepository.findNeedingReassignment().stream()
                .map(Rider::getId)
                .collect(Collectors.toSet());
        String body = list("unassigned", "true").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> listed = JsonPath.read(body, "$.riders[*].id");
        Set<UUID> ids = listed.stream().map(UUID::fromString).collect(Collectors.toSet());

        assertThat(expected).hasSize(2);
        assertThat(ids).isEqualTo(expected);
        list("unassigned", "false").andExpect(jsonPath("$.riders.length()").value(5));
    }

    // Case 5
    @Test
    void filtersNarrowTheRowsButNotTheTotalsOrCards() throws Exception {
        fiveRiders();

        expectUnfilteredTotalsAndCards(list("level", "3"))
                .andExpect(jsonPath("$.riders[*].name", contains("Omar Hassan", "Sara Ali")));
        expectUnfilteredTotalsAndCards(list("trainer", karim.toString()))
                .andExpect(jsonPath("$.riders[*].name", contains("Hana Karim")));
        expectUnfilteredTotalsAndCards(list("q", "sara"))
                .andExpect(jsonPath("$.riders[*].name", contains("Sara Ali")));
        expectUnfilteredTotalsAndCards(list("q", "0101"))
                .andExpect(jsonPath("$.riders[*].name", contains("Sara Ali")));
    }

    // Case 6
    @Test
    void aBadLevelOrTrainerIsAValidationFailure() throws Exception {
        for (String level : new String[]{"0", "7"}) {
            list("level", level)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("level"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("invalid_level"));
        }
        list("trainer", "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("trainer"));
    }

    // Case 7
    @Test
    void anUnknownTrainerGivesNoRows() throws Exception {
        fiveRiders();

        list("trainer", UUID.randomUUID().toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders").isEmpty())
                .andExpect(jsonPath("$.totals.total").value(5));
    }

    // Case 8
    @Test
    void cardsShowActiveThenDisabledTrainersWithTheirRiderCounts() throws Exception {
        trainer("Ibrahim Invited", "invited");
        UUID adam = trainer("adam Early", "active");
        fiveRiders();

        list()
                .andExpect(jsonPath("$.trainers[*].name",
                        contains("adam Early", "Ahmed Said", "Mona Adel", "Karim Nabil")))
                .andExpect(jsonPath("$.trainers[*].status", contains("active", "active", "active", "disabled")))
                .andExpect(jsonPath("$.trainers[*].riderCount", contains(0, 2, 1, 1)))
                .andExpect(jsonPath("$.trainers[0].id").value(adam.toString()));
    }

    // Case 9
    @Test
    void sortsUnassignedFirstThenByLevelThenByName() throws Exception {
        rider("b level 2", null, 2, ahmed, null);
        rider("A level 2", null, 2, ahmed, null);
        rider("C level 1", null, 1, mona, null);
        rider("Z unassigned level 4", null, 4, null, null);
        rider("Y unassigned level 3", null, 3, karim, null);
        rider("D level 5", null, 5, ahmed, null);

        list().andExpect(jsonPath("$.riders[*].name", contains(
                "Y unassigned level 3", "Z unassigned level 4",
                "C level 1", "A level 2", "b level 2", "D level 5")));
    }

    // Case 10
    @Test
    void aRebookedMemberGivesOneRowWithThePlacedBookingsAssessor() throws Exception {
        UUID member = member("Rana Rebooked", "01299990000");
        booking(member, mona, "noshow");
        booking(member, ahmed, "placed");
        UUID rana = rider("Rana Rebooked", "01299990000", 2, ahmed, member);

        list()
                .andExpect(jsonPath("$.riders.length()").value(1))
                .andExpect(jsonPath("$.riders[0].id").value(rana.toString()))
                .andExpect(jsonPath("$.riders[0].assessor.id").value(ahmed.toString()))
                .andExpect(jsonPath("$.riders[0].howAssigned").value("from_assessment"));
    }

    // Case 11
    @Test
    void theListRunsTheSameNumberOfQueriesWhateverTheNumberOfRiders() throws Exception {
        rider("Rider 000", "01100000000", 1, ahmed, placedMember("Rider 000", ahmed));
        long withOne = statementsFor(this::list);

        UUID[] trainers = {ahmed, mona, karim, null};
        for (int i = 1; i < 100; i++) {
            String name = "Rider %03d".formatted(i);
            UUID trainer = trainers[i % trainers.length];
            UUID member = i % 5 == 0 ? null : placedMember(name, i % 2 == 0 ? ahmed : karim);
            rider(name, "011%08d".formatted(i), 1 + i % 6, trainer, member);
        }
        list().andExpect(jsonPath("$.riders.length()").value(100));
        long withHundred = statementsFor(this::list);

        assertThat(withHundred).isEqualTo(withOne);
    }

    // Case 12
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(ahmed, Role.TRAINER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    // Sara placed by Ahmed and still his (level 3); Omar placed by Ahmed, moved to Mona (level 3); Nour a seed rider
    // of Ahmed's; Hana placed by Karim, who is now disabled; Youssef placed by Ahmed, with no trainer.
    private void fiveRiders() {
        rider("Sara Ali", "01011112222", 3, ahmed, placedMember("Sara Ali", ahmed));
        rider("Omar Hassan", "01222223333", 3, mona, placedMember("Omar Hassan", ahmed));
        rider("Nour Seed", "01533334444", 2, ahmed, null);
        rider("Hana Karim", "01144445555", 2, karim, placedMember("Hana Karim", karim));
        rider("Youssef Alone", "01255556666", 1, null, placedMember("Youssef Alone", ahmed));
    }

    private ResultActions expectUnfilteredTotalsAndCards(ResultActions result) throws Exception {
        return result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.total").value(5))
                .andExpect(jsonPath("$.totals.fromAssessment").value(1))
                .andExpect(jsonPath("$.totals.changedManually").value(1))
                .andExpect(jsonPath("$.totals.setManually").value(1))
                .andExpect(jsonPath("$.totals.unassigned").value(2))
                .andExpect(jsonPath("$.trainers[*].name", contains("Ahmed Said", "Mona Adel", "Karim Nabil")))
                .andExpect(jsonPath("$.trainers[*].riderCount", contains(2, 1, 1)));
    }

    private ResultActions list() throws Exception {
        return mvc.perform(asAdmin(get(PATH)));
    }

    private ResultActions list(String param, String value) throws Exception {
        return mvc.perform(asAdmin(get(PATH).param(param, value)));
    }

    private static String row(String riderName, String field) {
        return "$.riders[?(@.name == '" + riderName + "')]." + field;
    }

    private long statementsFor(Request request) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        request.run().andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private UUID trainer(String name, String status) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, ?, ?) returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "trainer%d@school.eg".formatted(seq), status,
                status.equals("invited") ? null : "not-a-real-hash");
    }

    private UUID member(String name, String phone) {
        return jdbc.queryForObject("insert into member (name, phone) values (?, ?) returning id",
                UUID.class, name, phone);
    }

    // A member placed by this assessor.
    private UUID placedMember(String name, UUID assessor) {
        UUID member = member(name, null);
        booking(member, assessor, "placed");
        return member;
    }

    // An assessment slot (no level) on that date. Saved through JPA, as the app saves them.
    private UUID slot(LocalDate date) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            SessionTemplate template = new SessionTemplate();
            template.setDayOfWeek(date.getDayOfWeek());
            template.setStartTime(LocalTime.of(10, 0));
            template.setEndTime(LocalTime.of(11, 0));
            template.setCapacity(6);
            template.setValidFrom(date);
            template = sessionTemplateRepository.save(template);
            return sessionRepository.save(Session.of(template, date)).getId();
        });
    }

    // The result columns follow the status as the table's check requires; placed members go to level 2.
    private void booking(UUID member, UUID assessor, String status) {
        boolean placed = status.equals("placed");
        jdbc.update("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at)
                        values (?, ?, ?, ?, ?, ?, ?)""",
                member, slot, assessor, status, placed, placed ? 2 : null, Timestamp.from(Instant.now()));
    }

    private UUID rider(String name, String phone, int level, UUID trainer, UUID member) {
        return jdbc.queryForObject(
                "insert into rider (name, phone, level_id, trainer_id, member_id) values (?, ?, ?, ?, ?) returning id",
                UUID.class, name, phone, level, trainer, member);
    }

    @FunctionalInterface
    private interface Request {
        ResultActions run() throws Exception;
    }
}
