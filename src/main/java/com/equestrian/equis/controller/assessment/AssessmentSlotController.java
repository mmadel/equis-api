package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.assessment.AssessmentSlotsDto;
import com.equestrian.equis.dto.assessment.SlotDetailDto;
import com.equestrian.equis.service.assessment.AssessmentSlotQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

// /assessment and /me are for the trainer role; see SecurityConfig.
@RestController
public class AssessmentSlotController {
    private final AssessmentSlotQueryService query;

    public AssessmentSlotController(AssessmentSlotQueryService query) {
        this.query = query;
    }

    // The same list for every trainer, so no caller. Without dates, the coming two weeks.
    @GetMapping("/assessment/slots")
    AssessmentSlotsDto slots(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return query.slots(from, to);
    }

    @GetMapping("/me/assessment/slots/{id}")
    SlotDetailDto slot(@PathVariable UUID id, @AuthenticationPrincipal AuthUser me) {
        return query.slot(id, me.id());
    }
}
