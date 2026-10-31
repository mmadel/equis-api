package com.equestrian.equis.service.sessions;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
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
import org.assertj.core.api.InstanceOfAssertFactories;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({AssignmentService.class, AssignmentRules.class, RiderBalanceService.class})
public class AssignmentServiceTest {
    private static final short LEVEL_1 = 1;
    private static final short LEVEL_2 = 2;
    // The clock is fixed on this Monday at 13:00 school time (Africa/Cairo).
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    AssignmentService service;
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

    Trainer sara;
    Trainer omar;
    Rider laila;
    Rider nour;
    Rider hana;
    Rider omarsRider;
    AdminAccount admin;

    @BeforeEach
    void setUp() {
        sara = trainer("Sara Ali", "01999990001", "assignment-service-test-sara@example.com");
        omar = trainer("Omar Hany", "01999990002", "assignment-service-test-omar@example.com");
        laila = rider("Laila Nour", LEVEL_1, sara);
        nour = rider("Nour Adel", LEVEL_1, sara);
        hana = rider("Hana Samir", LEVEL_1, sara);
        omarsRider = rider("Youssef Tarek", LEVEL_1, omar);
    }

    @Test
    void seatsTheRequestedRidersAndReturnsTheSession() {
        Session session = session(LEVEL_1, TODAY, 6);

        Session returned = service.replaceMine(sara.getId(), session.getId(), ids(laila, nour));

        assertThat(returned).isEqualTo(session);
        assertThat(seats(session)).extracting(Assignment::getRider).containsExactlyInAnyOrder(laila, nour);
        assertThat(seats(session)).extracting(Assignment::getAssignedBy).containsOnly(sara);
    }

    @Test
    void replacingKeepsUnchangedSeatsRemovesDroppedOnesAndAddsNewOnes() {
        Session session = session(LEVEL_1, TODAY, 6);
        Assignment lailasSeat = assign(session, laila, sara);
        assign(session, nour, sara);

        service.replaceMine(sara.getId(), session.getId(), ids(laila, hana));

        assertThat(seats(session)).extracting(Assignment::getRider).containsExactlyInAnyOrder(laila, hana);
        assertThat(seats(session)).contains(lailasSeat);
    }

    @Test
    void emptyListRemovesOnlyTheCallersSeats() {
        Session session = session(LEVEL_1, TODAY, 6);
        assign(session, laila, sara);
        Assignment omarsSeat = assign(session, omarsRider, omar);

        service.replaceMine(sara.getId(), session.getId(), List.of());

        assertThat(seats(session)).containsExactly(omarsSeat);
    }

    @Test
    void repeatedIdsAreSeatedOnce() {
        Session session = session(LEVEL_1, TODAY, 6);

        service.replaceMine(sara.getId(), session.getId(), ids(laila, laila));

        assertThat(seats(session)).extracting(Assignment::getRider).containsExactly(laila);
    }

    @Test
    void unknownSessionIsNotFound() {
        assertRefused(() -> service.replaceMine(sara.getId(), UUID.randomUUID(), ids(laila)),
                HttpStatus.NOT_FOUND, ErrorCodes.SESSION_NOT_FOUND);
    }

    @Test
    void startedSessionTodayStillTakesRiders() {
        // 13:00 now; the session started at 09:00 today.
        Session started = sessionRepository.saveAndFlush(Session.of(template(LEVEL_1, 6, LocalTime.of(9, 0)), TODAY));

        service.replaceMine(sara.getId(), started.getId(), ids(laila));

        assertThat(seats(started)).extracting(Assignment::getRider).containsExactly(laila);
    }

    @Test
    void sessionsOnOtherDaysAreNotToday() {
        Session yesterday = session(LEVEL_1, TODAY.minusDays(1), 6);
        Session nextWeek = session(LEVEL_1, TODAY.plusWeeks(1), 6);

        assertRefused(() -> service.replaceMine(sara.getId(), yesterday.getId(), ids(laila)),
                HttpStatus.CONFLICT, ErrorCodes.SESSION_NOT_TODAY);
        assertRefused(() -> service.replaceMine(sara.getId(), nextWeek.getId(), ids(laila)),
                HttpStatus.CONFLICT, ErrorCodes.SESSION_NOT_TODAY);
    }

    @Test
    void finishedAndCancelledSessionsAreReadOnly() {
        Session finished = Session.of(template(LEVEL_1, 6), TODAY);
        finished.finish(sara.getId(), NOW);
        sessionRepository.saveAndFlush(finished);
        Session cancelled = Session.of(template(LEVEL_1, 6), TODAY);
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);

