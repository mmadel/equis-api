package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.dto.assessment.AssessmentSlotsDto;
import com.equestrian.equis.service.assessment.AssessmentSlotQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// The admin's copy of the trainer slot list, for W2's New person panel. SecurityConfig limits /admin/** to the admin;
// /assessment/slots stays trainer-only.
@RestController
@RequestMapping("/admin/assessment/slots")
public class AdminAssessmentSlotController {
    private final AssessmentSlotQueryService query;

    public AdminAssessmentSlotController(AssessmentSlotQueryService query) {
        this.query = query;
    }

    // Same answer as GET /assessment/slots: without dates, the coming two weeks.
    @GetMapping
    AssessmentSlotsDto slots(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return query.slots(from, to);
    }
}
