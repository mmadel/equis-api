package com.equestrian.equis.dto.sessions;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

// The caller's whole list for the session; an empty list frees all of their seats.
public record AssignmentsRequest(@NotNull List<@NotNull UUID> riderIds) {
}
