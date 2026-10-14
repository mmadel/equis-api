package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.assessment.RecordResultRequest;
import com.equestrian.equis.dto.assessment.ResultRecorded;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({RecordResultService.class, AssessmentRules.class})
public class RecordResultServiceTest {
    // The clock is fixed on this Monday at 13:00 school time (Africa/Cairo).
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    RecordResultService service;
    @Autowired
    AssessmentBookingRepository bookingRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    RiderRepository riderRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;
    @Autowired
    EntityManager em;

    Trainer sara;
    AuthUser saraUser;

    @BeforeEach
    void setUp() {
        sara = trainerRepository.saveAndFlush(trainer("Sara Ali", "01999990001", "record-result-test-sara@example.com"));
        saraUser = new AuthUser(sara.getId(), Role.TRAINER);
    }

    @Test
    void attendedPlacesTheMemberAndCreatesTheirRiderWithTheAssessorAsTrainer() {
        AssessmentBooking booking = startedBooking("01012345678");
        long versionBefore = booking.getVersion();

        ResultRecorded result = service.record(booking.getId(), saraUser,
                new RecordResultRequest(true, 3, "  Good seat "));

        assertThat(result.bookingId()).isEqualTo(booking.getId());
        assertThat(result.status()).isEqualTo("placed");
        assertThat(result.riderId()).isNotNull();
        em.flush();
        em.clear();

        AssessmentBooking saved = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PLACED);
        assertThat(saved.getAttended()).isTrue();
        assertThat(saved.getLevel().getId()).isEqualTo((short) 3);
        assertThat(saved.getNotes()).isEqualTo("Good seat");
        assertThat(saved.getRecordedAt()).isEqualTo(NOW);
        assertThat(saved.getVersion()).isEqualTo(versionBefore + 1);

        Rider rider = riderRepository.findById(result.riderId()).orElseThrow();
        assertThat(rider.getName()).isEqualTo("Mona Hassan");
        assertThat(rider.getPhone()).isEqualTo("01012345678");
        assertThat(rider.getLevel().getId()).isEqualTo((short) 3);
        assertThat(rider.getTrainer().getId()).isEqualTo(sara.getId());
        assertThat(rider.getMemberId()).isEqualTo(saved.getMember().getId());
    }

    @Test
    void noShowRecordsTheResultWithoutARider() {
        AssessmentBooking booking = startedBooking(null);
        long ridersBefore = riderRepository.count();

        ResultRecorded result = service.record(booking.getId(), saraUser, new RecordResultRequest(false, null, null));

        assertThat(result).isEqualTo(new ResultRecorded(booking.getId(), "noshow", null));
        em.flush();
        em.clear();
        AssessmentBooking saved = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.NOSHOW);
        assertThat(saved.getAttended()).isFalse();
        assertThat(saved.getLevel()).isNull();
        assertThat(riderRepository.count()).isEqualTo(ridersBefore);
    }

    @Test
    void theAdminMayRecordAnyBooking() {
        AssessmentBooking booking = startedBooking(null);

        ResultRecorded result = service.record(booking.getId(), new AuthUser(UUID.randomUUID(), Role.ADMIN),
                new RecordResultRequest(true, 1, null));

        assertThat(result.status()).isEqualTo("placed");
    }

    @Test
    void aSecondResultIsRefused() {
        AssessmentBooking booking = startedBooking(null);
        service.record(booking.getId(), saraUser, new RecordResultRequest(true, 2, null));
        em.flush();

        assertRefused(() -> service.record(booking.getId(), saraUser, new RecordResultRequest(false, null, null)),
                HttpStatus.CONFLICT, ErrorCodes.RESULT_ALREADY_RECORDED);
    }

    @Test
    void theOneRiderPerMemberKeyIsTheSame409() {
        AssessmentBooking booking = startedBooking(null);
        // A rider for the member already exists, as if a racing Save had got past the lock.
        riderRepository.saveAndFlush(Rider.fromAssessment(booking.getMember(), levelRepository.getReferenceById((short) 1), sara));

        assertRefused(() -> service.record(booking.getId(), saraUser, new RecordResultRequest(true, 2, null)),
                HttpStatus.CONFLICT, ErrorCodes.RESULT_ALREADY_RECORDED);
    }

    @Test
    void unknownBookingIsNotFound() {
        assertRefused(() -> service.record(UUID.randomUUID(), saraUser, new RecordResultRequest(false, null, null)),
                HttpStatus.NOT_FOUND, ErrorCodes.BOOKING_NOT_FOUND);
    }

    @Test
    void inconsistentRequestIsABadRequestBeforeTheBookingIsLookedUp() {
        assertRefused(() -> service.record(UUID.randomUUID(), saraUser, new RecordResultRequest(true, null, null)),
                HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED);
    }

    @Test
    void anotherTrainerIsNotTheAssessor() {
        AssessmentBooking booking = startedBooking(null);
        Trainer omar = trainerRepository.saveAndFlush(trainer("Omar Nabil", "01999990002", "record-result-test-omar@example.com"));

        assertRefused(() -> service.record(booking.getId(), new AuthUser(omar.getId(), Role.TRAINER),
                new RecordResultRequest(false, null, null)), HttpStatus.FORBIDDEN, ErrorCodes.NOT_ASSESSOR);
    }

    // A booking in an assessment slot that started a week ago, assessed by Sara.
    private AssessmentBooking startedBooking(String phone) {
        Session slot = sessionRepository.saveAndFlush(Session.of(assessmentTemplate(), TODAY.minusWeeks(1)));
        Member member = memberRepository.saveAndFlush(Member.register("Mona Hassan", phone));
        return bookingRepository.saveAndFlush(AssessmentBooking.book(member, slot, sara));
    }

    private void assertRefused(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
        });
    }

    private SessionTemplate assessmentTemplate() {
        SessionTemplate template = new SessionTemplate();
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(4);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    private Trainer trainer(String name, String phone, String email) {
        Trainer trainer = new Trainer();
        trainer.setName(name);
        trainer.setPhone(phone);
        trainer.setEmail(email);
        return trainer;
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
