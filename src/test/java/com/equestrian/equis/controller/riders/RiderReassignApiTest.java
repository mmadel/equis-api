package com.equestrian.equis.controller.riders;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.service.accounts.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PATCH /admin/riders/trainer (W3) over HTTP against a real database. Rows go in with SQL so each rider's trainer,
// member and placed booking can be set exactly. Not @Transactional, so the race cases commit. The container belongs
// to this class.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=riders-reassign-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@AutoConfigureMockMvc
@Testcontainers
public class RiderReassignApiTest {
    private static final String PATH = "/admin/riders/trainer";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    JdbcTemplate jdbc;

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
        karim = trainer("Karim Nabil", "active");
        slot = session(null, LocalDate.of(2026, 10, 14));
    }

    // Case 1
    @Test
    void movesOneRiderToAnotherTrainer() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, placedMember("Sara Ali", ahmed));

        assign(List.of(sara), mona)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.unchanged").value(0))
                .andExpect(jsonPath("$.riders.length()").value(1))
                .andExpect(jsonPath("$.riders[0].id").value(sara.toString()))
                .andExpect(jsonPath("$.riders[0].trainer.id").value(mona.toString()))
                .andExpect(jsonPath("$.riders[0].trainer.name").value("Mona Adel"))
                .andExpect(jsonPath("$.riders[0].assessor.id").value(ahmed.toString()))
                .andExpect(jsonPath("$.riders[0].howAssigned").value("changed_manually"))
                .andExpect(jsonPath("$.riders[0].canResetToAssessor").value(true));
        assertThat(trainerOf(sara)).isEqualTo(mona);
    }

    // Case 2
    @Test
    void theNewTrainerSeesTheRiderAndTheOldOneNoLonger() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, placedMember("Sara Ali", ahmed));
        assign(List.of(sara), mona).andExpect(status().isOk());

        mvc.perform(asTrainer(mona, get("/me/riders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(sara.toString())));
        mvc.perform(asTrainer(ahmed, get("/me/riders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(sara.toString()))));
    }

    // Case 3. A seat belongs to the trainer who gave it (E3), so it stays Ahmed's: Mona sees it as another coach's
    // seat and the rider as one of hers not yet in this session.
    @Test
    void futureSeatsStayAsTheyAre() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, placedMember("Sara Ali", ahmed));
        UUID tuesday = session(3, LocalDate.now().plusDays(7));
        seat(tuesday, sara, ahmed);

        assign(List.of(sara), mona).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select assigned_by from assignment where session_id = ? and rider_id = ?",
                UUID.class, tuesday, sara)).isEqualTo(ahmed);
        mvc.perform(asTrainer(mona, get("/me/sessions/" + tuesday)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCoachSeats").value(1))
                .andExpect(jsonPath("$.mySeats").isEmpty())
                .andExpect(jsonPath("$.riders[*].id", contains(sara.toString())))
                .andExpect(jsonPath("$.riders[0].inThisSession").value(false));
        // Mona's save leaves seats given by other trainers alone.
        mvc.perform(asTrainer(mona, put("/me/sessions/" + tuesday + "/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"riderIds\": [] }"))
                .andExpect(status().isOk());
        assertThat(count("assignment")).isEqualTo(1);
    }

    // Case 4
    @Test
    void theLevelIsNeverTouched() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, placedMember("Sara Ali", ahmed));

        assign(List.of(sara), mona).andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[0].levelId").value(3));

        assertThat(jdbc.queryForObject("select level_id from rider where id = ?", Integer.class, sara)).isEqualTo(3);
        assertThat(count("level_change")).isZero();
    }

    // Case 5
    @Test
    void bulkCountsUpdatedAndUnchangedAndIgnoresDuplicates() throws Exception {
        UUID a = rider("Rider A", 2, ahmed, null);
        UUID b = rider("Rider B", 2, karim, null);
        UUID c = rider("Rider C", 2, mona, null);

        assign(List.of(a, b, c, a), mona)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2))
                .andExpect(jsonPath("$.unchanged").value(1))
                .andExpect(jsonPath("$.riders[*].id", contains(a.toString(), b.toString(), c.toString())))
                .andExpect(jsonPath("$.riders[*].trainer.id",
                        contains(mona.toString(), mona.toString(), mona.toString())));
    }

    // Case 6
    @Test
    void resetSendsTheRiderBackToTheAssessor() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, placedMember("Sara Ali", ahmed));
        assign(List.of(sara), mona).andExpect(status().isOk());

        reset(List.of(sara))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.riders[0].trainer.id").value(ahmed.toString()))
                .andExpect(jsonPath("$.riders[0].howAssigned").value("from_assessment"))
                .andExpect(jsonPath("$.riders[0].canResetToAssessor").value(false));
        assertThat(trainerOf(sara)).isEqualTo(ahmed);
    }

    // Case 7
    @Test
    void aRiderWithoutAnAssessorBlocksTheWholeReset() throws Exception {
        UUID seed = rider("Nour Seed", 2, ahmed, null);
        UUID omar = rider("Omar Hassan", 2, mona, placedMember("Omar Hassan", ahmed));

        reset(List.of(seed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("no_active_assessor"))
                .andExpect(jsonPath("$.riderIds", contains(seed.toString())));
        reset(List.of(omar, seed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("no_active_assessor"))
                .andExpect(jsonPath("$.riderIds", contains(seed.toString())));

        assertThat(trainerOf(omar)).isEqualTo(mona);
        assertThat(trainerOf(seed)).isEqualTo(ahmed);
    }

    // Case 8
    @Test
    void aDisabledAssessorCannotTakeTheRiderBack() throws Exception {
        UUID hana = rider("Hana Karim", 2, mona, placedMember("Hana Karim", karim));
        jdbc.update("update trainer set status = 'disabled' where id = ?", karim);

        reset(List.of(hana))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("no_active_assessor"))
                .andExpect(jsonPath("$.riderIds", contains(hana.toString())));
        assertThat(trainerOf(hana)).isEqualTo(mona);
    }

    // Case 9
    @Test
    void theTargetMustBeAnActiveTrainer() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, null);
        UUID invited = trainer("Ibrahim Invited", "invited");
        UUID disabled = trainer("Dina Disabled", "disabled");

        for (UUID target : List.of(invited, disabled)) {
            assign(List.of(sara), target)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("trainer_not_active"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("trainerId"));
        }
        assign(List.of(sara), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("trainer_not_found"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("trainerId"));
        assertThat(trainerOf(sara)).isEqualTo(ahmed);
    }

    // Case 10
    @Test
    void anUnknownRiderBlocksTheWholeRequest() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, placedMember("Sara Ali", ahmed));
        UUID unknown = UUID.randomUUID();

        assign(List.of(sara, unknown), mona)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"))
                .andExpect(jsonPath("$.riderIds", contains(unknown.toString())));
        reset(List.of(sara, unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.riderIds", contains(unknown.toString())));
        assertThat(trainerOf(sara)).isEqualTo(ahmed);
    }

    // Case 11
    @Test
    void aBadBodyIsAValidationFailure() throws Exception {
        UUID sara = rider("Sara Ali", 3, ahmed, placedMember("Sara Ali", ahmed));
        String tooMany = IntStream.range(0, 201).mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
                .collect(Collectors.joining(","));

        expectValidationFailed(send("{ \"riderIds\": [], \"trainerId\": \"" + mona + "\" }"), "riderIds");
        expectValidationFailed(send("{ \"riderIds\": [" + tooMany + "], \"trainerId\": \"" + mona + "\" }"),
                "riderIds");
        expectValidationFailed(send("{ \"riderIds\": [\"" + sara + "\"], \"trainerId\": \"" + mona
                + "\", \"resetToAssessor\": true }"), "trainerId")
                .andExpect(jsonPath("$.fieldErrors[0].message").value("trainer_or_reset_required"));
        expectValidationFailed(send("{ \"riderIds\": [\"" + sara + "\"] }"), "trainerId");
        expectValidationFailed(send("{ \"riderIds\": [\"" + sara + "\"], \"resetToAssessor\": false }"),
                "trainerId");
        send("{ \"riderIds\": [null], \"trainerId\": \"" + mona + "\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        assertThat(trainerOf(sara)).isEqualTo(ahmed);
    }

    // Case 12
    @Test
    void ridersMovedOffADisabledTrainerStayWhenTheyAreEnabledAgain() throws Exception {
        UUID a = rider("Rider A", 2, karim, placedMember("Rider A", karim));
        UUID b = rider("Rider B", 2, karim, placedMember("Rider B", karim));
        admin(post("/admin/trainers/" + karim + "/disable").param("confirm", "true")).andExpect(status().isOk());
        listUnassigned().andExpect(jsonPath("$.riders[*].id", containsInAnyOrder(a.toString(), b.toString())));

        assign(List.of(a, b), mona).andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[*].howAssigned", contains("changed_manually", "changed_manually")));
        admin(post("/admin/trainers/" + karim + "/enable")).andExpect(status().isOk());

        assertThat(trainerOf(a)).isEqualTo(mona);
        assertThat(trainerOf(b)).isEqualTo(mona);
        listUnassigned().andExpect(jsonPath("$.riders").isEmpty());
    }

    // Case 13
    @Test
    void assigningToATrainerBeingDisabledIsSerialised() throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ids.add(rider("Rider " + i, 2, karim, null));
        }
        rider("Mona's own", 2, mona, null); // so disabling Mona needs confirm

        List<ResultActions> results = race(
                () -> assign(ids, mona),
                () -> admin(post("/admin/trainers/" + mona + "/disable").param("confirm", "true")));

        ResultActions assigned = results.get(0);
        results.get(1).andExpect(status().isOk());
        int status = assigned.andReturn().getResponse().getStatus();
        assertThat(status).isIn(200, 409);
        if (status == 200) {
            // The assign won: the riders are Mona's, and Mona is now disabled.
            assertThat(ids).allSatisfy(id -> assertThat(trainerOf(id)).isEqualTo(mona));
            listUnassigned().andExpect(jsonPath("$.riders.length()").value(6));
        } else {
            assigned.andExpect(jsonPath("$.code").value("trainer_not_active"));
            assertThat(ids).allSatisfy(id -> assertThat(trainerOf(id)).isEqualTo(karim));
        }
    }

    // Case 14
    @Test
    void overlappingBulkChangesDoNotDeadlock() throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ids.add(rider("Rider " + i, 2, karim, null));
        }
        List<UUID> first = ids.subList(0, 15);
        List<UUID> second = new ArrayList<>(ids.subList(5, 20));
        Collections.reverse(second);

        for (int round = 0; round < 5; round++) {
            List<ResultActions> results = race(() -> assign(first, mona), () -> assign(second, ahmed));
            results.get(0).andExpect(status().isOk());
            results.get(1).andExpect(status().isOk());
        }

        Map<UUID, UUID> trainers = ids.stream().collect(Collectors.toMap(id -> id, this::trainerOf));
        ids.subList(0, 5).forEach(id -> assertThat(trainers.get(id)).isEqualTo(mona));
        ids.subList(15, 20).forEach(id -> assertThat(trainers.get(id)).isEqualTo(ahmed));
        ids.subList(5, 15).forEach(id -> assertThat(trainers.get(id)).isIn(mona, ahmed));
    }

    // Case 15
    @Test
    void onlyTheAdminGetsIn() throws Exception {
        String body = "{ \"riderIds\": [\"" + UUID.randomUUID() + "\"], \"trainerId\": \"" + mona + "\" }";
        mvc.perform(patch(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(asTrainer(ahmed, patch(PATH)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    private ResultActions assign(List<UUID> riderIds, UUID trainerId) throws Exception {
        return send("{ \"riderIds\": %s, \"trainerId\": \"%s\" }".formatted(json(riderIds), trainerId));
    }

    private ResultActions reset(List<UUID> riderIds) throws Exception {
        return send("{ \"riderIds\": %s, \"resetToAssessor\": true }".formatted(json(riderIds)));
    }

    private ResultActions send(String body) throws Exception {
        return admin(patch(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions listUnassigned() throws Exception {
        return admin(get("/admin/riders").param("unassigned", "true")).andExpect(status().isOk());
    }

    private static ResultActions expectValidationFailed(ResultActions result, String field) throws Exception {
        return result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }

    private static String json(List<UUID> ids) {
        return ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    // Starts both calls at once and waits for both; a deadlock would surface as a 500 or a timeout.
    private static List<ResultActions> race(Request first, Request second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ResultActions> a = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return first.send();
            });
            Future<ResultActions> b = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return second.send();
            });
            start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                bearer(new AuthUser(UUID.randomUUID(), Role.ADMIN))));
    }

    private MockHttpServletRequestBuilder asTrainer(UUID trainerId, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(new AuthUser(trainerId, Role.TRAINER)));
    }

    private String bearer(AuthUser user) {
        return "Bearer " + jwtService.issue(user).value();
    }

    private UUID trainerOf(UUID riderId) {
        return jdbc.queryForObject("select trainer_id from rider where id = ?", UUID.class, riderId);
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private UUID trainer(String name, String status) {
        seq++;
        return jdbc.queryForObject("""
                        insert into trainer (name, phone, email, status, password_hash)
                        values (?, ?, ?, ?, ?) returning id""",
                UUID.class, name, "0199999%04d".formatted(seq), "trainer%d@school.eg".formatted(seq), status,
                status.equals("invited") ? null : "not-a-real-hash");
    }

    // A member placed by this assessor in the assessment slot.
    private UUID placedMember(String name, UUID assessor) {
        seq++;
        UUID member = jdbc.queryForObject("insert into member (name, phone, email) values (?, ?, ?) returning id",
                UUID.class, name, "0128888%04d".formatted(seq), "member%d@mail.eg".formatted(seq));
        jdbc.update("""
                        insert into assessment_booking
                            (member_id, session_id, assessor_id, status, attended, level_id, recorded_at)
                        values (?, ?, ?, 'placed', true, 2, ?)""",
                member, slot, assessor, Timestamp.from(Instant.now()));
        return member;
    }

    // A session at 17:00 on that date; a null level makes it an assessment slot.
    private UUID session(Integer levelId, LocalDate date) {
        LocalTime start = LocalTime.of(17, 0);
        UUID template = jdbc.queryForObject("""
                        insert into session_template (level_id, day_of_week, start_time, end_time, capacity, valid_from)
                        values (?, ?, ?, ?, 6, ?) returning id""",
                UUID.class, levelId, date.getDayOfWeek().getValue(), start, start.plusHours(1), date);
        return jdbc.queryForObject("""
                        insert into session (template_id, session_date, start_time, end_time, capacity)
                        values (?, ?, ?, ?, 6) returning id""",
                UUID.class, template, date, start, start.plusHours(1));
    }

    private void seat(UUID sessionId, UUID riderId, UUID assignedBy) {
        jdbc.update("insert into assignment (session_id, rider_id, assigned_by) values (?, ?, ?)",
                sessionId, riderId, assignedBy);
    }

    private UUID rider(String name, int level, UUID trainer, UUID member) {
        return jdbc.queryForObject(
                "insert into rider (name, level_id, trainer_id, member_id) values (?, ?, ?, ?) returning id",
                UUID.class, name, level, trainer, member);
    }

    @FunctionalInterface
    private interface Request {
        ResultActions send() throws Exception;
    }
}
