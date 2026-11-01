package com.equestrian.equis.common.config;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class,
        SecurityConfigTest.TestController.class})
public class SecurityConfigTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    TrainerRepository trainerRepository;
    @MockitoBean
    RiderRepository riderRepository;

    @RestController
    static class TestController {

        @GetMapping({"/auth/ping", "/admin/ping", "/assessment/ping", "/other/ping", "/actuator/health"})
        String ping() {
            return "pong";
        }

        @GetMapping("/me/ping")
        String me(@AuthenticationPrincipal AuthUser user) {
            return user.id() + ":" + user.role();
        }
    }

    @Test
    void authPathsAreOpenWithoutAToken() throws Exception {
        mvc.perform(get("/auth/ping")).andExpect(status().isOk());
    }

    @Test
    void authPathsIgnoreABadToken() throws Exception {
        mvc.perform(get("/auth/ping").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isOk());
    }

    @Test
    void healthIsOpenWithoutATokenAndIgnoresABadOne() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isOk());
    }

    @Test
    void missingToken_returns401ProblemDetail() throws Exception {
        mvc.perform(get("/me/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void badToken_returns401ProblemDetail() throws Exception {
        mvc.perform(get("/me/ping").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void activeTrainerReachesTrainerPathsAsAuthUser() throws Exception {
        when(trainerRepository.existsByIdAndStatus(ID, TrainerStatus.ACTIVE)).thenReturn(true);

        mvc.perform(get("/me/ping").header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(content().string(ID + ":TRAINER"));
        mvc.perform(get("/assessment/ping").header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER)))
                .andExpect(status().isOk());
    }

    @Test
    void disabledTrainer_returns401() throws Exception {
        when(trainerRepository.existsByIdAndStatus(ID, TrainerStatus.ACTIVE)).thenReturn(false);

        mvc.perform(get("/me/ping").header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void trainerOnAdminPath_returns403ProblemDetail() throws Exception {
        when(trainerRepository.existsByIdAndStatus(ID, TrainerStatus.ACTIVE)).thenReturn(true);

        mvc.perform(get("/admin/ping").header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void adminReachesAdminPathsButNotTrainerPaths() throws Exception {
        mvc.perform(get("/admin/ping").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk());
        mvc.perform(get("/me/ping").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyOtherPathIsDeniedEvenWithAValidToken() throws Exception {
        mvc.perform(get("/other/ping").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(get("/other/ping")).andExpect(status().isUnauthorized());
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(ID, role)).value();
    }
}
