package com.equestrian.equis.service.sessions;

import com.equestrian.equis.entity.evaluations.SessionEvaluation;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// One level session as a trainer sees it: seats are every trainer's assignments; eligibleRiders are the trainer's
// riders of the session's level with a session left that day, by name, and empty unless canAssign.
// sessionsLeft holds, on the session date, every eligible rider and every rider the trainer seated.
// evaluations are the session's saved evaluations by rider id, of every coach; empty unless finished or done.
public record SessionDetail(Session session,
                            List<Assignment> seats,
                            List<Rider> eligibleRiders,
                            Map<UUID, Integer> sessionsLeft,
                            Map<UUID, SessionEvaluation> evaluations,
                            boolean canAssign,
                            boolean canFinish) {

    public boolean readOnly() {
        return !canAssign;
    }

    public int sessionsLeft(UUID riderId) {
        return sessionsLeft.getOrDefault(riderId, 0);
    }

    public SessionEvaluation evaluation(UUID riderId) {
        return evaluations.get(riderId);
    }

    // Only for a seat the caller gave; the caller checks that.
    public boolean canEvaluate(UUID riderId) {
        return session.getStatus() == SessionStatus.FINISHED && !evaluations.containsKey(riderId);
    }
}
