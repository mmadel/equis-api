package com.equestrian.equis.repository.payments;

import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.entity.payments.SessionUsage;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class PaymentRepositoryTest {
    private static final short LEVEL_3 = 3;
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

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
    JdbcTemplate jdbc;
    @Autowired
    EntityManager entityManager;

    UUID admin;
    Rider sara;
    Rider omar;

    @BeforeEach
    void setUp() {
        admin = adminAccountRepository.saveAndFlush(admin()).getId();
        sara = riderRepository.saveAndFlush(rider("Sara Adel"));
        omar = riderRepository.saveAndFlush(rider("Omar Khaled"));
    }

    // Case 1: the context started, so Flyway ran V19 and Hibernate validated both tables.
    @Test
    void bothTablesExist() {
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN ('payment', 'session_usage')
                """, String.class)).containsExactlyInAnyOrder("payment", "session_usage");
    }

    @Test
    void savesAndReadsBackAPayment() {
        Payment saved = paymentRepository.saveAndFlush(
                Payment.record(sara, new BigDecimal("1200.50"), PaymentMethod.BANK_TRANSFER, NOV, 1, 8, admin, AT));
        entityManager.clear();

        Payment read = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(read.getAmountEgp()).isEqualByComparingTo("1200.50");
        assertThat(read.getMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(read.getStartMonth()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(read.getValidUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(read.getRecordedAt()).isEqualTo(AT);
        assertThat(read.getRecordedBy()).isEqualTo(admin);
        assertThat(jdbc.queryForObject("SELECT method FROM payment WHERE id = ?", String.class, saved.getId()))
                .isEqualTo("bank_transfer");
    }

    // The CHECK on valid_until agrees with Payment.record for every length, across year ends.
    @Test
    void schemaAcceptsWhatRecordComputes() {
        for (int months = 1; months <= 12; months++) {
            paymentRepository.saveAndFlush(record(sara, YearMonth.of(2026, 12), months, 8));
        }
        assertThat(paymentRepository.count()).isEqualTo(12);
    }

    @Test
    void voidingIsSaved() {
        Payment payment = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        payment.voidPayment(admin, " Entered twice ", AT.plusSeconds(60));
        paymentRepository.saveAndFlush(payment);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT void_reason, voided_by, version FROM payment WHERE id = ?", payment.getId());
        assertThat(row.get("void_reason")).isEqualTo("Entered twice");
        assertThat(row.get("voided_by")).isEqualTo(admin);
        assertThat(row.get("version")).isEqualTo(1L);
    }

    // Case 5
    @Test
    void schemaRejectsAnAmountOf0() {
        assertThatThrownBy(() -> insert("0", "cash", "2026-11-01", 1, "2026-12-31"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_payment_amount");
    }

    // Case 5
    @Test
    void schemaRejectsAnUnknownMethod() {
        assertThatThrownBy(() -> insert("100", "paypal", "2026-11-01", 1, "2026-12-31"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_payment_method");
    }

    // Case 5
    @Test
    void schemaRejectsAStartMonthNotOnThe1st() {
        assertThatThrownBy(() -> insert("100", "cash", "2026-11-15", 1, "2027-01-14"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_payment_start_month");
    }

    // Case 5
    @Test
    void schemaRejectsAWrongValidUntil() {
        assertThatThrownBy(() -> insert("100", "cash", "2026-11-01", 1, "2026-11-30"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_payment_valid_until");
    }

    @Test
    void schemaRejectsMonthsAndSessionsOutOfRange() {
        assertThatThrownBy(() -> insert("100", "cash", "2026-11-01", 13, "2027-12-31"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_payment_months");
    }

    // Case 5
    @Test
    void schemaRejectsAVoidWithoutAReason() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO payment (rider_id, amount_egp, method, start_month, months, sessions, valid_until,
                                     recorded_by, voided_at, voided_by, void_reason)
                VALUES (?, 100, 'cash', DATE '2026-11-01', 1, 8, DATE '2026-12-31', ?, now(), ?, '   ')
                """, sara.getId(), admin, admin))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_payment_void");
    }

    // Case 12 (history part): voided payments stay in the history.
    @Test
    void historyListsEveryPaymentByStartMonthThenRecordedAt() {
        Payment dec = paymentRepository.saveAndFlush(record(sara, DEC, 1, 8));
        Payment novLater = paymentRepository.saveAndFlush(
                Payment.record(sara, BigDecimal.TEN, PaymentMethod.CASH, NOV, 1, 8, admin, AT.plusSeconds(3600)));
        Payment novFirst = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        novFirst.voidPayment(admin, "Entered twice", AT);
        paymentRepository.saveAndFlush(novFirst);
        paymentRepository.saveAndFlush(record(omar, NOV, 1, 8));
        entityManager.clear();

        List<Payment> history = paymentRepository.findByRiderIdOrderByStartMonthAscRecordedAtAsc(sara.getId());

        assertThat(history).containsExactly(novFirst, novLater, dec);
        assertThat(history.getFirst().isVoided()).isTrue();
    }

    @Test
    void findValidOnSkipsVoidedFutureAndExpiredPackages() {
        Payment live = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        Payment voided = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        voided.voidPayment(admin, "Wrong rider", AT);
        paymentRepository.saveAndFlush(voided);
        paymentRepository.saveAndFlush(record(sara, YearMonth.of(2027, 1), 1, 8));   // not started yet
        paymentRepository.saveAndFlush(record(sara, YearMonth.of(2026, 9), 1, 8));   // expired 31 Oct
        Payment omars = paymentRepository.saveAndFlush(record(omar, DEC, 1, 8));
        entityManager.clear();

        LocalDate dec31 = LocalDate.of(2026, 12, 31);
        assertThat(paymentRepository.findValidOn(List.of(sara.getId(), omar.getId()), dec31))
                .containsExactlyInAnyOrder(live, omars);
        assertThat(paymentRepository.findValidOn(List.of(sara.getId()), LocalDate.of(2027, 1, 1))).hasSize(1);
    }

    @Test
    void countsUsedSessionsPerPackage() {
        Payment nov = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        Payment dec = paymentRepository.saveAndFlush(record(sara, DEC, 1, 8));
        Payment unused = paymentRepository.saveAndFlush(record(sara, DEC, 1, 8));
        for (int day = 1; day <= 3; day++) {
            sessionUsageRepository.saveAndFlush(SessionUsage.of(nov, session(LocalDate.of(2026, 11, day)), AT));
        }
        sessionUsageRepository.saveAndFlush(SessionUsage.of(dec, session(LocalDate.of(2026, 12, 5)), AT));

        Map<UUID, Long> used = sessionUsageRepository
                .countUsedByPayment(List.of(nov.getId(), dec.getId(), unused.getId())).stream()
                .collect(Collectors.toMap(UsedCount::getPaymentId, UsedCount::getUsed));

        assertThat(used).containsExactlyInAnyOrderEntriesOf(Map.of(nov.getId(), 3L, dec.getId(), 1L));
    }

    @Test
    void usageTakesTheRiderFromThePackage() {
        Payment nov = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        SessionUsage usage = sessionUsageRepository.saveAndFlush(
                SessionUsage.of(nov, session(LocalDate.of(2026, 11, 7)), AT));

        assertThat(jdbc.queryForObject("SELECT rider_id FROM session_usage WHERE id = ?", UUID.class, usage.getId()))
                .isEqualTo(sara.getId());
    }

    // Case 14
    @Test
    void aRiderUsesASessionOnlyOnce() {
        Payment nov = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        Payment dec = paymentRepository.saveAndFlush(record(sara, DEC, 1, 8));
        Session ride = session(LocalDate.of(2026, 12, 12));
        sessionUsageRepository.saveAndFlush(SessionUsage.of(nov, ride, AT));

        assertThatThrownBy(() -> sessionUsageRepository.saveAndFlush(SessionUsage.of(dec, ride, AT)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_session_usage_rider_session");
    }

    // Case 15
    @Test
    void usageRiderMustBeThePackagesRider() {
        Payment sarasPackage = paymentRepository.saveAndFlush(record(sara, NOV, 1, 8));
        Session ride = session(LocalDate.of(2026, 11, 7));

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO session_usage (rider_id, payment_id, session_id) VALUES (?, ?, ?)",
                omar.getId(), sarasPackage.getId(), ride.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_session_usage_payment");
    }

    private void insert(String amount, String method, String startMonth, int months, String validUntil) {
        jdbc.update("""
                INSERT INTO payment (rider_id, amount_egp, method, start_month, months, sessions, valid_until, recorded_by)
                VALUES (?, ?, ?, ?, ?, 8, ?, ?)
                """, sara.getId(), new BigDecimal(amount), method, Date.valueOf(startMonth), months,
                Date.valueOf(validUntil), admin);
    }

    private Payment record(Rider rider, YearMonth start, int months, int sessions) {
        return Payment.record(rider, new BigDecimal("1200"), PaymentMethod.CASH, start, months, sessions, admin, AT);
    }

    private Session session(LocalDate date) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(level());
        template.setDayOfWeek(date.getDayOfWeek());
        template.setStartTime(LocalTime.of(9, 0));
        template.setEndTime(LocalTime.of(10, 0));
        template.setCapacity(12);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        template = sessionTemplateRepository.saveAndFlush(template);
        return sessionRepository.saveAndFlush(Session.of(template, date));
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
        account.setEmail("payment-test-admin@example.com");
        account.setPasswordHash("hash");
        return account;
    }
}
