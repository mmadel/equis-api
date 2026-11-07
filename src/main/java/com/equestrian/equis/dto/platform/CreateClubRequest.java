package com.equestrian.equis.dto.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// P2, a new club: its name, its sign-in code and its first admin. The code is trimmed and lowercased, then checked
// against the club code rule (ClubOnboardingService).
public record CreateClubRequest(@NotBlank @Size(max = 120) String name,
                                @NotBlank String code,
                                @NotNull @Valid ClubAdminInput admin) {
}
