package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.dto.accounts.InviteTrainerRequest;
import com.equestrian.equis.dto.accounts.TrainerList;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.service.accounts.TrainerAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// W4, the admin Trainers screen. SecurityConfig limits /admin/** to the admin.
@RestController
@RequestMapping("/admin/trainers")
public class AdminTrainerController {
    private final TrainerAdminService service;

    public AdminTrainerController(TrainerAdminService service) {
        this.service = service;
    }

    @GetMapping
    TrainerList list() {
        return service.list();
    }

    // 201 with the new row and no Location header: there is no endpoint to read one trainer.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TrainerRow invite(@Valid @RequestBody InviteTrainerRequest req) {
        return service.invite(req);
    }
}
