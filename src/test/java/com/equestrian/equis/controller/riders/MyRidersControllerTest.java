package com.equestrian.equis.controller.riders;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.riders.MyRiderDto;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.riders.MyRidersQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    MyRidersQueryService query;
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

    private MockHttpServletRequestBuilder trainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(TRAINER_ID, role)).value();
    }
}
