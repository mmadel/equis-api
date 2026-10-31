package com.equestrian.equis.dto.sessions;

import com.equestrian.equis.dto.evaluations.SeatEvaluation;
import com.equestrian.equis.dto.levels.LevelRef;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// status is scheduled, cancelled, finished or done (finished and every rider evaluated). canAssign: riders can be added now (the session's day, not finished
// or cancelled); canFinish: the caller can mark it finished now. readOnly is kept for older apps and is !canAssign.
// riders are the caller's riders of the level with a session left that day, empty unless canAssign; mySeats are
// the ones the caller seated here. sessionsLeft is counted on the session's date. A seat's evaluation is null until
// saved; canEvaluate is true while the session is finished and the seat has none.
public record SessionDetailResponse(UUID id,
                                    LevelRef level,
                                    LocalDate date,
                                    @JsonFormat(pattern = "HH:mm") LocalTime start,
                                    @JsonFormat(pattern = "HH:mm") LocalTime end,
                                    int capacity,
                                    String status,
                                    @JsonInclude(JsonInclude.Include.ALWAYS) Instant finishedAt,
                                    boolean canAssign,
                                    boolean canFinish,
                                    boolean readOnly,
                                    int otherCoachSeats,
                                    List<MySeat> mySeats,
                                    List<RiderOption> riders) {

    public record MySeat(UUID riderId, String name, String initials, int sessionsLeft,
                         @JsonInclude(JsonInclude.Include.ALWAYS) SeatEvaluation evaluation,
                         boolean canEvaluate) {
    }

    public record RiderOption(UUID id, String name, String initials, boolean inThisSession, int sessionsLeft) {
    }
}