        assertRefused(() -> service.replaceMine(sara.getId(), finished.getId(), ids(laila)),
                HttpStatus.CONFLICT, ErrorCodes.SESSION_READ_ONLY);
        assertRefused(() -> service.replaceMine(sara.getId(), cancelled.getId(), ids(laila)),
                HttpStatus.CONFLICT, ErrorCodes.SESSION_READ_ONLY);
    }

    @Test
    void newlyAddedRiderWithoutASessionLeftIsRefused() {
        Session session = session(LEVEL_1, TODAY, 6);
        Rider unpaid = rider("Omar Fathy", LEVEL_1, sara, false);

        assertRefused(() -> service.replaceMine(sara.getId(), session.getId(), ids(laila, unpaid)),
                HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.RIDER_NO_SESSIONS_LEFT, unpaid.getId());
        assertThat(seats(session)).isEmpty();
    }

    @Test
    void riderAlreadySeatedIsNotCheckedAgain() {
        Session session = session(LEVEL_1, TODAY, 6);
        Rider unpaid = rider("Omar Fathy", LEVEL_1, sara, false);
        assign(session, unpaid, sara);

        service.replaceMine(sara.getId(), session.getId(), ids(unpaid, laila));

        assertThat(seats(session)).extracting(Assignment::getRider).containsExactlyInAnyOrder(unpaid, laila);
    }

    @Test
    void anotherTrainersRiderAndUnknownRiderAreRefused() {
        Session session = session(LEVEL_1, TODAY, 6);
        UUID unknown = UUID.randomUUID();

        assertRefused(() -> service.replaceMine(sara.getId(), session.getId(),
                        List.of(laila.getId(), omarsRider.getId(), unknown)),
                HttpStatus.FORBIDDEN, ErrorCodes.NOT_YOUR_RIDER, omarsRider.getId(), unknown);
        assertThat(seats(session)).isEmpty();
    }

    @Test
    void riderOfAnotherLevelIsRefused() {
        Session session = session(LEVEL_1, TODAY, 6);
        Rider levelTwo = rider("Mariam Fouad", LEVEL_2, sara);

        assertRefused(() -> service.replaceMine(sara.getId(), session.getId(), ids(laila, levelTwo)),
                HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.LEVEL_OUT_OF_RANGE, levelTwo.getId());
    }

    @Test
    void assessmentSlotIsNotFound() {
        Session session = session(null, TODAY, 6);

        assertRefused(() -> service.replaceMine(sara.getId(), session.getId(), ids(laila)),
                HttpStatus.NOT_FOUND, ErrorCodes.SESSION_NOT_FOUND);
    }

    @Test
    void riderSeatedByAnotherTrainerIsAlreadyBooked() {
        Session session = session(LEVEL_1, TODAY, 6);
        // Omar seated the rider before she moved to Sara.
        assign(session, laila, omar);

        assertRefused(() -> service.replaceMine(sara.getId(), session.getId(), ids(laila, nour)),
                HttpStatus.CONFLICT, ErrorCodes.ALREADY_BOOKED, laila.getId());
    }

    @Test
    void cannotSeatMoreRidersThanTheSeatsOtherTrainersLeftFree() {
        Session session = session(LEVEL_1, TODAY, 2);
        assign(session, omarsRider, omar);

        assertThatThrownBy(() -> service.replaceMine(sara.getId(), session.getId(), ids(laila, nour)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.CAPACITY_EXCEEDED);
                    assertThat(ex.properties()).containsEntry("capacity", 2).containsEntry("free", 1);
                });
    }

    @Test
    void theCallersOwnSeatsDoNotCountAgainstTheReplacement() {
        Session session = session(LEVEL_1, TODAY, 2);
        assign(session, laila, sara);
        assign(session, nour, sara);

        service.replaceMine(sara.getId(), session.getId(), ids(nour, hana));

        assertThat(seats(session)).extracting(Assignment::getRider).containsExactlyInAnyOrder(nour, hana);
    }

    private void assertRefused(Runnable call, HttpStatus status, String code, UUID... riderIds) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
            if (riderIds.length > 0) {
                assertThat(ex.properties().get("fieldErrors"))
                        .asInstanceOf(InstanceOfAssertFactories.list(ApiFieldError.class))
                        .extracting(ApiFieldError::field)
                        .containsExactly(List.of(riderIds).stream().map(UUID::toString).toArray(String[]::new));
            }
        });
    }

    private List<UUID> ids(Rider... riders) {
        return List.of(riders).stream().map(Rider::getId).toList();
    }

    private List<Assignment> seats(Session session) {
        assignmentRepository.flush();
        return assignmentRepository.findBySessionIdIn(List.of(session.getId()));
    }

    private Assignment assign(Session session, Rider rider, Trainer trainer) {
        return assignmentRepository.saveAndFlush(Assignment.of(session, rider, trainer));
    }

    private Session session(Short levelId, LocalDate date, int capacity) {
        return sessionRepository.saveAndFlush(Session.of(template(levelId, capacity), date));
    }

    private SessionTemplate template(Short levelId, int capacity) {
        return template(levelId, capacity, LocalTime.of(16, 0));
    }

    private SessionTemplate template(Short levelId, int capacity, LocalTime start) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(start);
        template.setEndTime(start.plusHours(1));
        template.setCapacity(capacity);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    // Riders come with an October package of 8 sessions, so they can ride today, unless paid is false.
    private Rider rider(String name, short levelId, Trainer trainer) {
        return rider(name, levelId, trainer, true);
    }

    private Rider rider(String name, short levelId, Trainer trainer, boolean paid) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(levelRepository.getReferenceById(levelId));
        rider.setTrainer(trainer);
        rider = riderRepository.saveAndFlush(rider);
        if (paid) {
            paymentRepository.saveAndFlush(Payment.record(rider, new BigDecimal("1200"), PaymentMethod.CASH,
                    YearMonth.of(2026, 10), 1, 8, admin().getId(), NOW));
        }
        return rider;
    }

    private AdminAccount admin() {
        if (admin == null) {
            AdminAccount account = new AdminAccount();
            account.setEmail("assignment-service-test-admin@example.com");
            account.setPasswordHash("hash");
            admin = adminAccountRepository.saveAndFlush(account);
        }
        return admin;
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainerRepository.saveAndFlush(trainer);
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
