package com.equestrian.equis.entity.notices;

import com.equestrian.equis.entity.accounts.Trainer;
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

// One in-app notice to a trainer, shown on Home (1). The message is fixed when the notice is made;
// later changes to what it points at don't rewrite it. Only readAt ever changes.
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {
    public static final int MESSAGE_MAX_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false, updatable = false)
    private Trainer trainer;

    @Column(name = "type", nullable = false, updatable = false, length = 32)
    private NoticeType type;

    // The booking for result_overdue, the session for session_cancelled.
    @Column(name = "ref_id", updatable = false)
    private UUID refId;

    @Column(name = "message", nullable = false, updatable = false, length = MESSAGE_MAX_LENGTH)
    private String message;

    // From the app's Clock, not the database, so tests can move time.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Null while unread.
    @Column(name = "read_at")
    private Instant readAt;

    public static Notice resultOverdue(Trainer assessor, UUID bookingId, String message, Instant now) {
        Notice notice = new Notice();
        notice.trainer = Objects.requireNonNull(assessor, "assessor");
        notice.type = NoticeType.RESULT_OVERDUE;
        notice.refId = Objects.requireNonNull(bookingId, "bookingId");
        notice.message = requireMessage(message);
        notice.createdAt = Objects.requireNonNull(now, "now");
        return notice;
    }

    // Reading it again keeps the first readAt.
    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = Objects.requireNonNull(now, "now");
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("A notice needs a message");
        }
        if (message.length() > MESSAGE_MAX_LENGTH) {
            throw new IllegalArgumentException("A notice message is at most " + MESSAGE_MAX_LENGTH + " characters");
        }
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notice other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    // Constant so the hash does not change when the id is generated on save.
    @Override
    public int hashCode() {
        return Notice.class.hashCode();
    }
}
