package com.equestrian.equis.service.sessions;

// sessions: rows inserted in the run; templates: templates processed without an error.
public record GenerationResult(int sessions, int templates) {
}
