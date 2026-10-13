package com.equestrian.equis.service.sessions;

import com.equestrian.equis.entity.sessions.SessionTemplate;

import java.util.List;

// The saved template and the warnings the admin should see next to it.
public record SaveResult(SessionTemplate template, List<Warning> warnings) {
    public SaveResult {
        warnings = List.copyOf(warnings);
    }
}
