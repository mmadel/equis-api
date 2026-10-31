package com.equestrian.equis.dto.evaluations;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// GET /admin/evaluations/follow-up: for one date, the level sessions that started and still have loose ends,
// grouped by the coach who seated the riders. Trainers by name, sessions by start, riders by name.
public record EvaluationFollowUp(LocalDate date, FollowUpTotals totals, List<FollowUpTrainer> trainers) {

    // trainers listed, distinct sessions not finished, riders still to evaluate.
    public record FollowUpTotals(int trainers, int notFinished, int ridersToEvaluate) {
    }

    public record FollowUpTrainer(UUID trainerId,
                                  String trainerName,
                                  boolean trainerActive,
                                  List<UnfinishedSession> notFinished,
                                  List<UnevaluatedSession> toEvaluate) {
    }

    // A started session nobody marked Finished; riders is how many seats this trainer gave in it.
    public record UnfinishedSession(UUID sessionId,
                                    @JsonFormat(pattern = "HH:mm") LocalTime start,
                                    @JsonFormat(pattern = "HH:mm") LocalTime end,
                                    int levelId,
                                    int riders) {
    }

    // A finished session with riders this trainer seated and has not evaluated yet.
    public record UnevaluatedSession(UUID sessionId,
                                     @JsonFormat(pattern = "HH:mm") LocalTime start,
                                     @JsonFormat(pattern = "HH:mm") LocalTime end,
                                     int levelId,
                                     List<RiderRef> riders) {
    }

    public record RiderRef(UUID riderId, String name) {
    }
}
