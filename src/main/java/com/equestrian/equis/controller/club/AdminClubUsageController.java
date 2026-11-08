package com.equestrian.equis.controller.club;

import com.equestrian.equis.dto.club.ClubUsageView;
import com.equestrian.equis.service.club.PlanLimitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The club admin's usage against the plan. Admin only (the /admin/** rule in SecurityConfig).
@RestController
public class AdminClubUsageController {
    private final PlanLimitService limits;

    public AdminClubUsageController(PlanLimitService limits) {
        this.limits = limits;
    }

    @GetMapping("/admin/club/usage")
    ClubUsageView usage() {
        return limits.usage();
    }
}
