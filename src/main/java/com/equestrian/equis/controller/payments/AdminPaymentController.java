package com.equestrian.equis.controller.payments;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.payments.PaymentResult;
import com.equestrian.equis.dto.payments.RecordPaymentRequest;
import com.equestrian.equis.service.payments.AdminPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// W3's Record payment panel. SecurityConfig limits /admin/** to the admin.
@RestController
public class AdminPaymentController {
    private final AdminPaymentService service;

    public AdminPaymentController(AdminPaymentService service) {
        this.service = service;
    }

    // Answers with the new package and the rider's balance, so W3 updates in place.
    @PostMapping("/admin/riders/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResult record(@PathVariable UUID id, @Valid @RequestBody RecordPaymentRequest req,
                         @AuthenticationPrincipal AuthUser me) {
        return service.record(id, req, me.id());
    }
}
