package com.equestrian.equis.common.security;

import com.equestrian.equis.common.tenant.TenantContext;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.riders.RiderAccountStatus;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
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
    private final RiderRepository riderRepository = mock(RiderRepository.class);
    private final AuthUserConverter converter = new AuthUserConverter(trainerRepository, riderRepository);

    @Test
    void activeTrainerBecomesAuthUserWithTrainerRole() {
        when(trainerRepository.existsByIdAndStatus(ID, TrainerStatus.ACTIVE)).thenReturn(true);

        AbstractAuthenticationToken authentication = converter.convert(jwt(ID.toString(), "TRAINER"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthUser(ID, Role.TRAINER, Club.DEFAULT_ID));
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TRAINER");
    }

    @Test
    void activeRiderBecomesAuthUserWithRiderRole() {
        when(riderRepository.existsByIdAndAccountStatus(ID, RiderAccountStatus.ACTIVE)).thenReturn(true);

        AbstractAuthenticationToken authentication = converter.convert(jwt(ID.toString(), "RIDER"));

        assertThat(authentication.getPrincipal()).isEqualTo(new AuthUser(ID, Role.RIDER, Club.DEFAULT_ID));
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RIDER");
    }

    @Test
    void riderWhoIsNotActiveIsRejected() {
        when(riderRepository.existsByIdAndAccountStatus(ID, RiderAccountStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> converter.convert(jwt(ID.toString(), "RIDER")))
                .isInstanceOf(DisabledException.class);
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

        assertThat(authentication.getPrincipal()).isEqualTo(new AuthUser(ID, Role.ADMIN, Club.DEFAULT_ID));
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

    // A token issued before clubs existed: the app refreshes it (the refresh row knows the club).
    @Test
    void rejectsMissingOrInvalidClub() {
        assertThatThrownBy(() -> converter.convert(jwt(ID.toString(), "ADMIN", null)))
                .isInstanceOf(InvalidBearerTokenException.class);
        assertThatThrownBy(() -> converter.convert(jwt(ID.toString(), "ADMIN", "school")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    // The converter runs before TenantFilter, so the status lookup must run in the token's club.
    @Test
    void checksTheTrainerInTheTokensClubAndPutsTheOuterClubBack() {
        UUID cairo = UUID.randomUUID();
        when(trainerRepository.existsByIdAndStatus(ID, TrainerStatus.ACTIVE)).thenAnswer(call -> {
            assertThat(TenantContext.current()).isEqualTo(cairo);
            return true;
        });

        AbstractAuthenticationToken authentication = converter.convert(jwt(ID.toString(), "TRAINER", cairo.toString()));

        assertThat(((AuthUser) authentication.getPrincipal()).clubId()).isEqualTo(cairo);
        assertThat(TenantContext.current()).isEqualTo(Club.DEFAULT_ID);
    }

    private static Jwt jwt(String subject, String role) {
        return jwt(subject, role, Club.DEFAULT_ID.toString());
    }

    private static Jwt jwt(String subject, String role, String club) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900));
        if (role != null) {
            builder.claim("role", role);
        }
        if (club != null) {
            builder.claim("cid", club);
        }
        return builder.build();
    }
}
