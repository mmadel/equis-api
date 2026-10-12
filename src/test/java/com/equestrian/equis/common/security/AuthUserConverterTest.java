package com.equestrian.equis.common.security;

import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AuthUserConverterTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TrainerRepository trainerRepository = mock(TrainerRepository.class);
    private final AuthUserConverter converter = new AuthUserConverter(trainerRepository);

    @Test
    void activeTrainerBecomesAuthUserWithTrainerRole() {
        when(trainerRepository.existsByIdAndStatus(ID, TrainerStatus.ACTIVE)).thenReturn(true);

        AbstractAuthenticationToken authentication = converter.convert(jwt(ID.toString(), "TRAINER"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthUser(ID, Role.TRAINER));
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TRAINER");
    }

    @Test
    void trainerWhoIsNotActiveIsRejected() {
        when(trainerRepository.existsByIdAndStatus(ID, TrainerStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> converter.convert(jwt(ID.toString(), "TRAINER")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void adminBecomesAuthUserWithoutATrainerLookup() {
        AbstractAuthenticationToken authentication = converter.convert(jwt(ID.toString(), "ADMIN"));

        assertThat(authentication.getPrincipal()).isEqualTo(new AuthUser(ID, Role.ADMIN));
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        verifyNoInteractions(trainerRepository);
    }

    @Test
    void rejectsSubjectThatIsNotAnId() {
        assertThatThrownBy(() -> converter.convert(jwt("not-a-uuid", "TRAINER")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void rejectsMissingOrUnknownRole() {
        assertThatThrownBy(() -> converter.convert(jwt(ID.toString(), null)))
                .isInstanceOf(InvalidBearerTokenException.class);
        assertThatThrownBy(() -> converter.convert(jwt(ID.toString(), "OWNER")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private static Jwt jwt(String subject, String role) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900));
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.build();
    }
}
