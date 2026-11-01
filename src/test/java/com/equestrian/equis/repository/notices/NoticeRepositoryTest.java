package com.equestrian.equis.repository.notices;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.notices.Notice;
import com.equestrian.equis.entity.notices.NoticeType;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The notice table, its checks and the reminder queries. Case 1 is that this context starts at all:
// Flyway runs V15 and Hibernate validates the Notice mapping against it.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class NoticeRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    NoticeRepository noticeRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    EntityManager entityManager;

    Trainer ahmed;

    @BeforeEach
    void setUp() {
        Trainer trainer = new Trainer();
        trainer.setName("Ahmed Said");
        trainer.setPhone("01999990001");
        trainer.setEmail("notice-test-ahmed@example.com");
        ahmed = trainerRepository.saveAndFlush(trainer);
    }

    // Case 1
    @Test
    void aReminderIsStoredAndReadBack() {
        UUID booking = UUID.randomUUID();
        Notice saved = noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, booking, "Result overdue: X.", NOW));
        entityManager.clear();

        Notice found = noticeRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getType()).isEqualTo(NoticeType.RESULT_OVERDUE);
        assertThat(found.getRefId()).isEqualTo(booking);
        assertThat(found.getMessage()).isEqualTo("Result overdue: X.");
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
        assertThat(found.getReadAt()).isNull();
        assertThat(jdbcTemplate.queryForObject("select type from notice where id = ?", String.class, saved.getId()))
                .isEqualTo("result_overdue");
    }

    // Case 2: one test per check, since a failed insert aborts the test's transaction.
    @Test
    void anUnknownTypeIsRejected() {
        assertThatThrownBy(() -> insert("hello", "Some text", null))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class,
                        e -> assertThat(e.getMessage()).contains("ck_notice_type"));
    }

    // Case 2
    @Test
    void aBlankMessageIsRejected() {
        assertThatThrownBy(() -> insert("result_overdue", "   ", null))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class,
                        e -> assertThat(e.getMessage()).contains("ck_notice_message"));
    }

    // Case 3
    @Test
    void onlyOneUnreadReminderPerTrainerAndBooking() {
        UUID booking = UUID.randomUUID();
        noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, booking, "First.", NOW));

        assertThatThrownBy(() -> noticeRepository.saveAndFlush(
                Notice.resultOverdue(ahmed, booking, "Second.", NOW.plusSeconds(60))))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class,
                        e -> assertThat(Constraints.nameOf(e)).contains("uq_notice_unread_reminder"));
    }

    // Case 3, second half
    @Test
    void aReadReminderAllowsANewOne() {
        UUID booking = UUID.randomUUID();
        Notice first = noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, booking, "First.", NOW));
        first.markRead(NOW.plusSeconds(30));
        noticeRepository.saveAndFlush(first);

        noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, booking, "Second.", NOW.plusSeconds(60)));

        assertThat(noticeRepository.count()).isEqualTo(2);
        assertThat(noticeRepository.existsByTrainerIdAndTypeAndRefIdAndReadAtIsNull(
                ahmed.getId(), NoticeType.RESULT_OVERDUE, booking)).isTrue();
    }

    @Test
    void markReadKeepsTheFirstTime() {
        Notice notice = Notice.resultOverdue(ahmed, UUID.randomUUID(), "Text.", NOW);
        notice.markRead(NOW.plusSeconds(10));
        notice.markRead(NOW.plusSeconds(20));

        assertThat(notice.getReadAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void anUnreadReminderIsFoundOnlyWhileUnread() {
        UUID booking = UUID.randomUUID();
        assertThat(noticeRepository.existsByTrainerIdAndTypeAndRefIdAndReadAtIsNull(
                ahmed.getId(), NoticeType.RESULT_OVERDUE, booking)).isFalse();

        Notice notice = noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, booking, "Text.", NOW));
        assertThat(noticeRepository.existsByTrainerIdAndTypeAndRefIdAndReadAtIsNull(
                ahmed.getId(), NoticeType.RESULT_OVERDUE, booking)).isTrue();

        notice.markRead(NOW.plusSeconds(10));
        noticeRepository.saveAndFlush(notice);
        assertThat(noticeRepository.existsByTrainerIdAndTypeAndRefIdAndReadAtIsNull(
                ahmed.getId(), NoticeType.RESULT_OVERDUE, booking)).isFalse();
    }

    @Test
    void theLatestReminderPerBookingReadOrNot() {
        UUID sara = UUID.randomUUID();
        UUID omar = UUID.randomUUID();
        UUID none = UUID.randomUUID();
        Notice old = noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, sara, "Old.", NOW));
        old.markRead(NOW.plusSeconds(10));
        noticeRepository.saveAndFlush(old);
        noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, sara, "New.", NOW.plusSeconds(3600)));
        noticeRepository.saveAndFlush(Notice.resultOverdue(ahmed, omar, "Omar.", NOW.plusSeconds(60)));
        // Another type with the same ref doesn't count.
        insert("session_cancelled", "Cancelled.", omar);

        List<RefTime> latest = noticeRepository.findLatestByRefIds(List.of(sara, omar, none), NoticeType.RESULT_OVERDUE);

        assertThat(latest).containsExactlyInAnyOrder(
                new RefTime(sara, NOW.plusSeconds(3600)),
                new RefTime(omar, NOW.plusSeconds(60)));
    }

    private void insert(String type, String message, UUID refId) {
        jdbcTemplate.update("insert into notice (trainer_id, type, ref_id, message) values (?, ?, ?, ?)",
                ahmed.getId(), type, refId, message);
    }
}
