package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.sessions.DeleteResponse;
import com.equestrian.equis.dto.sessions.SaveResponse;
import com.equestrian.equis.dto.sessions.SessionTemplateCreateRequest;
import com.equestrian.equis.dto.sessions.SessionTemplateResponse;
import com.equestrian.equis.dto.sessions.SessionTemplateUpdateRequest;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.service.sessions.BookedSeats;
import com.equestrian.equis.service.sessions.SaveResult;
import com.equestrian.equis.service.sessions.SessionTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Everything under /admin is for the admin role; see SecurityConfig.
@RestController
@RequestMapping("/admin/session-templates")
public class SessionTemplateController {
    private static final String ASSESSMENT = "assessment";
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 6;

    private final SessionTemplateService service;
    private final BookedSeats bookedSeats;

    public SessionTemplateController(SessionTemplateService service, BookedSeats bookedSeats) {
        this.service = service;
        this.bookedSeats = bookedSeats;
    }

    // level is "assessment" or a level number; one W1 tab per value.
    @GetMapping
    List<SessionTemplateResponse> list(@RequestParam(required = false) String level) {
        List<SessionTemplate> templates = ASSESSMENT.equals(level)
                ? service.listAssessment()
                : service.listByLevel(levelId(level));
        return templates.stream().map(this::response).toList();
    }

    @PostMapping
    ResponseEntity<SaveResponse> create(@Valid @RequestBody SessionTemplateCreateRequest req,
                                        HttpServletRequest http) {
        SaveResult result = service.create(SessionTemplateMapper.toInput(req));
        // The context path (/api) is not part of the mapping, so it is added here.
        URI location = URI.create(http.getContextPath() + "/admin/session-templates/" + result.template().getId());
        return ResponseEntity.created(location).body(response(result));
    }

    @PutMapping("/{id}")
    SaveResponse update(@PathVariable UUID id, @Valid @RequestBody SessionTemplateUpdateRequest req) {
        return response(service.update(id, SessionTemplateMapper.toInput(req)));
    }

    @DeleteMapping("/{id}")
    DeleteResponse delete(@PathVariable UUID id) {
        return SessionTemplateMapper.toResponse(service.delete(id));
    }

    private SaveResponse response(SaveResult result) {
        return SessionTemplateMapper.toResponse(response(result.template()), result.warnings());
    }

    // Two count calls per template; a tab has only a few templates.
    private SessionTemplateResponse response(SessionTemplate t) {
        return SessionTemplateMapper.toResponse(t,
                bookedSeats.maxBookedInUpcoming(t.getId()), bookedSeats.bookingsInUpcoming(t.getId()));
    }

    private static short levelId(String level) {
        try {
            int id = Integer.parseInt(level);
            if (id >= MIN_LEVEL && id <= MAX_LEVEL) {
                return (short) id;
            }
        } catch (NumberFormatException e) {
            // Falls through to the error below; a missing level lands here too.
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED,
                "Some fields are not valid.",
                Map.of("fieldErrors", List.of(new ApiFieldError("level",
                        "Level must be \"" + ASSESSMENT + "\" or a number from " + MIN_LEVEL + " to " + MAX_LEVEL
                                + "."))));
    }
}
