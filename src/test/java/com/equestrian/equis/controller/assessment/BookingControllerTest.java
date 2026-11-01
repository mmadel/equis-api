package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.assessment.RecordResultRequest;
import com.equestrian.equis.dto.assessment.ResultRecorded;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.assessment.RecordResultService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class BookingControllerTest {
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID RIDER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final String PATH = "/me/bookings/" + BOOKING_ID + "/result";
    private static final AuthUser ME = new AuthUser(TRAINER_ID, Role.TRAINER);
    private static final String PLACED = """
            {"attended": true, "levelId": 3, "notes": "Good seat"}
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    RecordResultService service;
    @MockitoBean
    TrainerRepository trainerRepository;
    @MockitoBean
    RiderRepository riderRepository;

    @BeforeEach
    void trainerIsActive() {
        when(trainerRepository.existsByIdAndStatus(TRAINER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void recordsTheResultForTheCaller() throws Exception {
        when(service.record(BOOKING_ID, ME, new RecordResultRequest(true, 3, "Good seat")))
                .thenReturn(new ResultRecorded(BOOKING_ID, "placed", RIDER_ID));

        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(PLACED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.status").value("placed"))
                .andExpect(jsonPath("$.riderId").value(RIDER_ID.toString()));
    }

    @Test
    void aWrongShapeIsAValidationFailure() throws Exception {
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("attended"));
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"attended": false, "notes": "%s"}
                        """.formatted("x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("notes"));
        verifyNoInteractions(service);
    }

    @Test
    void aBadIdOrBodyIsABadRequest() throws Exception {
        mvc.perform(trainer(post("/me/bookings/abc/result")).contentType(MediaType.APPLICATION_JSON).content(PLACED))
                .andExpect(status().isBadRequest());
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void serviceRefusalsComeBackAsProblemDetail() throws Exception {
        when(service.record(eq(BOOKING_ID), eq(ME), any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                        "Some fields are not valid.",
                        Map.of("fieldErrors", List.of(new ApiFieldError("levelId", ErrorCodes.LEVEL_REQUIRED)))))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.NOT_ASSESSOR,
                        "Only the assessor can record this result."))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, ErrorCodes.SLOT_NOT_STARTED,
                        "Results can be recorded once the slot starts."));

        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(PLACED))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("levelId"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("level_required"));
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(PLACED))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not_assessor"));
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(PLACED))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slot_not_started"));
    }

    @Test
    void onlyTrainersGetIn() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(PLACED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(PLACED))
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
