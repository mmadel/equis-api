package com.equestrian.equis.controller.levels;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.levels.LevelRef;
import com.equestrian.equis.dto.levels.LevelSessionsResponse;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.Day;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.Seat;
import com.equestrian.equis.dto.levels.LevelSessionsResponse.SessionSlot;
import com.equestrian.equis.dto.levels.MyLevelSummary;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.levels.MyLevelsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MyLevelsController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class MyLevelsControllerTest {
    private static final String PATH = "/me/levels";
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID RIDER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final LocalDate FROM = LocalDate.of(2026, 10, 17);
    private static final LocalDate TO = LocalDate.of(2026, 10, 23);

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    MyLevelsService service;
    @MockitoBean
    TrainerRepository trainerRepository;

    @BeforeEach
    void trainerIsActive() {
        when(trainerRepository.existsByIdAndStatus(TRAINER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void listsTheCallersLevelSummaries() throws Exception {
        when(service.summaries(TRAINER_ID)).thenReturn(List.of(
                new MyLevelSummary(1, "Level 1", "#2F5D46", 6, 3, 14, 36)));

        mvc.perform(trainer(get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Level 1"))
                .andExpect(jsonPath("$[0].colour").value("#2F5D46"))
                .andExpect(jsonPath("$[0].sessionsThisWeek").value(6))
                .andExpect(jsonPath("$[0].myRiders").value(3))
                .andExpect(jsonPath("$[0].seatsFilled").value(14))
                .andExpect(jsonPath("$[0].seatsTotal").value(36));
    }

    @Test
    void listsALevelsSessionsByDay() throws Exception {
        when(service.sessions(TRAINER_ID, (short) 2, FROM, TO)).thenReturn(new LevelSessionsResponse(
                new LevelRef(2, "Level 2", "#2A559A"), FROM, TO, List.of(new Day(FROM, List.of(
                new SessionSlot(SESSION_ID, LocalTime.of(8, 30), LocalTime.of(9, 30), "scheduled", 6, 2, false,
                        List.of(new Seat(true, RIDER_ID, "MA"), Seat.other())))))));

        mvc.perform(trainer(get(PATH + "/2/sessions").param("from", "2026-10-17").param("to", "2026-10-23")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level.id").value(2))
                .andExpect(jsonPath("$.level.name").value("Level 2"))
                .andExpect(jsonPath("$.level.colour").value("#2A559A"))
                .andExpect(jsonPath("$.from").value("2026-10-17"))
                .andExpect(jsonPath("$.to").value("2026-10-23"))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-17"))
                .andExpect(jsonPath("$.days[0].sessions[0].id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.days[0].sessions[0].start").value("08:30"))
                .andExpect(jsonPath("$.days[0].sessions[0].end").value("09:30"))
                .andExpect(jsonPath("$.days[0].sessions[0].status").value("scheduled"))
                .andExpect(jsonPath("$.days[0].sessions[0].capacity").value(6))
                .andExpect(jsonPath("$.days[0].sessions[0].booked").value(2))
                .andExpect(jsonPath("$.days[0].sessions[0].past").value(false))
                .andExpect(jsonPath("$.days[0].sessions[0].seats[0].mine").value(true))
                .andExpect(jsonPath("$.days[0].sessions[0].seats[0].riderId").value(RIDER_ID.toString()))
                .andExpect(jsonPath("$.days[0].sessions[0].seats[0].initials").value("MA"))
                // Another trainer's seat shows nothing about the rider.
                .andExpect(jsonPath("$.days[0].sessions[0].seats[1].mine").value(false))
                .andExpect(jsonPath("$.days[0].sessions[0].seats[1].riderId").doesNotExist())
                .andExpect(jsonPath("$.days[0].sessions[0].seats[1].initials").doesNotExist());
    }

    @Test
    void serviceRefusalsComeBackAsProblemDetail() throws Exception {
        when(service.sessions(TRAINER_ID, (short) 9, FROM, TO)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND, ErrorCodes.LEVEL_NOT_FOUND, "This level does not exist."));
        when(service.sessions(TRAINER_ID, (short) 2, TO, FROM)).thenThrow(new ApiException(
                HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE, "The dates must be in order."));

        mvc.perform(trainer(get(PATH + "/9/sessions").param("from", "2026-10-17").param("to", "2026-10-23")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("level_not_found"));
        mvc.perform(trainer(get(PATH + "/2/sessions").param("from", "2026-10-23").param("to", "2026-10-17")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_range"));
    }

    @Test
    void missingOrMalformedDatesAreBadRequests() throws Exception {
        mvc.perform(trainer(get(PATH + "/2/sessions").param("from", "2026-10-17")))
                .andExpect(status().isBadRequest());
        mvc.perform(trainer(get(PATH + "/2/sessions").param("from", "17/10/2026").param("to", "2026-10-23")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void onlyTrainersGetIn() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        verifyNoInteractions(service);
    }

    private MockHttpServletRequestBuilder trainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(TRAINER_ID, role)).value();
    }
}
