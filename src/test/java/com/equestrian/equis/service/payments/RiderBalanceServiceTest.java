package com.equestrian.equis.service.payments;

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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Statistics are on so the N+1 test can count SQL statements.
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(RiderBalanceService.class)
public class RiderBalanceServiceTest {
    private static final short LEVEL_3 = 3;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // 1 Nov 2026, 00:30 in Cairo, still 31 Oct in UTC: today() must follow the school time zone.
    private static final Instant NOW = ZonedDateTime.of(2026, 11, 1, 0, 30, 0, 0, CAIRO).toInstant();
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    RiderBalanceService service;
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

    UUID admin;
    Rider sara;

    @BeforeEach
    void setUp() {
        admin = adminAccountRepository.saveAndFlush(admin()).getId();
        sara = riderRepository.saveAndFlush(rider("Sara Adel"));
    }

    @Test
    void todayIsInTheSchoolTimeZone() {
        assertThat(service.today()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    // Case 6
    @Test
    void riderWithNoPaymentsIsAwaitingPayment() {
        RiderBalance balance = service.balanceOn(sara.getId(), LocalDate.of(2026, 11, 3));

        assertThat(balance.riderId()).isEqualTo(sara.getId());
        assertThat(balance.sessionsLeft()).isZero();
        assertThat(balance.status()).isEqualTo(RiderPaymentStatus.AWAITING_PAYMENT);
        assertThat(balance.nextExpiry()).isNull();
        assertThat(balance.packages()).isEmpty();
        assertThat(service.packageToUse(sara.getId(), LocalDate.of(2026, 11, 3))).isEmpty();
    }

    // Case 7
    @Test
    void onePackageCountsThroughTheGraceMonthThenExpires() {
        pay(sara, NOV, 1, 8);

        assertBalance(LocalDate.of(2026, 11, 3), 8, RiderPaymentStatus.ACTIVE);
        assertBalance(LocalDate.of(2026, 12, 31), 8, RiderPaymentStatus.ACTIVE);
        assertBalance(LocalDate.of(2027, 1, 1), 0, RiderPaymentStatus.AWAITING_PAYMENT);
    }

    // Case 8
    @Test
    void packagesAddUpAndTheFirstToExpireComesFirst() {
        Payment dec = pay(sara, DEC, 1, 8);
        Payment nov = pay(sara, NOV, 1, 8);

        RiderBalance balance = service.balanceOn(sara.getId(), LocalDate.of(2026, 12, 10));

        assertThat(balance.sessionsLeft()).isEqualTo(16);
        assertThat(balance.status()).isEqualTo(RiderPaymentStatus.ACTIVE);
        assertThat(balance.nextExpiry()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(balance.packages()).extracting(PackageBalance::paymentId).containsExactly(nov.getId(), dec.getId());
    }

    // Case 9
    @Test
    void usedSessionsComeOffAndExpireWithTheirPackage() {
        Payment nov = pay(sara, NOV, 1, 8);
        Payment dec = pay(sara, DEC, 1, 8);
        use(nov, 5);

        RiderBalance onDec10 = service.balanceOn(sara.getId(), LocalDate.of(2026, 12, 10));
        assertThat(onDec10.sessionsLeft()).isEqualTo(11);
        assertThat(onDec10.packages()).containsExactly(
                new PackageBalance(nov.getId(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), 8, 5, 3),
                new PackageBalance(dec.getId(), LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 31), 8, 0, 8));

        RiderBalance onJan1 = service.balanceOn(sara.getId(), LocalDate.of(2027, 1, 1));
        assertThat(onJan1.sessionsLeft()).isEqualTo(8);
        assertThat(onJan1.nextExpiry()).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    // Case 10
    @Test
    void nextSessionComesFromTheOldestPackageUntilItIsUsedUp() {
        Payment nov = pay(sara, NOV, 1, 8);
        Payment dec = pay(sara, DEC, 1, 8);
        LocalDate dec12 = LocalDate.of(2026, 12, 12);

        assertThat(service.packageToUse(sara.getId(), dec12)).contains(nov.getId());

        use(nov, 8);
        assertThat(service.packageToUse(sara.getId(), dec12)).contains(dec.getId());
        RiderBalance balance = service.balanceOn(sara.getId(), dec12);
        assertThat(balance.sessionsLeft()).isEqualTo(8);
        // The used-up November package no longer sets the next expiry.
        assertThat(balance.nextExpiry()).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    // Decision 6: same expiry, earlier start month first; then the one recorded first.
    @Test
    void tiesGoToTheEarlierStartMonthThenTheFirstRecorded() {
        Payment novTwoMonths = pay(sara, NOV, 2, 16, AT);                     // valid to 31 Jan
        Payment decLater = pay(sara, DEC, 1, 8, AT.plusSeconds(120));         // valid to 31 Jan
        Payment decFirst = pay(sara, DEC, 1, 8, AT.plusSeconds(60));          // valid to 31 Jan

        assertThat(service.balanceOn(sara.getId(), LocalDate.of(2026, 12, 10)).packages())
                .extracting(PackageBalance::paymentId)
                .containsExactly(novTwoMonths.getId(), decFirst.getId(), decLater.getId());
    }

    // Case 11
    @Test
    void aPackageDoesNotCountBeforeItsStartMonth() {
        pay(sara, DEC, 1, 8, Instant.parse("2026-11-25T08:00:00Z"));

        assertBalance(LocalDate.of(2026, 11, 25), 0, RiderPaymentStatus.AWAITING_PAYMENT);
        assertBalance(LocalDate.of(2026, 12, 1), 8, RiderPaymentStatus.ACTIVE);
    }

    // Case 12
    @Test
    void aVoidedPackageCountsForNothing() {
        Payment only = pay(sara, NOV, 1, 8);
        only.voidPayment(admin, "Entered by mistake", AT);
        paymentRepository.saveAndFlush(only);

        RiderBalance balance = service.balanceOn(sara.getId(), LocalDate.of(2026, 11, 10));
        assertThat(balance.sessionsLeft()).isZero();
        assertThat(balance.status()).isEqualTo(RiderPaymentStatus.AWAITING_PAYMENT);
        assertThat(balance.packages()).isEmpty();
        assertThat(paymentRepository.findByRiderIdOrderByStartMonthAscRecordedAtAsc(sara.getId()))
                .singleElement().satisfies(p -> assertThat(p.isVoided()).isTrue());
    }

    // Case 16
    @Test
    void balancesForSeveralRidersTakeTwoQueries() {
        Rider omar = riderRepository.saveAndFlush(rider("Omar Khaled"));
        Rider laila = riderRepository.saveAndFlush(rider("Laila Hassan"));
        Payment saraNov = pay(sara, NOV, 1, 8);
        pay(sara, DEC, 1, 8);
        Payment omarNov = pay(omar, NOV, 2, 12);
        use(saraNov, 2);
        use(omarNov, 12);
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        Map<UUID, RiderBalance> balances = service.balancesOn(
                List.of(sara.getId(), omar.getId(), laila.getId()), LocalDate.of(2026, 12, 10));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(balances.keySet()).containsExactly(sara.getId(), omar.getId(), laila.getId());
        assertThat(balances.get(sara.getId()).sessionsLeft()).isEqualTo(14);
        assertThat(balances.get(sara.getId()).status()).isEqualTo(RiderPaymentStatus.ACTIVE);
        assertThat(balances.get(omar.getId()).sessionsLeft()).isZero();
        assertThat(balances.get(omar.getId()).status()).isEqualTo(RiderPaymentStatus.AWAITING_PAYMENT);
        assertThat(balances.get(omar.getId()).nextExpiry()).isNull();
        assertThat(balances.get(omar.getId()).packages()).hasSize(1);
        assertThat(balances.get(laila.getId())).isEqualTo(RiderBalance.empty(laila.getId()));
    }

    @Test
    void noRidersGiveAnEmptyMap() {
        assertThat(service.balancesOn(List.of(), LocalDate.of(2026, 12, 10))).isEmpty();
    }

    private void assertBalance(LocalDate date, int sessionsLeft, RiderPaymentStatus status) {
        RiderBalance balance = service.balanceOn(sara.getId(), date);
        assertThat(balance.sessionsLeft()).as("sessions left on %s", date).isEqualTo(sessionsLeft);
        assertThat(balance.status()).as("status on %s", date).isEqualTo(status);
    }

    private Payment pay(Rider rider, YearMonth start, int months, int sessions) {
        return pay(rider, start, months, sessions, AT);
    }

    private Payment pay(Rider rider, YearMonth start, int months, int sessions, Instant recordedAt) {
        return paymentRepository.saveAndFlush(Payment.record(rider, new BigDecimal("1200"), PaymentMethod.CASH,
                start, months, sessions, admin, recordedAt));
    }

    // Uses count sessions from the package, one session a day from the package's first day.
    private void use(Payment payment, int count) {
        for (int i = 0; i < count; i++) {
            Session session = session(payment.getStartMonth().plusDays(i));
            sessionUsageRepository.saveAndFlush(SessionUsage.of(payment, session, AT));
        }
    }

    private Session session(LocalDate date) {
        SessionTemplate t = new SessionTemplate();
        t.setLevel(level());
        t.setDayOfWeek(date.getDayOfWeek());
        t.setStartTime(LocalTime.of(9, 0));
        t.setEndTime(LocalTime.of(10, 0));
        t.setCapacity(12);
        t.setValidFrom(LocalDate.of(2026, 9, 1));
        t = sessionTemplateRepository.saveAndFlush(t);
        return sessionRepository.saveAndFlush(Session.of(t, date));
    }

    private Level level() {
        return levelRepository.getReferenceById(LEVEL_3);
    }

    private Rider rider(String name) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setLevel(level());
        return rider;
    }

    private AdminAccount admin() {
        AdminAccount account = new AdminAccount();
        account.setEmail("balance-test-admin@example.com");
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
