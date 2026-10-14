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
import com.equestrian.equis.dto.assessment.NewMemberRequest;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.assessment.MemberBookingService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class MemberControllerTest {
    private static final String PATH = "/me/members";
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SLOT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final String BODY = """
            {"name": "Sara Mahmoud", "phone": "01012345678", "slotId": "%s"}
            """.formatted(SLOT_ID);

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    MemberBookingService service;
    @MockitoBean
    TrainerRepository trainerRepository;

    @BeforeEach
    void trainerIsActive() {
        when(trainerRepository.existsByIdAndStatus(TRAINER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void registersAndBooksTheMember() throws Exception {
        AssessmentBooking booking = booking();
        when(service.registerAndBook(TRAINER_ID, new NewMemberRequest("Sara Mahmoud", "01012345678", SLOT_ID)))
                .thenReturn(booking);

        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.memberId").value(MEMBER_ID.toString()))
                .andExpect(jsonPath("$.slotId").value(SLOT_ID.toString()));
    }

    @Test
    void aBlankPhoneIsNoPhone() throws Exception {
        AssessmentBooking booking = booking();
        when(service.registerAndBook(eq(TRAINER_ID), any())).thenReturn(booking);

        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": " Sara Mahmoud ", "phone": "  ", "slotId": "%s"}
                        """.formatted(SLOT_ID)))
                .andExpect(status().isCreated());
        verify(service).registerAndBook(TRAINER_ID, new NewMemberRequest("Sara Mahmoud", null, SLOT_ID));
    }

    @Test
    void aWrongShapeIsAValidationFailure() throws Exception {
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Sara", "phone": "12345", "slotId": "%s"}
                        """.formatted(SLOT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));
        verifyNoInteractions(service);
    }

    @Test
    void anUnreadableBodyIsABadRequest() throws Exception {
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Sara", "slotId": "abc"}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void ruleRefusalsComeBackAsProblemDetail() throws Exception {
        when(service.registerAndBook(eq(TRAINER_ID), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, ErrorCodes.SLOT_FULL, "This slot is full."))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, ErrorCodes.PHONE_TAKEN,
                        "This phone number is already registered.",
                        Map.of("fieldErrors", List.of(new ApiFieldError("phone", ErrorCodes.PHONE_TAKEN)))));

        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("slot_full"));
        mvc.perform(trainer(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("phone_taken"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));
    }

    @Test
    void onlyTrainersGetIn() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        verifyNoInteractions(service);
    }

    private static AssessmentBooking booking() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(MEMBER_ID);
        Session slot = mock(Session.class);
        when(slot.getId()).thenReturn(SLOT_ID);
        AssessmentBooking booking = mock(AssessmentBooking.class);
        when(booking.getId()).thenReturn(BOOKING_ID);
        when(booking.getMember()).thenReturn(member);
        when(booking.getSession()).thenReturn(slot);
        return booking;
    }

    private MockHttpServletRequestBuilder trainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(TRAINER_ID, role)).value();
    }
}
