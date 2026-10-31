package com.equestrian.equis.service.payments;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.payments.VoidPaymentRequest;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.entity.payments.RiderPaymentStatus;
import com.equestrian.equis.entity.payments.SessionUsage;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.payments.PaymentRepository;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// E13 · the deduction under the trainer's Finished button. Statistics are on so case 12 can count SQL statements.
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({SessionUsageService.class, RiderBalanceService.class, AdminPaymentService.class, PaymentEntryAssembler.class})
public class SessionUsageServiceTest {
    private static final short LEVEL_2 = 2;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // Tue 3 Nov 2026, 18:05 in Cairo.
    private static final Instant NOW = ZonedDateTime.of(2026, 11, 3, 18, 5, 0, 0, CAIRO).toInstant();
    private static final Instant AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final LocalDate NOV_3 = LocalDate.of(2026, 11, 3);
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    SessionUsageService service;
    @Autowired
    RiderBalanceService balances;
    @Autowired
    AdminPaymentService adminPayments;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    SessionUsageRepository sessionUsageRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    AdminAccountRepository adminAccountRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    Environment environment;

    UUID admin;
    Rider sara;

    // The race tests run without the test transaction, so these rows are committed there and removed at the end.
    @BeforeEach
    void setUp() {
        admin = adminAccountRepository.saveAndFlush(admin()).getId();
        sara = riderRepository.saveAndFlush(rider("Sara Adel"));
    }

