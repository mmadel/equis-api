package com.equestrian.equis.controller.evaluations;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.evaluations.EvaluationFollowUp;
import com.equestrian.equis.dto.evaluations.RiderEvaluations;
import com.equestrian.equis.service.evaluations.EvaluationFollowUpQuery;
import com.equestrian.equis.service.evaluations.EvaluationHistoryQuery;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// W3's Evaluations section and the Follow-up screen. SecurityConfig limits /admin/** to the admin.
@RestController
public class AdminEvaluationController {
    private final EvaluationHistoryQuery history;
    private final EvaluationFollowUpQuery followUp;

    public AdminEvaluationController(EvaluationHistoryQuery history, EvaluationFollowUpQuery followUp) {
        this.history = history;
        this.followUp = followUp;
    }

    @GetMapping("/admin/riders/{id}/evaluations")
    RiderEvaluations evaluations(@PathVariable UUID id) {
        return history.forAdmin(id);
    }

    // date is YYYY-MM-DD; without it, today in the school's time zone.
    @GetMapping("/admin/evaluations/follow-up")
    EvaluationFollowUp followUp(@RequestParam(required = false) String date) {
        return followUp.followUp(date == null || date.isBlank() ? followUp.today() : parseDate(date));
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                    Map.of("fieldErrors", List.of(new ApiFieldError("date", ErrorCodes.INVALID_DATE))));
        }
    }
}
