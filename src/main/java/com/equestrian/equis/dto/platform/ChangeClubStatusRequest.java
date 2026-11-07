package com.equestrian.equis.dto.platform;

import com.equestrian.equis.entity.club.ClubStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Suspend ({ status: SUSPENDED, reason }, reason at least 5 characters once trimmed) or reactivate
// ({ status: ACTIVE }, any reason is ignored).
public record ChangeClubStatusRequest(@NotNull ClubStatus status, @Size(max = 500) String reason) {
}
