package com.equestrian.equis.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E17 · case 1 and the rules of TenantContext itself. No Spring, no database.
class TenantContextTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @BeforeEach
    @AfterEach
    void noClub() {
        TenantContext.clear();
    }

    // Case 1
    @Test
    void anInnerRunAsSeesItsClubAndTheOuterClubComesBack() {
        UUID[] seen = new UUID[3];

        TenantContext.runAs(B, () -> {
            seen[0] = TenantContext.current();
            TenantContext.runAs(A, () -> seen[1] = TenantContext.current());
            seen[2] = TenantContext.current();
            return null;
        });

        assertThat(seen).containsExactly(B, A, B);
        assertThat(TenantContext.current()).isNull();
    }

    @Test
    void runAsPutsThePreviousClubBackWhenTheWorkThrows() {
        TenantContext.set(A);

        assertThatThrownBy(() -> TenantContext.runAs(B, () -> {
            throw new IllegalStateException("boom");
        })).hasMessage("boom");

        assertThat(TenantContext.current()).isEqualTo(A);
    }

    @Test
    void rootModeHasNoClubAndEndsWithTheRun() {
        TenantContext.set(A);

        TenantContext.runAsRoot(() -> {
            assertThat(TenantContext.isRoot()).isTrue();
            assertThat(TenantContext.current()).isNull();
            return null;
        });

        assertThat(TenantContext.isRoot()).isFalse();
        assertThat(TenantContext.current()).isEqualTo(A);
    }

    @Test
    void runAsWithNoClubRunsWithoutOne() {
        TenantContext.set(A);

        TenantContext.runAs(null, () -> {
            assertThat(TenantContext.current()).isNull();
            assertThat(TenantContext.isRoot()).isFalse();
            return null;
        });

        assertThat(TenantContext.current()).isEqualTo(A);
    }

    @Test
    void theRootIdIsNotAClub() {
        assertThatThrownBy(() -> TenantContext.set(TenantContext.ROOT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TenantContext.set(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theHibernateResolverUsesRootWhenThereIsNoClub() {
        TenantIdentifierResolver resolver = new TenantIdentifierResolver();

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(TenantContext.ROOT);
        TenantContext.runAs(A, () -> assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(A));
        assertThat(resolver.isRoot(TenantContext.ROOT)).isTrue();
        assertThat(resolver.isRoot(A)).isFalse();
    }
}
