package com.equestrian.equis.controller.platform;

import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.dto.platform.AddSubscriptionRequest;
import com.equestrian.equis.dto.platform.PlanView;
import com.equestrian.equis.dto.platform.SubscriptionResult;
import com.equestrian.equis.dto.platform.SubscriptionView;
import com.equestrian.equis.dto.platform.VoidSubscriptionRequest;
import com.equestrian.equis.service.platform.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// P3, a club's subscriptions. SecurityConfig limits /platform/** to the super admin; requests run in root mode.
@RestController
@RequestMapping("/platform")
public class PlatformSubscriptionController {
    private final SubscriptionService subscriptions;

    public PlatformSubscriptionController(SubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @GetMapping("/plans")
    List<PlanView> plans() {
        return subscriptions.plans();
    }

    @GetMapping("/clubs/{id}/subscriptions")
    List<SubscriptionView> history(@PathVariable UUID id) {
        return subscriptions.history(id);
    }

    @PostMapping("/clubs/{id}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionResult add(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                           @Valid @RequestBody AddSubscriptionRequest req) {
        return subscriptions.add(id, req, me.id());
    }

    @PostMapping("/subscriptions/{id}/void")
    SubscriptionResult voidSubscription(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                        @Valid @RequestBody VoidSubscriptionRequest req) {
        return subscriptions.voidSubscription(id, req.reason(), me.id());
    }
}
