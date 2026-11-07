package com.equestrian.equis.controller.platform;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.platform.PlatformLoginRequest;
import com.equestrian.equis.service.platform.PlatformAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The super admin's sign-in is public; the rest of /platform needs a super admin token (SecurityConfig).
// Refresh uses the shared /auth/refresh.
@RestController
@RequestMapping("/platform/auth")
public class PlatformAuthController {
    private final PlatformAuthService auth;

    public PlatformAuthController(PlatformAuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    AuthTokens login(@Valid @RequestBody PlatformLoginRequest req) {
        return auth.login(req);
    }
}
