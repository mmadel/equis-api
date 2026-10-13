package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.sessions.AssignmentsRequest;
import com.equestrian.equis.dto.sessions.SessionDetailResponse;
import com.equestrian.equis.service.sessions.AssignmentService;
import com.equestrian.equis.service.sessions.SessionDetailService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Everything under /me is for the trainer role; see SecurityConfig.
@RestController
@RequestMapping("/me/sessions")
public class MySessionsController {
    private final SessionDetailService details;
    private final AssignmentService assignments;

    public MySessionsController(SessionDetailService details, AssignmentService assignments) {
        this.details = details;
        this.assignments = assignments;
    }

    @GetMapping("/{id}")
    SessionDetailResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return SessionDetailMapper.toResponse(details.get(user.id(), id), user.id());
    }

    // Answers with the session as saved, so the app redraws from the server's state.
    @PutMapping("/{id}/assignments")
    SessionDetailResponse replaceAssignments(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                             @Valid @RequestBody AssignmentsRequest req) {
        assignments.replaceMine(user.id(), id, req.riderIds());
        return SessionDetailMapper.toResponse(details.get(user.id(), id), user.id());
    }
}
