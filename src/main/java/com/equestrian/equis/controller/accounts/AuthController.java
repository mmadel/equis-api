package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.accounts.InviteAcceptRequest;
import com.equestrian.equis.dto.accounts.LoginRequest;
import com.equestrian.equis.dto.accounts.RefreshRequest;
import com.equestrian.equis.service.accounts.AuthService;
import com.equestrian.equis.service.accounts.AuthTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Everything under /auth is public; see SecurityConfig.
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthTokenService tokens;

    public AuthController(AuthService auth, AuthTokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    AuthTokens login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/refresh")
    AuthTokens refresh(@Valid @RequestBody RefreshRequest req) {
        return tokens.rotate(req.refreshToken());
    }

    @PostMapping("/invite/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void accept(@Valid @RequestBody InviteAcceptRequest req) {
        auth.acceptInvite(req);
    }
}
