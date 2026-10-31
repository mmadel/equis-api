package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Assignment;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.payments.PaymentRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.AssignmentRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.service.payments.RiderBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({SessionDetailService.class, RiderBalanceService.class})
public class SessionDetailServiceTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_2 = 2;
    // The clock is fixed on this Monday at 13:00 school time (Africa/Cairo).
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SessionDetailService service;
    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    AdminAccountRepository adminAccountRepository;

    AdminAccount admin;
    Trainer sara;
    Trainer omar;
    Rider laila;
    Rider nour;
    Rider unpaid;
    Rider omarsRider;

    @BeforeEach
    void setUp() {
        admin = admin();
        sara = trainer("Sara Ali", "01999990021", "session-detail-service-test-sara@example.com");
        omar = trainer("Omar Hany", "01999990022", "session-detail-service-test-omar@example.com");
        nour = rider("Nour Adel", LEVEL_1, sara, 1);
        laila = rider("Laila Nour", LEVEL_1, sara, 8);
        unpaid = rider("Hana Samir", LEVEL_1, sara, 0);
        rider("Mariam Fouad", LEVEL_2, sara, 8);
        omarsRider = rider("Youssef Tarek", LEVEL_1, omar, 8);
    }

    @Test
    void todaysSessionListsTheCallersRidersWhoCanRideTodayWithTheirSessionsLeft() {
        Session session = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 16), TODAY));
        Assignment mine = assign(session, laila, sara);
        Assignment others = assign(session, omarsRider, omar);

        SessionDetail detail = service.get(sara.getId(), session.getId());

        assertThat(detail.session()).isEqualTo(session);
        assertThat(detail.session().getTemplate().getLevel().getName()).isEqualTo("Level 1");
        assertThat(detail.seats()).containsExactlyInAnyOrder(mine, others);
        assertThat(detail.eligibleRiders()).containsExactly(laila, nour);
        assertThat(detail.sessionsLeft(laila.getId())).isEqualTo(8);
        assertThat(detail.sessionsLeft(nour.getId())).isEqualTo(1);
        assertThat(detail.canAssign()).isTrue();
        assertThat(detail.readOnly()).isFalse();
        assertThat(detail.canFinish()).isFalse();
    }

    @Test
    void startedSessionTodayCanStillTakeRidersAndBeFinished() {
        Session started = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 9), TODAY));
        assign(started, laila, sara);

        SessionDetail detail = service.get(sara.getId(), started.getId());

        assertThat(detail.canAssign()).isTrue();
        assertThat(detail.canFinish()).isTrue();
    }

    @Test
    void seatedRiderWithNothingLeftShowsZero() {
        Session session = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 16), TODAY));
        assign(session, unpaid, sara);

        SessionDetail detail = service.get(sara.getId(), session.getId());

        assertThat(detail.sessionsLeft(unpaid.getId())).isZero();
        assertThat(detail.eligibleRiders()).doesNotContain(unpaid);
    }

    @Test
    void otherDaysFinishedAndCancelledSessionsAreReadOnlyWithNoPickList() {
        Session yesterday = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 16), TODAY.minusDays(1)));
        Session nextWeek = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 16), TODAY.plusWeeks(1)));
        Session finished = Session.of(template(LEVEL_1, 9), TODAY);
        finished.finish(sara.getId(), NOW);
        sessionRepository.saveAndFlush(finished);
        Session cancelled = Session.of(template(LEVEL_1, 16), TODAY);
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);

        for (Session session : new Session[]{yesterday, nextWeek, finished, cancelled}) {
            SessionDetail detail = service.get(sara.getId(), session.getId());
            assertThat(detail.canAssign()).isFalse();
            assertThat(detail.readOnly()).isTrue();
            assertThat(detail.eligibleRiders()).isEmpty();
        }
        assertThat(service.get(sara.getId(), finished.getId()).canFinish()).isFalse();
    }

    @Test
    void onlyATrainerWithARiderInItOrAnyoneOnAnEmptySessionCanFinish() {
        Session othersOnly = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 9), TODAY));
        assign(othersOnly, omarsRider, omar);
        Session empty = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 9), TODAY));

        assertThat(service.get(sara.getId(), othersOnly.getId()).canFinish()).isFalse();
        assertThat(service.get(omar.getId(), othersOnly.getId()).canFinish()).isTrue();
        assertThat(service.get(sara.getId(), empty.getId()).canFinish()).isTrue();
    }

    @Test
    void missingAndAssessmentSessionsAreNotFound() {
        Session assessment = sessionRepository.saveAndFlush(Session.of(template(null, 16), TODAY));

        for (UUID id : new UUID[]{UUID.randomUUID(), assessment.getId()}) {
            assertThatThrownBy(() -> service.get(sara.getId(), id))
                    .isInstanceOfSatisfying(ApiException.class, ex -> {
                        assertThat(ex.status()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.code()).isEqualTo(ErrorCodes.SESSION_NOT_FOUND);
                    });
        }
    }

    private Assignment assign(Session session, Rider rider, Trainer trainer) {
        return assignmentRepository.saveAndFlush(Assignment.of(session, rider, trainer));
    }

    private SessionTemplate template(Short levelId, int startHour) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(startHour, 0));
        template.setEndTime(LocalTime.of(startHour + 1, 0));
        template.setCapacity(6);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    // sessions is the size of an October package; 0 means the rider hasn't paid.
    private Rider rider(String name, short levelId, Trainer trainer, int sessions) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(levelRepository.getReferenceById(levelId));
        rider.setTrainer(trainer);
        rider = riderRepository.saveAndFlush(rider);
        if (sessions > 0) {
            paymentRepository.saveAndFlush(Payment.record(rider, new BigDecimal("1200"), PaymentMethod.CASH,
                    YearMonth.of(2026, 10), 1, sessions, admin.getId(), NOW));
        }
        return rider;
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainerRepository.saveAndFlush(trainer);
    }

    private AdminAccount admin() {
        AdminAccount account = new AdminAccount();
        account.setEmail("session-detail-service-test-admin@example.com");
        account.setPasswordHash("hash");
        return adminAccountRepository.saveAndFlush(account);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
