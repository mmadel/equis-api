package com.equestrian.equis.controller.riders;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.riders.LevelHistoryEntry;
import com.equestrian.equis.dto.riders.MyRiderDto;
import com.equestrian.equis.dto.riders.RiderLevelDto;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.riders.MyRidersQueryService;
import com.equestrian.equis.service.riders.RiderLevelQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MyRidersController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class MyRidersControllerTest {
    private static final String PATH = "/me/riders";
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID NOUR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OMAR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final String LEVEL_PATH = PATH + "/" + NOUR_ID + "/level-history";

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    MyRidersQueryService query;
    @MockitoBean
    RiderLevelQueryService riderLevels;
    @MockitoBean
    TrainerRepository trainerRepository;

    @BeforeEach
    void trainerIsActive() {
        when(trainerRepository.existsByIdAndStatus(TRAINER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void listsTheCallersRidersInTheContractShape() throws Exception {
        when(query.myRiders(TRAINER_ID)).thenReturn(List.of(
                new MyRiderDto(NOUR_ID, "Nour Saleh", "NS", 2, LocalDate.of(2026, 9, 20),
                        new UpcomingSeat(SESSION_ID, LocalDate.of(2026, 10, 17), LocalTime.of(10, 0),
                                LocalTime.of(11, 0))),
                new MyRiderDto(OMAR_ID, "Omar Ali", "OA", 3, LocalDate.of(2026, 10, 1), null)));

        mvc.perform(trainer(get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(NOUR_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Nour Saleh"))
                .andExpect(jsonPath("$[0].initials").value("NS"))
                .andExpect(jsonPath("$[0].levelId", isA(Integer.class)))
                .andExpect(jsonPath("$[0].levelId").value(2))
                .andExpect(jsonPath("$[0].since").value("2026-09-20"))
                .andExpect(jsonPath("$[0].nextSession.sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$[0].nextSession.date").value("2026-10-17"))
                .andExpect(jsonPath("$[0].nextSession.start").value("10:00"))
                .andExpect(jsonPath("$[0].nextSession.end").value("11:00"))
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                // Present and null, not left out.
                .andExpect(jsonPath("$[1].nextSession").hasJsonPath())
                .andExpect(jsonPath("$[1].nextSession").isEmpty());
    }

    @Test
    void noRidersIsAnEmptyArray() throws Exception {
        when(query.myRiders(TRAINER_ID)).thenReturn(List.of());

        mvc.perform(trainer(get(PATH)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void onlyTrainersGetIn() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        verifyNoInteractions(query);
    }

    @Test
    void riderLevelInTheContractShape() throws Exception {
        when(riderLevels.riderLevel(NOUR_ID, TRAINER_ID)).thenReturn(new RiderLevelDto(
                NOUR_ID, "Nour Saleh", "NS", 3, LocalDate.of(2026, 10, 11), true, true,
                List.of(new UpcomingSeat(SESSION_ID, LocalDate.of(2026, 10, 15), LocalTime.of(18, 0),
                        LocalTime.of(19, 0))),
                List.of(new LevelHistoryEntry("demoted", 4, 3, "Lost confidence", "Ahmed Fathy",
                                Instant.parse("2026-10-14T15:20:00Z")),
                        new LevelHistoryEntry("placed", null, 3, null, "Ahmed Fathy",
                                Instant.parse("2026-10-11T08:40:00Z")))));

        mvc.perform(trainer(get(LEVEL_PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOUR_ID.toString()))
                .andExpect(jsonPath("$.name").value("Nour Saleh"))
                .andExpect(jsonPath("$.initials").value("NS"))
                .andExpect(jsonPath("$.levelId").value(3))
                .andExpect(jsonPath("$.since").value("2026-10-11"))
                .andExpect(jsonPath("$.canPromote").value(true))
                .andExpect(jsonPath("$.canDemote").value(true))
                .andExpect(jsonPath("$.upcomingSeats[0].sessionId").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.upcomingSeats[0].date").value("2026-10-15"))
                .andExpect(jsonPath("$.upcomingSeats[0].start").value("18:00"))
                .andExpect(jsonPath("$.upcomingSeats[0].end").value("19:00"))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[0].type").value("demoted"))
                .andExpect(jsonPath("$.history[0].fromLevelId").value(4))
                .andExpect(jsonPath("$.history[0].toLevelId").value(3))
                .andExpect(jsonPath("$.history[0].reason").value("Lost confidence"))
                .andExpect(jsonPath("$.history[0].byName").value("Ahmed Fathy"))
                .andExpect(jsonPath("$.history[0].at").value("2026-10-14T15:20:00Z"))
                .andExpect(jsonPath("$.history[1].type").value("placed"))
                // Present and null, not left out.
                .andExpect(jsonPath("$.history[1].fromLevelId").hasJsonPath())
                .andExpect(jsonPath("$.history[1].fromLevelId").isEmpty())
                .andExpect(jsonPath("$.history[1].reason").hasJsonPath())
                .andExpect(jsonPath("$.history[1].reason").isEmpty());
    }

    @Test
    void riderLevelOfARiderThatIsNotTheCallersIsNotFound() throws Exception {
        when(riderLevels.riderLevel(NOUR_ID, TRAINER_ID)).thenThrow(new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.RIDER_NOT_FOUND, "This rider does not exist."));

        mvc.perform(trainer(get(LEVEL_PATH)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
    }

    @Test
    void riderLevelWithAnIdThatIsNotAUuidIsABadRequest() throws Exception {
        mvc.perform(trainer(get(PATH + "/abc/level-history")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(riderLevels);
    }

    @Test
    void onlyTrainersGetARiderLevel() throws Exception {
        mvc.perform(get(LEVEL_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(LEVEL_PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        verifyNoInteractions(riderLevels);
    }

    private MockHttpServletRequestBuilder trainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(TRAINER_ID, role)).value();
    }
}
