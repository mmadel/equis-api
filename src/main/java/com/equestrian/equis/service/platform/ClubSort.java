package com.equestrian.equis.service.platform;

import java.util.Arrays;
import java.util.Optional;

// The columns P1 can sort by, with the name the API takes (?sort=endsAt).
public enum ClubSort {
    NAME("name"),
    CODE("code"),
    ENDS_AT("endsAt");

    private final String param;

    ClubSort(String param) {
        this.param = param;
    }

    public String param() {
        return param;
    }

    public static Optional<ClubSort> fromParam(String value) {
        return Arrays.stream(values()).filter(s -> s.param.equals(value)).findFirst();
    }
}
