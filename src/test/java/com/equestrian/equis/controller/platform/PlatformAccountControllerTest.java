package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.platform.AccountFilter;
import com.equestrian.equis.dto.platform.AccountPage;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountRow;
import com.equestrian.equis.dto.platform.PlatformAccountStatus;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.platform.SuperAdminRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.platform.PlatformAccountService;
import com.equestrian.equis.support.ActiveClubAccess;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlatformAccountController.class)
@Import({SecurityConfig.class, ActiveClubAccess.Config.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class,
        JwtService.class})
class PlatformAccountControllerTest {
    private static final UUID SUPER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID CLUB_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID HANY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID LAILA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private static final String PATH = "/platform/clubs/" + CLUB_ID + "/accounts";

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    PlatformAccountService accounts;
    @MockitoBean
    TrainerRepository trainerRepository;
    @MockitoBean
    RiderRepository riderRepository;
    @MockitoBean
    SuperAdminRepository superAdminRepository;

    @BeforeEach
    void accountsExist() {
        when(superAdminRepository.existsById(SUPER_ID)).thenReturn(true);
        when(trainerRepository.existsByIdAndStatus(USER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void listsInTheContractShapeWithDefaultPaging() throws Exception {
        when(accounts.list(eq(CLUB_ID), any(), eq(0), eq(25))).thenReturn(new AccountPage(List.of(
                new PlatformAccountRow(HANY_ID, AccountType.ADMIN, "Hany Saeed", "hany@cairo-eq.com", null,
                        PlatformAccountStatus.ACTIVE, Instant.parse("2026-11-21T07:12:00Z")),
                new PlatformAccountRow(LAILA_ID, AccountType.RIDER, "Laila Mostafa", null, "01012345678",
                        PlatformAccountStatus.NO_ACCOUNT, null)), 0, 25, 2));

        mvc.perform(superAdmin(get(PATH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].length()").value(7))
                .andExpect(jsonPath("$.items[0].id").value(HANY_ID.toString()))
                .andExpect(jsonPath("$.items[0].type").value("ADMIN"))
                .andExpect(jsonPath("$.items[0].name").value("Hany Saeed"))
                .andExpect(jsonPath("$.items[0].email").value("hany@cairo-eq.com"))
                .andExpect(jsonPath("$.items[0].phone").hasJsonPath())
                .andExpect(jsonPath("$.items[0].phone").isEmpty())
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].lastSignInAt").value("2026-11-21T07:12:00Z"))
                .andExpect(jsonPath("$.items[1].type").value("RIDER"))
                .andExpect(jsonPath("$.items[1].status").value("NO_ACCOUNT"))
                .andExpect(jsonPath("$.items[1].email").isEmpty())
                .andExpect(jsonPath("$.items[1].lastSignInAt").isEmpty());
    }

    @Test
    void passesTheFiltersAndPaging() throws Exception {
        when(accounts.list(any(), any(), anyInt(), anyInt())).thenReturn(new AccountPage(List.of(), 1, 2, 7));

        mvc.perform(superAdmin(get(PATH).param("q", " laila ").param("type", "RIDER").param("status", "ACTIVE")
                        .param("page", "1").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(7));

        verify(accounts).list(CLUB_ID, new AccountFilter(" laila ", AccountType.RIDER, PlatformAccountStatus.ACTIVE),
                1, 2);
    }

    @Test
    void unknownTypeOrStatusIsAValidationError() throws Exception {
        mvc.perform(superAdmin(get(PATH).param("type", "OWNER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("type"));
        mvc.perform(superAdmin(get(PATH).param("status", "LOCKED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        verifyNoInteractions(accounts);
    }

    @Test
    void pageAndSizeOutOfRangeAreValidationErrors() throws Exception {
        mvc.perform(superAdmin(get(PATH).param("size", "500")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        mvc.perform(superAdmin(get(PATH).param("size", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        mvc.perform(superAdmin(get(PATH).param("page", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"));
        verifyNoInteractions(accounts);
    }

    @Test
    void unknownClubIsNotFound() throws Exception {
        when(accounts.list(any(), any(), anyInt(), anyInt())).thenThrow(
                new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.CLUB_NOT_FOUND, "This club doesn't exist."));

        mvc.perform(superAdmin(get(PATH)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("club_not_found"));
    }

    @Test
    void onlyTheSuperAdminGetsIn() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, clubBearer(Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, clubBearer(Role.TRAINER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        verifyNoInteractions(accounts);
    }

    private MockHttpServletRequestBuilder superAdmin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.issue(new AuthUser(SUPER_ID, Role.SUPER_ADMIN, null)).value());
    }

    private String clubBearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(USER_ID, role, Club.DEFAULT_ID)).value();
    }
}
