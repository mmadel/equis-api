package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.dto.assessment.AdminBookingList;
import com.equestrian.equis.service.assessment.AdminBookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// W2, the admin Assessments screen. SecurityConfig limits /admin/** to the admin.
@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {
    private final AdminBookingService service;

    public AdminBookingController(AdminBookingService service) {
        this.service = service;
    }

    @GetMapping
    AdminBookingList list(@RequestParam(required = false) String status, @RequestParam(required = false) String q) {
        return service.list(status, q);
    }
}
