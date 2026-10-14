package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.assessment.AssessmentSlotDayDto;
import com.equestrian.equis.dto.assessment.AssessmentSlotDto;
import com.equestrian.equis.dto.assessment.AssessmentSlotsDto;
import com.equestrian.equis.dto.assessment.BookingRowDto;
import com.equestrian.equis.dto.assessment.SlotDetailDto;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.assessment.AssessmentSlotQueryService;
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

@WebMvcTest(controllers = AssessmentSlotController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class AssessmentSlotControllerTest {
    private static final String LIST = "/assessment/slots";
    private static final String DETAIL = "/me/assessment/slots/";
    private static final UUID TRAINER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SLOT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final LocalDate FROM = LocalDate.of(2026, 10, 15);
    private static final LocalDate TO = LocalDate.of(2026, 10, 28);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 18);

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    AssessmentSlotQueryService query;
    @MockitoBean
    TrainerRepository trainerRepository;

    @BeforeEach
    void trainerIsActive() {
        when(trainerRepository.existsByIdAndStatus(TRAINER_ID, TrainerStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void listsSlotsByDayWithoutDates() throws Exception {
        when(query.slots(null, null)).thenReturn(new AssessmentSlotsDto(FROM, TO, List.of(
                new AssessmentSlotDayDto(SATURDAY, List.of(new AssessmentSlotDto(
                        SLOT_ID, SATURDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), 4, 3, true))))));

        mvc.perform(trainer(get(LIST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-15"))
                .andExpect(jsonPath("$.to").value("2026-10-28"))
                .andExpect(jsonPath("$.days[0].date").value("2026-10-18"))
                .andExpect(jsonPath("$.days[0].slots[0].id").value(SLOT_ID.toString()))
                .andExpect(jsonPath("$.days[0].slots[0].date").value("2026-10-18"))
                .andExpect(jsonPath("$.days[0].slots[0].start").value("10:00"))
                .andExpect(jsonPath("$.days[0].slots[0].end").value("11:00"))
                .andExpect(jsonPath("$.days[0].slots[0].capacity").value(4))
                .andExpect(jsonPath("$.days[0].slots[0].booked").value(3))
                .andExpect(jsonPath("$.days[0].slots[0].open").value(true));
    }

    @Test
    void passesTheDatesThrough() throws Exception {
        when(query.slots(FROM, TO)).thenReturn(new AssessmentSlotsDto(FROM, TO, List.of()));

        mvc.perform(trainer(get(LIST).param("from", "2026-10-15").param("to", "2026-10-28")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(0));
    }

    @Test
    void rangeRefusalsAndBadDatesAreBadRequests() throws Exception {
        when(query.slots(TO, FROM)).thenThrow(new ApiException(
                HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_RANGE, "The dates must be in order."));

        mvc.perform(trainer(get(LIST).param("from", "2026-10-28").param("to", "2026-10-15")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("invalid_range"));
        mvc.perform(trainer(get(LIST).param("from", "2026-13-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void showsOneSlotWithItsBookings() throws Exception {
        when(query.slot(SLOT_ID, TRAINER_ID)).thenReturn(new SlotDetailDto(SLOT_ID, SATURDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 4, 1, false, true, List.of(
                new BookingRowDto(BOOKING_ID, MEMBER_ID, "Laila Hassan", null, OTHER_ID, "Mona Adel", false,
                        "waiting", null, null))));

        mvc.perform(trainer(get(DETAIL + SLOT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SLOT_ID.toString()))
                .andExpect(jsonPath("$.date").value("2026-10-18"))
                .andExpect(jsonPath("$.start").value("10:00"))
                .andExpect(jsonPath("$.end").value("11:00"))
                .andExpect(jsonPath("$.capacity").value(4))
                .andExpect(jsonPath("$.booked").value(1))
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.started").value(true))
                .andExpect(jsonPath("$.bookings[0].id").value(BOOKING_ID.toString()))
                .andExpect(jsonPath("$.bookings[0].memberId").value(MEMBER_ID.toString()))
                .andExpect(jsonPath("$.bookings[0].memberName").value("Laila Hassan"))
                .andExpect(jsonPath("$.bookings[0].assessorId").value(OTHER_ID.toString()))
                .andExpect(jsonPath("$.bookings[0].assessorName").value("Mona Adel"))
                .andExpect(jsonPath("$.bookings[0].mine").value(false))
                .andExpect(jsonPath("$.bookings[0].status").value("waiting"))
                // Sent as null, as in the contract.
                .andExpect(jsonPath("$.bookings[0].memberPhone").isEmpty())
                .andExpect(jsonPath("$.bookings[0].levelId").isEmpty())
                .andExpect(jsonPath("$.bookings[0].notes").isEmpty());
    }

    @Test
    void slotRefusalsComeBackAsProblemDetail() throws Exception {
        when(query.slot(SLOT_ID, TRAINER_ID)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND, ErrorCodes.SLOT_NOT_FOUND, "Assessment slot not found."));

        mvc.perform(trainer(get(DETAIL + SLOT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("slot_not_found"));
        mvc.perform(trainer(get(DETAIL + "abc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTrainersGetIn() throws Exception {
        for (String path : List.of(LIST, DETAIL + SLOT_ID)) {
            mvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("unauthorized"));
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("forbidden"));
        }
        verifyNoInteractions(query);
    }

    private MockHttpServletRequestBuilder trainer(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(TRAINER_ID, role)).value();
    }
}
