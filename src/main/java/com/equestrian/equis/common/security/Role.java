package com.equestrian.equis.common.security;

import com.equestrian.equis.entity.accounts.SubjectType;

public enum Role {
    TRAINER,
    ADMIN,
    RIDER,
    // Runs equis itself: tokens carry no club and open only /platform/**.
    SUPER_ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public SubjectType toSubjectType() {
        return switch (this) {
            case TRAINER -> SubjectType.TRAINER;
            case ADMIN -> SubjectType.ADMIN;
            case RIDER -> SubjectType.RIDER;
            case SUPER_ADMIN -> SubjectType.SUPER_ADMIN;
        };
    }

    public static Role from(SubjectType subjectType) {
        return switch (subjectType) {
            case TRAINER -> TRAINER;
            case ADMIN -> ADMIN;
            case RIDER -> RIDER;
            case SUPER_ADMIN -> SUPER_ADMIN;
        };
    }
}
