package com.equestrian.equis.dto.sessions;

import com.equestrian.equis.dto.levels.LevelRef;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// readOnly means the session is cancelled or has already started.
// riders are all of the caller's riders in the session's level; mySeats are the ones the caller seated here.
public record SessionDetailResponse(UUID id,
                                    LevelRef level,
                                    LocalDate date,
                                    @JsonFormat(pattern = "HH:mm") LocalTime start,
                                    @JsonFormat(pattern = "HH:mm") LocalTime end,
                                    int capacity,
                                    boolean readOnly,
                                    int otherCoachSeats,
                                    List<MySeat> mySeats,
                                    List<RiderOption> riders) {

    public record MySeat(UUID riderId, String name, String initials) {
    }

    public record RiderOption(UUID id, String name, String initials, boolean inThisSession) {
    }
}
