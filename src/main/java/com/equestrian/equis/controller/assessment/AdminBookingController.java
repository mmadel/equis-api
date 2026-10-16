package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.dto.assessment.AdminBookingList;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.ChangeBookingRequest;
import com.equestrian.equis.service.assessment.AdminBookingChangeService;
import com.equestrian.equis.service.assessment.AdminBookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// W2, the admin Assessments screen. SecurityConfig limits /admin/** to the admin.
@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {
    private final AdminBookingService service;
    private final AdminBookingChangeService changes;

    public AdminBookingController(AdminBookingService service, AdminBookingChangeService changes) {
        this.service = service;
        this.changes = changes;
    }

    @GetMapping
    AdminBookingList list(@RequestParam(required = false) String status, @RequestParam(required = false) String q) {
        return service.list(status, q);
    }

    // Reschedule, rebook or change the assessor; answers with the row W2 shows from now on.
    @PatchMapping("/{id}")
    AdminBookingRow change(@PathVariable UUID id, @RequestBody ChangeBookingRequest req) {
        return changes.change(id, req);
    }
}
