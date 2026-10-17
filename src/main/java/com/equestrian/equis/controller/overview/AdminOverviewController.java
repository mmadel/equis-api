package com.equestrian.equis.controller.overview;

import com.equestrian.equis.dto.overview.OverviewResponse;
import com.equestrian.equis.service.overview.AdminOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The admin Overview screen. SecurityConfig limits /admin/** to the admin.
@RestController
public class AdminOverviewController {
    private final AdminOverviewService service;

    public AdminOverviewController(AdminOverviewService service) {
        this.service = service;
    }

    @GetMapping("/admin/overview")
    OverviewResponse get() {
        return service.get();
    }
}
