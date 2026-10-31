package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.evaluations.EvaluationRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.evaluations.EvaluationService;
import com.equestrian.equis.service.sessions.AssignmentService;
import com.equestrian.equis.service.sessions.FinishSessionService;
import com.equestrian.equis.service.sessions.SessionDetail;
import com.equestrian.equis.service.sessions.SessionDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MySessionsController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class MySessionsControllerTest {
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID MONA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID ZEINA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID OTHERS_RIDER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    private static final String PATH = "/me/sessions/" + SESSION_ID;

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    SessionDetailService details;
    @MockitoBean
    AssignmentService assignments;
    @MockitoBean
    FinishSessionService finisher;
    @MockitoBean
    EvaluationService evaluator;
    @MockitoBean
    TrainerRepository trainerRepository;

    @BeforeEach
    void trainerIsActive() {
        when(trainerRepository.existsByIdAndStatus(TRAINER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void returnsTheSessionWithMySeatsAndMyRiders() throws Exception {
        when(details.get(TRAINER_ID, SESSION_ID)).thenReturn(detail(true));

        mvc.perform(trainer(get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.level.id").value(2))
                .andExpect(jsonPath("$.level.name").value("Level 2"))
                .andExpect(jsonPath("$.level.colour").value("#2A559A"))
                .andExpect(jsonPath("$.date").value("2026-10-17"))
                .andExpect(jsonPath("$.start").value("08:30"))
                .andExpect(jsonPath("$.end").value("09:30"))
                .andExpect(jsonPath("$.capacity").value(6))
                .andExpect(jsonPath("$.status").value("scheduled"))
                .andExpect(jsonPath("$.finishedAt").value(nullValue()))
                .andExpect(jsonPath("$.canAssign").value(true))
                .andExpect(jsonPath("$.canFinish").value(false))
                .andExpect(jsonPath("$.readOnly").value(false))
                .andExpect(jsonPath("$.otherCoachSeats").value(1))
                .andExpect(jsonPath("$.mySeats.length()").value(1))
                .andExpect(jsonPath("$.mySeats[0].riderId").value(MONA_ID.toString()))
                .andExpect(jsonPath("$.mySeats[0].name").value("Mona Ali"))
                .andExpect(jsonPath("$.mySeats[0].initials").value("MA"))
                .andExpect(jsonPath("$.mySeats[0].sessionsLeft").value(8))
                .andExpect(jsonPath("$.mySeats[0].evaluation").value(nullValue()))
                .andExpect(jsonPath("$.mySeats[0].canEvaluate").value(false))
                .andExpect(jsonPath("$.riders.length()").value(2))
                .andExpect(jsonPath("$.riders[0].id").value(MONA_ID.toString()))
                .andExpect(jsonPath("$.riders[0].name").value("Mona Ali"))
                .andExpect(jsonPath("$.riders[0].initials").value("MA"))
                .andExpect(jsonPath("$.riders[0].inThisSession").value(true))
                .andExpect(jsonPath("$.riders[0].sessionsLeft").value(8))
                .andExpect(jsonPath("$.riders[1].id").value(ZEINA_ID.toString()))
                .andExpect(jsonPath("$.riders[1].inThisSession").value(false))
                .andExpect(jsonPath("$.riders[1].sessionsLeft").value(1));
    }

    @Test
    void readOnlySessionIsStillReturned() throws Exception {
        when(details.get(TRAINER_ID, SESSION_ID)).thenReturn(detail(false));

        mvc.perform(trainer(get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readOnly").value(true))
                .andExpect(jsonPath("$.canAssign").value(false));
    }

    @Test
    void missingSessionIs404() throws Exception {
        when(details.get(TRAINER_ID, SESSION_ID)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND, ErrorCodes.SESSION_NOT_FOUND, "This session does not exist."));

        mvc.perform(trainer(get(PATH)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("session_not_found"));
    }

    @Test
    void putSavesThenAnswersWithTheSessionAsSaved() throws Exception {
        when(details.get(TRAINER_ID, SESSION_ID)).thenReturn(detail(true));

        mvc.perform(trainer(put(PATH + "/assignments")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderIds\": [\"" + MONA_ID + "\", \"" + ZEINA_ID + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.mySeats[0].riderId").value(MONA_ID.toString()));

        InOrder order = inOrder(assignments, details);
        order.verify(assignments).replaceMine(TRAINER_ID, SESSION_ID, List.of(MONA_ID, ZEINA_ID));
        order.verify(details).get(TRAINER_ID, SESSION_ID);
    }

    @Test
    void putAcceptsAnEmptyList() throws Exception {
        when(details.get(TRAINER_ID, SESSION_ID)).thenReturn(detail(true));

        mvc.perform(trainer(put(PATH + "/assignments")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderIds\": []}"))
                .andExpect(status().isOk());

        inOrder(assignments).verify(assignments).replaceMine(TRAINER_ID, SESSION_ID, List.of());
    }

    @Test
    void putWithoutTheListIsRefused() throws Exception {
        for (String body : List.of("{}", "{\"riderIds\": null}", "{\"riderIds\": [null]}")) {
            mvc.perform(trainer(put(PATH + "/assignments")).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("validation_failed"));
        }
        verifyNoInteractions(assignments, details);
    }

    @Test
    void putPassesRuleRefusalsThrough() throws Exception {
        when(assignments.replaceMine(TRAINER_ID, SESSION_ID, List.of(MONA_ID))).thenThrow(new ApiException(
                HttpStatus.CONFLICT, ErrorCodes.CAPACITY_EXCEEDED, "This session has 0 seats left for your riders."));

        mvc.perform(trainer(put(PATH + "/assignments")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderIds\": [\"" + MONA_ID + "\"]}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("capacity_exceeded"));
        verifyNoInteractions(details);
    }

    @Test
    void finishThenAnswersWithTheSessionAsFinished() throws Exception {
        when(details.get(TRAINER_ID, SESSION_ID)).thenReturn(detail(false));

        mvc.perform(trainer(post(PATH + "/finish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()));

        InOrder order = inOrder(finisher, details);
        order.verify(finisher).finish(TRAINER_ID, SESSION_ID);
        order.verify(details).get(TRAINER_ID, SESSION_ID);
    }

    @Test
    void finishPassesRefusalsThrough() throws Exception {
        when(finisher.finish(TRAINER_ID, SESSION_ID)).thenThrow(new ApiException(
                HttpStatus.CONFLICT, ErrorCodes.SESSION_NOT_STARTED, "This session has not started yet."));

        mvc.perform(trainer(post(PATH + "/finish")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_not_started"));
        verifyNoInteractions(details);
    }

    @Test
    void evaluateSavesThenAnswersWithTheSessionAsSaved() throws Exception {
        when(details.get(TRAINER_ID, SESSION_ID)).thenReturn(detail(false));

        mvc.perform(trainer(post(PATH + "/evaluations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderId\": \"" + MONA_ID + "\", \"stars\": 4, \"comment\": \"  Heels down \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()));

        InOrder order = inOrder(evaluator, details);
        order.verify(evaluator).evaluate(TRAINER_ID, SESSION_ID, new EvaluationRequest(MONA_ID, 4, "Heels down"));
        order.verify(details).get(TRAINER_ID, SESSION_ID);
    }

    @Test
    void evaluatePassesRefusalsThrough() throws Exception {
        EvaluationRequest req = new EvaluationRequest(MONA_ID, 4, null);
        when(evaluator.evaluate(TRAINER_ID, SESSION_ID, req)).thenThrow(new ApiException(
                HttpStatus.CONFLICT, ErrorCodes.ALREADY_EVALUATED, "Mona Ali already has an evaluation for this session."));

        mvc.perform(trainer(post(PATH + "/evaluations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderId\": \"" + MONA_ID + "\", \"stars\": 4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_evaluated"));
        verifyNoInteractions(details);
    }

    @Test
    void onlyTrainersGetIn() throws Exception {
        mvc.perform(post(PATH + "/evaluations").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"riderId\": \"" + MONA_ID + "\", \"stars\": 4}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(evaluator);
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden());
        mvc.perform(put(PATH + "/assignments").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"riderIds\": []}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(PATH + "/finish").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(assignments, details, finisher);
    }

    private MockHttpServletRequestBuilder trainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(TRAINER_ID, role)).value();
    }

    // Mona (8 left) is seated by the caller, one more seat by another trainer; Zeina (1 left) is the caller's
    // rider without a seat.
    private static SessionDetail detail(boolean canAssign) {
        Level level = new Level();
        level.setId((short) 2);
        level.setName("Level 2");
        level.setColour("#2A559A");
        SessionTemplate template = new SessionTemplate();
        template.setLevel(level);
        template.setStartTime(LocalTime.of(8, 30));
        template.setEndTime(LocalTime.of(9, 30));
        template.setCapacity(6);
        Session session = Session.of(template, LocalDate.of(2026, 10, 17));
        ReflectionTestUtils.setField(session, "id", SESSION_ID);

        Rider mona = rider(MONA_ID, "Mona Ali");
        Rider zeina = rider(ZEINA_ID, "Zeina Hassan");
        return new SessionDetail(session,
                List.of(Assignment.of(session, rider(OTHERS_RIDER_ID, "Youssef Tarek"), trainer(OTHER_TRAINER_ID)),
                        Assignment.of(session, mona, trainer(TRAINER_ID))),
                List.of(mona, zeina), Map.of(MONA_ID, 8, ZEINA_ID, 1), Map.of(), canAssign, false);
    }

    private static Rider rider(UUID id, String name) {
        Rider rider = new Rider();
        rider.setId(id);
        rider.setName(name);
        return rider;
    }

    private static Trainer trainer(UUID id) {
        Trainer trainer = new Trainer();
        trainer.setId(id);
        return trainer;
    }
}