    // Case 1: this context only starts if Hibernate validates the migrated schema.
    @Test
    void contextStartsWithSchemaValidation() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(service).isNotNull();
    }

    // Case 8
    @Test
    void oneSessionComesOffTheOnlyPackage() {
        Payment nov = pay(sara, NOV, 1, 8);
        Session session = session(NOV_3);

        List<SessionUsage> used = service.useOneSessionEach(session, List.of(sara), NOW);

        assertThat(used).singleElement().satisfies(u -> {
            assertThat(u.getId()).isNotNull();
            assertThat(u.getPayment()).isEqualTo(nov);
            assertThat(u.getRider()).isEqualTo(sara);
            assertThat(u.getSession()).isEqualTo(session);
            assertThat(u.getUsedAt()).isEqualTo(NOW);
        });
        assertThat(balances.balanceOn(sara.getId(), NOV_3).sessionsLeft()).isEqualTo(7);
    }

    // Case 9
    @Test
    void theOldestPackageWithSessionsLeftIsUsed() {
        Payment nov = pay(sara, NOV, 1, 8);
        pay(sara, DEC, 1, 8);
        use(nov, 5);

        List<SessionUsage> used = service.useOneSessionEach(session(LocalDate.of(2026, 12, 12)), List.of(sara), NOW);

        assertThat(used).singleElement().satisfies(u -> assertThat(u.getPayment()).isEqualTo(nov));
    }

    // Case 10
    @Test
    void theSessionDateDecidesWhichPackageCountsNotToday() {
        Payment nov = pay(sara, NOV, 1, 8);                              // valid to 31 Dec
        pay(sara, YearMonth.of(2027, 1), 1, 8);                          // valid from 1 Jan
        Instant jan1 = ZonedDateTime.of(2027, 1, 1, 0, 20, 0, 0, CAIRO).toInstant();

        List<SessionUsage> used = service.useOneSessionEach(session(LocalDate.of(2026, 12, 31)), List.of(sara), jan1);

        assertThat(used).singleElement().satisfies(u -> assertThat(u.getPayment()).isEqualTo(nov));
    }

    // Case 11
    @Test
    void oneRiderWithoutSessionsStopsTheWholeDeduction() {
        pay(sara, NOV, 1, 8);
        Rider omar = riderRepository.saveAndFlush(rider("Omar Ali"));
        Payment omarVoided = pay(omar, NOV, 1, 8);
        omarVoided.voidPayment(admin, "Entered by mistake", AT);
        paymentRepository.saveAndFlush(omarVoided);
        Session session = session(NOV_3);

        assertThatThrownBy(() -> service.useOneSessionEach(session, List.of(sara, omar), NOW))
                .isInstanceOfSatisfying(NoSessionsLeftException.class,
                        e -> assertThat(e.getRiders()).containsExactly(omar));
        assertThat(sessionUsageRepository.count()).isZero();
        assertThat(balances.balanceOn(sara.getId(), NOV_3).sessionsLeft()).isEqualTo(8);
    }

    @Test
    void ridersWithoutSessionsAreSortedByName() {
        Rider omar = riderRepository.saveAndFlush(rider("Omar Ali"));
        Rider ahmed = riderRepository.saveAndFlush(rider("Ahmed Nabil"));

        assertThatThrownBy(() -> service.useOneSessionEach(session(NOV_3), List.of(sara, omar, ahmed), NOW))
                .isInstanceOfSatisfying(NoSessionsLeftException.class,
                        e -> assertThat(e.getRiders()).containsExactly(ahmed, omar, sara));
    }

    // Case 12
    @Test
    void noRidersWriteNothingAndQueryNothing() {
        Session session = session(NOV_3);
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        assertThat(service.useOneSessionEach(session, List.of(), NOW)).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    // Case 13
    @Test
    void theLastSessionLeavesTheRiderAwaitingPayment() {
        Payment nov = pay(sara, NOV, 1, 8);
        use(nov, 7);

        service.useOneSessionEach(session(NOV_3), List.of(sara), NOW);

        RiderBalance balance = balances.balanceOn(sara.getId(), NOV_3);
        assertThat(balance.sessionsLeft()).isZero();
        assertThat(balance.status()).isEqualTo(RiderPaymentStatus.AWAITING_PAYMENT);
    }

    // Case 14
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void needsARunningTransaction() {
        try {
            Session session = Session.of(template(), NOV_3);
            assertThatThrownBy(() -> service.useOneSessionEach(session, List.of(sara), NOW))
                    .isInstanceOf(IllegalTransactionStateException.class);
        } finally {
            cleanUp();
        }
    }

    // Case 15, deduction first: the void waits for the lock, then sees the usage and is refused.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aVoidWaitingOnADeductionIsRefused() throws Exception {
        try {
            Payment only = pay(sara, NOV, 1, 8);
            Session session = session(NOV_3);
            CountDownLatch locked = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);

            CompletableFuture<Void> deduction = CompletableFuture.runAsync(() -> inTransaction(() -> {
                service.useOneSessionEach(session, List.of(sara), NOW);
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            CompletableFuture<Void> voiding = CompletableFuture.runAsync(() ->
                    adminPayments.voidPayment(only.getId(), new VoidPaymentRequest("Entered by mistake"), admin));
            Thread.sleep(500);
            assertThat(voiding).as("the void waits for the deduction's lock").isNotDone();

            release.countDown();
            deduction.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> voiding.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ApiException.class)
                    .cause().satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo(ErrorCodes.PAYMENT_HAS_USAGE));

            assertThat(paymentRepository.findById(only.getId()).orElseThrow().isVoided()).isFalse();
            assertThat(sessionUsageRepository.countByPaymentId(only.getId())).isEqualTo(1);
        } finally {
            cleanUp();
        }
    }

    // Case 15, void first: the deduction waits for the lock, then no longer sees the voided package.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aDeductionWaitingOnAVoidFindsNoSessionsLeft() throws Exception {
        try {
            Payment only = pay(sara, NOV, 1, 8);
            Session session = session(NOV_3);
            CountDownLatch locked = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);

            CompletableFuture<Void> voiding = CompletableFuture.runAsync(() -> inTransaction(() -> {
                adminPayments.voidPayment(only.getId(), new VoidPaymentRequest("Entered by mistake"), admin);
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            CompletableFuture<Void> deduction = CompletableFuture.runAsync(() -> inTransaction(() ->
                    service.useOneSessionEach(session, List.of(sara), NOW)));
            Thread.sleep(500);
            assertThat(deduction).as("the deduction waits for the void's lock").isNotDone();

            release.countDown();
            voiding.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> deduction.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(NoSessionsLeftException.class);

            assertThat(paymentRepository.findById(only.getId()).orElseThrow().isVoided()).isTrue();
            assertThat(sessionUsageRepository.countByPaymentId(only.getId())).isZero();
        } finally {
            cleanUp();
        }
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> work.run());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the other transaction");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // Removes what the tests without a test transaction committed.
    private void cleanUp() {
        jdbc.update("delete from session_usage where rider_id = ?", sara.getId());
        jdbc.update("delete from payment where rider_id = ?", sara.getId());
        jdbc.update("delete from session where template_id in (select id from session_template where valid_from = '1999-01-01')");
        jdbc.update("delete from session_template where valid_from = '1999-01-01'");
        jdbc.update("delete from rider where id = ?", sara.getId());
        jdbc.update("delete from admin_account where id = ?", admin);
    }

    private Payment pay(Rider rider, YearMonth start, int months, int sessions) {
        return paymentRepository.saveAndFlush(Payment.record(rider, new BigDecimal("1200"), PaymentMethod.CASH,
                start, months, sessions, admin, AT));
    }

    // Uses count sessions from the package, one session a day from the package's first day.
    private void use(Payment payment, int count) {
        for (int i = 0; i < count; i++) {
            Session session = session(payment.getStartMonth().plusDays(i));
            sessionUsageRepository.saveAndFlush(SessionUsage.of(payment, session, AT));
        }
    }

    private Session session(LocalDate date) {
        return sessionRepository.saveAndFlush(Session.of(template(), date));
    }

    // Each session gets its own template, so any date can be used. The marker start date lets cleanUp find them.
    private SessionTemplate template() {
        SessionTemplate t = new SessionTemplate();
        t.setLevel(level());
        t.setDayOfWeek(NOV_3.getDayOfWeek());
        t.setStartTime(LocalTime.of(17, 0));
        t.setEndTime(LocalTime.of(18, 0));
        t.setCapacity(12);
        t.setValidFrom(LocalDate.of(1999, 1, 1));
        return sessionTemplateRepository.saveAndFlush(t);
    }

    private Level level() {
        return levelRepository.getReferenceById(LEVEL_2);
    }

    private Rider rider(String name) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(level());
        return rider;
    }

    private AdminAccount admin() {
        AdminAccount account = new AdminAccount();
        account.setEmail("usage-test-admin@example.com");
        account.setPasswordHash("hash");
        return account;
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
