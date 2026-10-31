package com.equestrian.equis.controller.evaluations;

import com.equestrian.equis.dto.evaluations.RiderEvaluations;
import com.equestrian.equis.service.evaluations.EvaluationHistoryQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// W3's Evaluations section. SecurityConfig limits /admin/** to the admin.
@RestController
public class AdminEvaluationController {
    private final EvaluationHistoryQuery history;

    public AdminEvaluationController(EvaluationHistoryQuery history) {
        this.history = history;
    }

    @GetMapping("/admin/riders/{id}/evaluations")
    RiderEvaluations evaluations(@PathVariable UUID id) {
        return history.forAdmin(id);
    }
}
