package com.equestrian.equis.controller.sessions;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.evaluations.EvaluationRequest;
import com.equestrian.equis.dto.sessions.AssignmentsRequest;
import com.equestrian.equis.dto.sessions.SessionDetailResponse;
import com.equestrian.equis.service.evaluations.EvaluationService;
import com.equestrian.equis.service.sessions.AssignmentService;
import com.equestrian.equis.service.sessions.FinishSessionService;
import com.equestrian.equis.service.sessions.SessionDetailService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final FinishSessionService finisher;
    private final EvaluationService evaluator;

    public MySessionsController(SessionDetailService details, AssignmentService assignments,
                                FinishSessionService finisher, EvaluationService evaluator) {
        this.details = details;
        this.assignments = assignments;
        this.finisher = finisher;
        this.evaluator = evaluator;
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

    // No body. Answers with the finished session and each seated rider's new sessions left.
    @PostMapping("/{id}/finish")
    SessionDetailResponse finish(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        finisher.finish(user.id(), id);
        return SessionDetailMapper.toResponse(details.get(user.id(), id), user.id());
    }

    // One rider at a time. Answers with the session as saved, done once every rider of every coach is evaluated.
    @PostMapping("/{id}/evaluations")
    SessionDetailResponse evaluate(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                   @Valid @RequestBody EvaluationRequest req) {
        evaluator.evaluate(user.id(), id, req);
        return SessionDetailMapper.toResponse(details.get(user.id(), id), user.id());
    }
}
