package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.Constraints;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.AuthUser;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.dto.assessment.RecordResultRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
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
import com.equestrian.equis.support.TestMembers;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(AssessmentRules.class)
public class AssessmentRulesTest {
    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Africa/Cairo");
    // A Monday; the slots start at 16:00 school time.
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    AssessmentRules rules;
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

    Trainer sara;
    Trainer omar;
    Session slot;
    Instant start;

    @BeforeEach
    void setUp() {
        sara = trainer("Sara Ali", "01999990001", "assessment-rules-test-sara@example.com");
        omar = trainer("Omar Hany", "01999990002", "assessment-rules-test-omar@example.com");
        slot = sessionRepository.saveAndFlush(Session.of(template(), MONDAY));
        start = slot.startsAt(SCHOOL_ZONE).toInstant();
    }

    // ---- booking an existing member ----

    @Test
    void memberWithNoActiveBookingCanBook() {
        Member member = member("01011111111");
        AssessmentBooking old = book(member, sara);
        old.recordNoShow(null, start);
        bookingRepository.saveAndFlush(old);

        assertThatCode(() -> rules.requireCanBook(member)).doesNotThrowAnyException();
    }

    @Test
    void memberWithAnUpcomingBookingIsToldWhichSlot() {
        Member member = member("01011111111");
        book(member, sara);

        assertThatThrownBy(() -> rules.requireCanBook(member)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.code()).isEqualTo(ErrorCodes.MEMBER_ALREADY_BOOKED);
            assertThat(ex.getMessage()).isEqualTo("Booked Mon 5 Oct, 16:00");
        });
    }

    @Test
    void memberAlreadyPlacedAsARiderCannotBook() {
        Member member = member("01011111111");
        Rider rider = new Rider();
        rider.setName("Mona Hassan");
        rider.setLevel(levelRepository.getReferenceById((short) 1));
        rider.setTrainer(sara);
        rider.setMemberId(member.getId());
        riderRepository.saveAndFlush(rider);

        assertRefused(() -> rules.requireCanBook(member), HttpStatus.CONFLICT, ErrorCodes.MEMBER_PLACED);
    }

    @Test
    void duplicatePhoneIsReportedByItsConstraintName() {
        member("01011111111");

        assertThatThrownBy(() -> memberRepository.saveAndFlush(TestMembers.member("Hady", "01011111111")))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class,
                        ex -> assertThat(Constraints.nameOf(ex)).contains("uq_member_phone"));
    }

    // ---- result ----

    @Test
    void attendedNeedsALevelInRange() {
        assertThatCode(() -> rules.requireConsistentResult(new RecordResultRequest(true, 3, null)))
                .doesNotThrowAnyException();
        assertInvalidLevel(new RecordResultRequest(true, null, null), ErrorCodes.LEVEL_REQUIRED);
        assertInvalidLevel(new RecordResultRequest(true, 0, null), ErrorCodes.LEVEL_REQUIRED);
        assertInvalidLevel(new RecordResultRequest(true, 7, null), ErrorCodes.LEVEL_REQUIRED);
    }

    @Test
    void noShowTakesNoLevel() {
        assertThatCode(() -> rules.requireConsistentResult(new RecordResultRequest(false, null, "  ")))
                .doesNotThrowAnyException();
        assertInvalidLevel(new RecordResultRequest(false, 2, null), ErrorCodes.LEVEL_NOT_ALLOWED);
    }

    @Test
    void assessorAndAdminCanRecordFromTheStartTime() {
        AssessmentBooking booking = book(member(null), sara);

        assertThatCode(() -> rules.requireCanRecord(booking, new AuthUser(sara.getId(), Role.TRAINER), start))
                .doesNotThrowAnyException();
        assertThatCode(() -> rules.requireCanRecord(booking, new AuthUser(UUID.randomUUID(), Role.ADMIN), start))
                .doesNotThrowAnyException();
    }

    @Test
    void anotherTrainerCannotRecord() {
        AssessmentBooking booking = book(member(null), sara);

        assertRefused(() -> rules.requireCanRecord(booking, new AuthUser(omar.getId(), Role.TRAINER), start),
                HttpStatus.FORBIDDEN, ErrorCodes.NOT_ASSESSOR);
    }

    @Test
    void resultCannotBeRecordedBeforeTheSlotStarts() {
        AssessmentBooking booking = book(member(null), sara);

        assertRefused(() -> rules.requireCanRecord(booking, new AuthUser(sara.getId(), Role.TRAINER),
                start.minusSeconds(1)), HttpStatus.CONFLICT, ErrorCodes.SLOT_NOT_STARTED);
    }

    @Test
    void cancelledAndRecordedBookingsAreRefused() {
        AssessmentBooking cancelled = book(member(null), sara);
        cancelled.cancel(start);
        AssessmentBooking placed = book(member(null), sara);
        placed.recordAttended(levelRepository.getReferenceById((short) 2), null, start);
        AssessmentBooking noShow = book(member(null), sara);
        noShow.recordNoShow(null, start);
        AuthUser admin = new AuthUser(UUID.randomUUID(), Role.ADMIN);

        assertRefused(() -> rules.requireCanRecord(cancelled, admin, start),
                HttpStatus.CONFLICT, ErrorCodes.BOOKING_CANCELLED);
        assertRefused(() -> rules.requireCanRecord(placed, admin, start),
                HttpStatus.CONFLICT, ErrorCodes.RESULT_ALREADY_RECORDED);
        assertRefused(() -> rules.requireCanRecord(noShow, admin, start),
                HttpStatus.CONFLICT, ErrorCodes.RESULT_ALREADY_RECORDED);
    }

    private void assertInvalidLevel(RecordResultRequest req, String message) {
        assertThatThrownBy(() -> rules.requireConsistentResult(req)).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.code()).isEqualTo(ErrorCodes.VALIDATION_FAILED);
            assertThat(ex.properties().get("fieldErrors"))
                    .asInstanceOf(InstanceOfAssertFactories.list(ApiFieldError.class))
                    .containsExactly(new ApiFieldError("levelId", message));
        });
    }

    private void assertRefused(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
        });
    }

    private AssessmentBooking book(Member member, Trainer assessor) {
        return bookingRepository.saveAndFlush(AssessmentBooking.book(member, slot, assessor));
    }

    private Member member(String phone) {
        return memberRepository.saveAndFlush(TestMembers.member("Mona Hassan", phone));
    }

    // An assessment template: no level.
    private SessionTemplate template() {
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
        return trainerRepository.saveAndFlush(trainer);
    }
}
