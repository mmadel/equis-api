package com.equestrian.equis.common.security;

import com.equestrian.equis.entity.accounts.SubjectType;

public enum Role {
    TRAINER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public SubjectType toSubjectType() {
        return switch (this) {
            case TRAINER -> SubjectType.TRAINER;
            case ADMIN -> SubjectType.ADMIN;
        };
    }

    public static Role from(SubjectType subjectType) {
        return switch (subjectType) {
            case TRAINER -> TRAINER;
            case ADMIN -> ADMIN;
            // Rider sign-in comes with the next E15 ticket; until then no rider token is ever issued.
            case RIDER -> throw new IllegalStateException("Riders can't sign in yet");
        };
    }
}
