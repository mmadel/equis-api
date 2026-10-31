package com.equestrian.equis.controller.evaluations;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.evaluations.PendingEvaluations;
import com.equestrian.equis.service.evaluations.PendingEvaluationQuery;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Everything under /me is for the trainer role; see SecurityConfig.
@RestController
@RequestMapping("/me/evaluations")
public class MyEvaluationsController {
    private final PendingEvaluationQuery pending;

    public MyEvaluationsController(PendingEvaluationQuery pending) {
        this.pending = pending;
    }

    @GetMapping("/pending")
    PendingEvaluations pending(@AuthenticationPrincipal AuthUser user) {
        return pending.pending(user.id());
    }
}
