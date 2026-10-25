package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.payments.BalanceDto;
import com.equestrian.equis.dto.riders.AdminRiderRow;
import com.equestrian.equis.dto.riders.MyRiderDto;
import com.equestrian.equis.dto.riders.UpcomingSeat;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.payments.PaymentMethod;
import com.equestrian.equis.entity.payments.RiderPaymentStatus;
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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Statistics are on so the N+1 test can count SQL statements.
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
// AdminRiderRowAssembler builds the W3 rows, so a test can check both screens show the same balance.
@Import({MyRidersQueryService.class, RiderBalanceService.class, AdminRiderRowAssembler.class})
public class MyRidersQueryServiceTest {
    private static final short LEVEL_2 = 2;
    private static final short LEVEL_3 = 3;
    private static final short LEVEL_5 = 5;
    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    // The clock is fixed on Tuesday 3 Nov 2026 at 10:15 school time.
    private static final Instant NOW = ZonedDateTime.of(2026, 11, 3, 10, 15, 0, 0, CAIRO).toInstant();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 7);
    private static final Instant RECORDED_AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private static final YearMonth DEC = YearMonth.of(2026, 12);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    MyRidersQueryService service;
    @Autowired
    AdminRiderRowAssembler adminRows;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    AdminAccountRepository adminAccountRepository;
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
    TestEntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    Trainer ahmed;
    Trainer mona;
    AdminAccount admin;

    @BeforeEach
    void setUp() {
        admin = adminAccountRepository.saveAndFlush(admin());
        ahmed = trainer("Ahmed Samir", "01999990021", "my-riders-test-ahmed@example.com");
        mona = trainer("Mona Fathy", "01999990022", "my-riders-test-mona@example.com");
    }

    @Test
    void listsOnlyTheCallersRidersByLevelThenNameIgnoringCase() {
        rider("Sara Mahmoud", LEVEL_3, ahmed);
        rider("omar Ali", LEVEL_3, ahmed);
        rider("Nour Saleh", LEVEL_2, ahmed);
        rider("Karim Adel", LEVEL_5, ahmed);
        rider("Hana Said", LEVEL_5, ahmed);
        rider("Laila Nabil", LEVEL_2, mona);

        List<MyRiderDto> result = service.myRiders(ahmed.getId());

        assertThat(result).extracting(MyRiderDto::name)
                .containsExactly("Nour Saleh", "omar Ali", "Sara Mahmoud", "Hana Said", "Karim Adel");
        assertThat(result).extracting(MyRiderDto::levelId).containsExactly(2, 3, 3, 5, 5);
        assertThat(result.get(1).initials()).isEqualTo("OA");
    }

    @Test
    void nextSessionIsTheEarliestThatHasNotStarted() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Session saturday1000 = session(LEVEL_3, SATURDAY, LocalTime.of(10, 0));
        Session today1800 = session(LEVEL_3, TODAY, LocalTime.of(18, 0));
        Session today0930 = session(LEVEL_3, TODAY, LocalTime.of(9, 30));
        Session yesterday = session(LEVEL_3, TODAY.minusDays(1), LocalTime.of(18, 0));
        for (Session s : List.of(saturday1000, today1800, today0930, yesterday)) {
            assign(s, sara, ahmed);
        }

        MyRiderDto row = service.myRiders(ahmed.getId()).getFirst();

        // 09:30 today has started, so it is not next.
        assertThat(row.nextSession())
                .isEqualTo(new UpcomingSeat(today1800.getId(), TODAY, LocalTime.of(18, 0), LocalTime.of(19, 0)));
    }

    @Test
    void cancelledSessionsAreNotNext() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Session cancelled = session(LEVEL_3, TODAY, LocalTime.of(12, 0));
        assign(cancelled, sara, ahmed);
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);
        Session saturday1000 = session(LEVEL_3, SATURDAY, LocalTime.of(10, 0));
        assign(saturday1000, sara, ahmed);

        assertThat(service.myRiders(ahmed.getId()).getFirst().nextSession().sessionId())
                .isEqualTo(saturday1000.getId());
    }

    @Test
    void aRiderWithOnlyPastSeatsHasNoNextSession() {
        Rider omar = rider("Omar Ali", LEVEL_3, ahmed);
        assign(session(LEVEL_3, TODAY.minusDays(1), LocalTime.of(18, 0)), omar, ahmed);

        assertThat(service.myRiders(ahmed.getId()).getFirst().nextSession()).isNull();
    }

    @Test
    void anotherTrainersRiderInTheSameSessionsAndUnassignedRidersAreLeftOut() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Rider monasRider = rider("Youssef Tarek", LEVEL_3, mona);
        rider("Free Rider", LEVEL_3, null);
        Session today1800 = session(LEVEL_3, TODAY, LocalTime.of(18, 0));
        Session saturday1000 = session(LEVEL_3, SATURDAY, LocalTime.of(10, 0));
        assign(saturday1000, sara, ahmed);
        // The other rider rides earlier too; that seat must not become Sara's next session.
        assign(today1800, monasRider, mona);
        assign(saturday1000, monasRider, mona);

        List<MyRiderDto> result = service.myRiders(ahmed.getId());

        assertThat(result).extracting(MyRiderDto::id).containsExactly(sara.getId());
        assertThat(result.getFirst().nextSession().sessionId()).isEqualTo(saturday1000.getId());
    }

    @Test
    void aTrainerWithNoRidersGetsAnEmptyList() {
        rider("Youssef Tarek", LEVEL_3, mona);

        assertThat(service.myRiders(ahmed.getId())).isEmpty();
    }

    @Test
    void sinceIsTheCreationDayInSchoolTime() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        // 23:30 in Cairo on 14 Oct is 20:30 UTC.
        jdbc.update("UPDATE rider SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-10-14T20:30:00Z")), sara.getId());
        em.clear();

        assertThat(service.myRiders(ahmed.getId()).getFirst().since()).isEqualTo(LocalDate.of(2026, 10, 14));
    }

    @Test
    void twentyRidersWithSeatsAndPaymentsTakeFourQueries() {
        // Two groups of ten, each with a seat today and one on Saturday (a session holds at most 12).
        List<Session> today = List.of(session(LEVEL_3, TODAY, LocalTime.of(17, 0)),
                session(LEVEL_3, TODAY, LocalTime.of(18, 0)));
        List<Session> saturday = List.of(session(LEVEL_3, SATURDAY, LocalTime.of(10, 0)),
                session(LEVEL_3, SATURDAY, LocalTime.of(11, 0)));
        for (int i = 0; i < 20; i++) {
            Rider rider = rider("Rider " + (char) ('A' + i), LEVEL_3, ahmed);
            assign(today.get(i / 10), rider, ahmed);
            assign(saturday.get(i / 10), rider, ahmed);
            pay(rider, NOV, 1, 8);
        }
        em.clear();

        Statistics statistics = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        List<MyRiderDto> result = service.myRiders(ahmed.getId());

        assertThat(result).hasSize(20);
        for (int i = 0; i < 20; i++) {
            assertThat(result.get(i).levelId()).isEqualTo(3);
            assertThat(result.get(i).nextSession().sessionId()).isEqualTo(today.get(i / 10).getId());
            assertThat(result.get(i).balance())
                    .isEqualTo(new BalanceDto(8, RiderPaymentStatus.ACTIVE, LocalDate.of(2026, 12, 31)));
        }
        // Riders, seats, valid payments, used sessions.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    // Case 1
    @Test
    void aRiderWithANovemberPackageHasItsSessionsLeft() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        pay(sara, NOV, 1, 8);

        assertThat(service.myRiders(ahmed.getId()).getFirst().balance())
                .isEqualTo(new BalanceDto(8, RiderPaymentStatus.ACTIVE, LocalDate.of(2026, 12, 31)));
    }

    // Case 2
    @Test
    void aRiderWhoHasNotPaidIsAwaitingPayment() {
        rider("Omar Ali", LEVEL_3, ahmed);

        assertThat(service.myRiders(ahmed.getId()).getFirst().balance())
                .isEqualTo(new BalanceDto(0, RiderPaymentStatus.AWAITING_PAYMENT, null));
    }

    // Case 3
    @Test
    void aPackageThatStartsNextMonthDoesNotCountYet() {
        Rider nour = rider("Nour Saleh", LEVEL_3, ahmed);
        pay(nour, DEC, 1, 8);

        assertThat(service.myRiders(ahmed.getId()).getFirst().balance())
                .isEqualTo(new BalanceDto(0, RiderPaymentStatus.AWAITING_PAYMENT, null));
    }

    // Case 4
    @Test
    void aVoidedPackageCountsForNothing() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Payment only = pay(sara, NOV, 1, 8);
        only.voidPayment(admin.getId(), "Entered by mistake", RECORDED_AT.plusSeconds(60));
        paymentRepository.saveAndFlush(only);

        assertThat(service.myRiders(ahmed.getId()).getFirst().balance())
                .isEqualTo(new BalanceDto(0, RiderPaymentStatus.AWAITING_PAYMENT, null));
    }

    // Case 5
    @Test
    void theBalanceIsTheSameTheAdminSeesOnW3() {
        Rider sara = rider("Sara Mahmoud", LEVEL_3, ahmed);
        Rider omar = rider("Omar Ali", LEVEL_3, ahmed);
        pay(sara, NOV, 1, 8);
        // Not valid until 1 December, so neither screen counts it today.
        pay(sara, DEC, 1, 8);

        List<MyRiderDto> mine = service.myRiders(ahmed.getId());
        List<AdminRiderRow> w3 = adminRows.rowsByIds(List.of(sara.getId(), omar.getId()));

        for (MyRiderDto row : mine) {
            AdminRiderRow adminRow = w3.stream().filter(r -> r.id().equals(row.id())).findFirst().orElseThrow();
            assertThat(row.balance()).isEqualTo(adminRow.balance());
        }
        assertThat(mine).extracting(MyRiderDto::balance).containsExactly(
                new BalanceDto(0, RiderPaymentStatus.AWAITING_PAYMENT, null),
                new BalanceDto(8, RiderPaymentStatus.ACTIVE, LocalDate.of(2026, 12, 31)));
    }

    private Payment pay(Rider rider, YearMonth start, int months, int sessions) {
        return paymentRepository.saveAndFlush(Payment.record(rider, new BigDecimal("1200"), PaymentMethod.CASH,
                start, months, sessions, admin.getId(), RECORDED_AT));
    }

    private AdminAccount admin() {
        AdminAccount account = new AdminAccount();
        account.setEmail("my-riders-test-admin@example.com");
        account.setPasswordHash("hash");
        return account;
    }

    private Assignment assign(Session session, Rider rider, Trainer by) {
        return assignmentRepository.saveAndFlush(Assignment.of(session, rider, by));
    }

    private Session session(short levelId, LocalDate date, LocalTime start) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(date.getDayOfWeek());
        template.setStartTime(start);
        template.setEndTime(start.plusHours(1));
        template.setCapacity(12);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        template = sessionTemplateRepository.saveAndFlush(template);
        return sessionRepository.saveAndFlush(Session.of(template, date));
    }

    private Rider rider(String name, short levelId, Trainer trainer) {
        Rider rider = new Rider();
        rider.setName(name);
        rider.setPhone("01012345678");
        rider.setLevel(levelRepository.getReferenceById(levelId));
        rider.setTrainer(trainer);
        return riderRepository.saveAndFlush(rider);
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
