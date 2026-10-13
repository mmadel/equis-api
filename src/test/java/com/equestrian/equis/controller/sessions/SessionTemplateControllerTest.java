package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.config.ClockConfig;
import com.equestrian.equis.common.config.JwtConfig;
import com.equestrian.equis.common.config.SecurityBeans;
import com.equestrian.equis.common.config.SecurityConfig;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.JwtService;
import com.equestrian.equis.service.sessions.BookedSeats;
import com.equestrian.equis.service.sessions.DeleteImpact;
import com.equestrian.equis.service.sessions.OverlapRef;
import com.equestrian.equis.service.sessions.SaveResult;
import com.equestrian.equis.service.sessions.SessionTemplateInput;
import com.equestrian.equis.service.sessions.SessionTemplateService;
import com.equestrian.equis.service.sessions.Warning;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionTemplateController.class)
@Import({SecurityConfig.class, SecurityBeans.class, ClockConfig.class, JwtConfig.class, JwtService.class})
public class SessionTemplateControllerTest {
    private static final String PATH = "/admin/session-templates";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 10, 1);
    private static final SessionTemplateInput INPUT = new SessionTemplateInput(2, DayOfWeek.MONDAY,
            LocalTime.of(8, 30), LocalTime.of(9, 30), 6, VALID_FROM);
    private static final String BODY = """
            {"levelId": 2, "dayOfWeek": "MONDAY", "start": "08:30", "end": "09:30", "capacity": 6,
             "validFrom": "2026-10-01"}
            """;

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @MockitoBean
    SessionTemplateService service;
    @MockitoBean
    BookedSeats bookedSeats;
    @MockitoBean
    TrainerRepository trainerRepository;

    @Test
    void listsTheTemplatesOfALevelWithTheirCounts() throws Exception {
        when(service.listByLevel((short) 2)).thenReturn(List.of(template(2)));
        when(bookedSeats.maxBookedInUpcoming(TEMPLATE_ID)).thenReturn(4);
        when(bookedSeats.bookingsInUpcoming(TEMPLATE_ID)).thenReturn(17);

        mvc.perform(admin(get(PATH).param("level", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(TEMPLATE_ID.toString()))
                .andExpect(jsonPath("$[0].levelId").value(2))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].start").value("08:30"))
                .andExpect(jsonPath("$[0].end").value("09:30"))
                .andExpect(jsonPath("$[0].capacity").value(6))
                .andExpect(jsonPath("$[0].validFrom").value("2026-10-01"))
                .andExpect(jsonPath("$[0].maxBookedInUpcoming").value(4))
                .andExpect(jsonPath("$[0].bookingsInUpcoming").value(17));
    }

    @Test
    void listsTheAssessmentSlots() throws Exception {
        when(service.listAssessment()).thenReturn(List.of(template(null)));

        mvc.perform(admin(get(PATH).param("level", "assessment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TEMPLATE_ID.toString()))
                .andExpect(jsonPath("$[0].levelId").doesNotExist());
    }

    @Test
    void anyOtherLevelIsRefused() throws Exception {
        for (String level : List.of("0", "7", "abc", "Assessment", "")) {
            mvc.perform(admin(get(PATH).param("level", level)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("validation_failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("level"));
        }
        mvc.perform(admin(get(PATH)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("level"));
        verifyNoInteractions(service);
    }

    @Test
    void createReturns201WithLocationTemplateAndWarnings() throws Exception {
        when(service.create(INPUT)).thenReturn(new SaveResult(template(2), List.of(Warning.overlap(List.of(
                new OverlapRef(OTHER_ID, LocalTime.of(9, 0), LocalTime.of(10, 0)))))));

        mvc.perform(admin(post("/api" + PATH).contextPath("/api"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/admin/session-templates/" + TEMPLATE_ID))
                .andExpect(jsonPath("$.template.id").value(TEMPLATE_ID.toString()))
                .andExpect(jsonPath("$.template.start").value("08:30"))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.warnings[0].code").value("overlap"))
                .andExpect(jsonPath("$.warnings[0].overlaps[0].id").value(OTHER_ID.toString()))
                .andExpect(jsonPath("$.warnings[0].overlaps[0].start").value("09:00"))
                .andExpect(jsonPath("$.warnings[0].overlaps[0].end").value("10:00"));
    }

    @Test
    void createAcceptsAnAssessmentSlotWithoutALevel() throws Exception {
        SessionTemplateInput assessment = new SessionTemplateInput(null, DayOfWeek.MONDAY,
                LocalTime.of(8, 30), LocalTime.of(9, 30), 6, VALID_FROM);
        when(service.create(assessment)).thenReturn(new SaveResult(template(null), List.of()));

        mvc.perform(admin(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("""
                        {"dayOfWeek": "MONDAY", "start": "08:30", "end": "09:30", "capacity": 6,
                         "validFrom": "2026-10-01"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings.length()").value(0));
    }

    @Test
    void createRefusesMissingAndOutOfRangeFields() throws Exception {
        mvc.perform(admin(post(PATH)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(5));

        for (String field : List.of("\"levelId\": 0", "\"levelId\": 7", "\"capacity\": 0", "\"capacity\": 13")) {
            String name = field.substring(1, field.lastIndexOf('"'));
            String body = "{" + field + ", \"dayOfWeek\": \"MONDAY\", \"start\": \"08:30\", \"end\": \"09:30\", "
                    + (name.equals("capacity") ? "" : "\"capacity\": 6, ") + "\"validFrom\": \"2026-10-01\"}";
            mvc.perform(admin(post(PATH)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value(name));
        }
        verifyNoInteractions(service);
    }

    @Test
    void updateReturnsTheSavedTemplate() throws Exception {
        when(service.update(TEMPLATE_ID, INPUT)).thenReturn(new SaveResult(template(2), List.of()));

        mvc.perform(admin(put(PATH + "/" + TEMPLATE_ID)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.id").value(TEMPLATE_ID.toString()))
                .andExpect(jsonPath("$.warnings.length()").value(0));
    }

    @Test
    void updateRefusesAnInvalidBody() throws Exception {
        mvc.perform(admin(put(PATH + "/" + TEMPLATE_ID)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        verifyNoInteractions(service);
    }

    @Test
    void serviceErrorsComeBackAsProblemDetail() throws Exception {
        when(service.update(TEMPLATE_ID, INPUT)).thenThrow(new ApiException(HttpStatus.NOT_FOUND,
                ErrorCodes.NOT_FOUND, "This session template does not exist."));

        mvc.perform(admin(put(PATH + "/" + TEMPLATE_ID)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void deleteReturnsTheImpactCounts() throws Exception {
        when(service.delete(TEMPLATE_ID)).thenReturn(new DeleteImpact(8, 17));

        mvc.perform(admin(delete(PATH + "/" + TEMPLATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledSessions").value(8))
                .andExpect(jsonPath("$.cancelledBookings").value(17));
    }

    @Test
    void onlyTheAdminGetsIn() throws Exception {
        when(trainerRepository.existsByIdAndStatus(USER_ID, TrainerStatus.ACTIVE)).thenReturn(true);

        mvc.perform(get(PATH).param("level", "2")).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).param("level", "2").header(HttpHeaders.AUTHORIZATION, bearer(Role.TRAINER)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(new AuthUser(USER_ID, role)).value();
    }

    // A null levelId makes an assessment slot.
    private static SessionTemplate template(Integer levelId) {
        SessionTemplate template = new SessionTemplate();
        template.setId(TEMPLATE_ID);
        if (levelId != null) {
            Level level = new Level();
            level.setId(levelId.shortValue());
            template.setLevel(level);
        }
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(8, 30));
        template.setEndTime(LocalTime.of(9, 30));
        template.setCapacity(6);
        template.setValidFrom(VALID_FROM);
        return template;
    }
}
