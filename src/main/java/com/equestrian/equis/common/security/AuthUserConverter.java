package com.equestrian.equis.common.security;

import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;
import java.util.UUID;

// Runs after the decoder has checked the signature, issuer and expiry.
// Registered in SecurityConfig, not as a @Component: web test slices pick up every Converter they find.
public class AuthUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    public static final String ROLE_CLAIM = "role";

    private final TrainerRepository trainerRepository;

    public AuthUserConverter(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthUser user = new AuthUser(subject(jwt), role(jwt));

        // Checked on every request so a disabled trainer is stopped at once, not when the token expires.
        if (user.role() == Role.TRAINER
                && !trainerRepository.existsByIdAndStatus(user.id(), TrainerStatus.ACTIVE)) {
            throw new DisabledException("Trainer is not active");
        }
        return new AuthUserAuthentication(user, jwt);
    }

    private static UUID subject(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("Token has no valid subject", e);
        }
    }

    private static Role role(Jwt jwt) {
        try {
            return Role.valueOf(jwt.getClaimAsString(ROLE_CLAIM));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("Token has no valid role", e);
        }
    }

    private static final class AuthUserAuthentication extends AbstractAuthenticationToken {
        private final AuthUser user;
        private final Jwt jwt;

        private AuthUserAuthentication(AuthUser user, Jwt jwt) {
            super(List.of(new SimpleGrantedAuthority(user.role().authority())));
            this.user = user;
            this.jwt = jwt;
            setAuthenticated(true);
        }

        @Override
        public AuthUser getPrincipal() {
            return user;
        }

        @Override
        public Jwt getCredentials() {
            return jwt;
        }

        @Override
        public String getName() {
            return user.id().toString();
        }
    }
}
