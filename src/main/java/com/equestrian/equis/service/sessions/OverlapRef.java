package com.equestrian.equis.service.sessions;

import java.time.LocalTime;
import java.util.UUID;

// An existing template the new times collide with.
public record OverlapRef(UUID id, LocalTime start, LocalTime end) {
}
