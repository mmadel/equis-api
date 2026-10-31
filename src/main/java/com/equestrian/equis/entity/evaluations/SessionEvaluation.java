package com.equestrian.equis.entity.evaluations;

import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// A trainer's evaluation of one rider's ride in a finished session: 1 to 5 stars and an optional comment.
// One per rider per session, only for a rider seated in it (both enforced by the database). Never edited.
@Entity
@Table(name = "session_evaluation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionEvaluation {
    public static final int MIN_STARS = 1;
    public static final int MAX_STARS = 5;
    public static final int MAX_COMMENT = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false, updatable = false)
    private Rider rider;

    // The trainer who wrote it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false, updatable = false)
    private Trainer trainer;

    @Column(name = "stars", nullable = false, updatable = false)
    private short stars;

    // Trimmed; null when blank.
    @Column(name = "comment", length = MAX_COMMENT, updatable = false)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static SessionEvaluation of(Session session, Rider rider, Trainer trainer, int stars, String comment,
                                       Instant now) {
        if (stars < MIN_STARS || stars > MAX_STARS) {
            throw new IllegalArgumentException("Stars must be between " + MIN_STARS + " and " + MAX_STARS);
        }
        String trimmed = comment == null ? "" : comment.trim();
        if (trimmed.length() > MAX_COMMENT) {
            throw new IllegalArgumentException("The comment can be up to " + MAX_COMMENT + " characters");
        }
        SessionEvaluation evaluation = new SessionEvaluation();
        evaluation.session = Objects.requireNonNull(session, "session");
        evaluation.rider = Objects.requireNonNull(rider, "rider");
        evaluation.trainer = Objects.requireNonNull(trainer, "trainer");
        evaluation.stars = (short) stars;
        evaluation.comment = trimmed.isEmpty() ? null : trimmed;
        evaluation.createdAt = Objects.requireNonNull(now, "now");
        return evaluation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionEvaluation other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return SessionEvaluation.class.hashCode();
    }
}
