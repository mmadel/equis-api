package com.equestrian.equis.dto.sessions;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// The saved template and the warnings the admin should see next to it.
public record SaveResponse(SessionTemplateResponse template, List<Warning> warnings) {

    // overlaps is empty for warnings that are not about overlapping templates.
    public record Warning(String code, List<Overlap> overlaps) {
    }

    public record Overlap(UUID id,
                          @JsonFormat(pattern = "HH:mm") LocalTime start,
                          @JsonFormat(pattern = "HH:mm") LocalTime end) {
    }
}
