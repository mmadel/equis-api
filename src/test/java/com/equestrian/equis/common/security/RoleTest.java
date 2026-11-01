package com.equestrian.equis.common.security;

import com.equestrian.equis.entity.accounts.SubjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RoleTest {
    @Test
    void authorityPrefixesNameWithRole() {
        assertThat(Role.TRAINER.authority()).isEqualTo("ROLE_TRAINER");
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void mapsToSubjectType() {
        assertThat(Role.TRAINER.toSubjectType()).isEqualTo(SubjectType.TRAINER);
        assertThat(Role.ADMIN.toSubjectType()).isEqualTo(SubjectType.ADMIN);
    }

    // RIDER has no role until rider sign-in (next E15 ticket); see refusesRiderUntilRiderSignIn.
    @ParameterizedTest
    @EnumSource(value = SubjectType.class, mode = EnumSource.Mode.EXCLUDE, names = "RIDER")
    void roundTripsEverySubjectType(SubjectType subjectType) {
        assertThat(Role.from(subjectType).toSubjectType()).isEqualTo(subjectType);
    }

    @Test
    void refusesRiderUntilRiderSignIn() {
        assertThatThrownBy(() -> Role.from(SubjectType.RIDER)).isInstanceOf(IllegalStateException.class);
    }
}
