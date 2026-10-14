package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.assessment.RecordResultRequest;
import com.equestrian.equis.dto.assessment.ResultRecorded;
import com.equestrian.equis.service.assessment.RecordResultService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Bean Validation covers the shape; attended <-> levelId and every other rule are checked in the service.
@RestController
public class BookingController {
    private final RecordResultService results;

    public BookingController(RecordResultService results) {
        this.results = results;
    }

    @PostMapping("/me/bookings/{id}/result")
    ResultRecorded record(@PathVariable UUID id, @AuthenticationPrincipal AuthUser me,
                          @Valid @RequestBody RecordResultRequest req) {
        return results.record(id, me, req);
    }
}
