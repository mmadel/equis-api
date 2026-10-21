package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.assessment.MemberBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E11: trainers no longer register walk-ins (A1); the admin does it in the web console.
@WebMvcTest(controllers = MemberController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class MemberControllerTest {
    private static final String PATH = "/me/members";
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String BODY = """
            {"name": "Sara Mahmoud", "phone": "01012345678", "slotId": "00000000-0000-0000-0000-0000000000a1"}
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    TrainerRepository trainerRepository;
    // Only here to show the endpoint no longer books or saves anything.
    @MockitoBean
    MemberBookingService booking;
    @MockitoBean
    MemberRepository members;

    @BeforeEach
    void trainerIsActive() {
        when(trainerRepository.existsByIdAndStatus(TRAINER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    // Case 9
    @Test
    void aTrainerIsToldTheEndpointIsGoneAndNothingIsSaved() throws Exception {
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isGone())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("endpoint_removed"))
                .andExpect(jsonPath("$.detail")
                        .value("Trainers no longer register new people; the admin does this in the web console."));
        verifyNoInteractions(booking, members);
    }

    @Test
    void theBodyIsNotReadSoAnyRequestIsGone() throws Exception {
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("endpoint_removed"));
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER)))
                .andExpect(status().isGone());
    }

    @Test
    void accessRulesAreUnchanged() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(TRAINER_ID, role)).value();
    }
}
