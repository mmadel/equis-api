package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.dto.accounts.InviteTrainerRequest;
import com.equestrian.equis.dto.accounts.TrainerList;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.service.accounts.TrainerAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    // The row actions answer with the updated row so W4 can redraw it.
    @PostMapping("/{id}/resend-invite")
    TrainerRow resendInvite(@PathVariable UUID id) {
        return service.resendInvite(id);
    }

    @PostMapping("/{id}/reset-password")
    TrainerRow resetPassword(@PathVariable UUID id) {
        return service.resetPassword(id);
    }

    @PostMapping("/{id}/disable")
    TrainerRow disable(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean confirm) {
        return service.disable(id, confirm);
    }

    @PostMapping("/{id}/enable")
    TrainerRow enable(@PathVariable UUID id) {
        return service.enable(id);
    }

    // 204: the trainer no longer exists.
    @DeleteMapping("/{id}/invite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelInvite(@PathVariable UUID id) {
        service.cancelInvite(id);
    }
}
