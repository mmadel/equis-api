package com.equestrian.equis.controller.riders;

import com.equestrian.equis.dto.riders.RiderAccount;
import com.equestrian.equis.service.riders.RiderAccountService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// W3's rider app account actions. SecurityConfig limits /admin/** to the admin.
@RestController
@RequestMapping("/admin/riders/{id}")
public class AdminRiderAccountController {
    private final RiderAccountService service;

    public AdminRiderAccountController(RiderAccountService service) {
        this.service = service;
    }

    // Send app invite, or send it again.
    @PostMapping("/invite")
    RiderAccount invite(@PathVariable UUID id) {
        return service.invite(id);
    }

    // Reset app password.
    @PostMapping("/reset-password")
    RiderAccount resetPassword(@PathVariable UUID id) {
        return service.resetPassword(id);
    }
}
