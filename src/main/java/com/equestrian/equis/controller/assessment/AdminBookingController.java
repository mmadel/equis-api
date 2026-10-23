package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.dto.assessment.AdminBookingList;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.CancelBookingRequest;
import com.equestrian.equis.dto.assessment.ChangeBookingRequest;
import com.equestrian.equis.service.assessment.AdminBookingCancelService;
import com.equestrian.equis.service.assessment.AdminBookingChangeService;
import com.equestrian.equis.service.assessment.AdminBookingService;
import com.equestrian.equis.service.assessment.BookingReminderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final BookingReminderService reminders;
    private final AdminBookingCancelService cancels;

    public AdminBookingController(AdminBookingService service, AdminBookingChangeService changes,
                                  BookingReminderService reminders, AdminBookingCancelService cancels) {
        this.service = service;
        this.changes = changes;
        this.reminders = reminders;
        this.cancels = cancels;
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

    // Cancel an upcoming assessment with a reason; answers with the row W2 shows from now on.
    @PostMapping("/{id}/cancel")
    AdminBookingRow cancel(@PathVariable UUID id, @Valid @RequestBody CancelBookingRequest req) {
        return cancels.cancel(id, req);
    }

    // Remind trainer: an in-app notice to the assessor of a booking waiting for its result.
    @PostMapping("/{id}/remind")
    AdminBookingRow remind(@PathVariable UUID id) {
        return reminders.remind(id);
    }
}
