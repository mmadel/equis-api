package com.equestrian.equis.service.assessment;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AdminNewMemberRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.entity.assessment.Member;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.entity.sessions.SessionTemplate;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.assessment.MemberRepository;
import com.equestrian.equis.repository.levels.LevelRepository;
import com.equestrian.equis.repository.sessions.SessionRepository;
import com.equestrian.equis.repository.sessions.SessionTemplateRepository;
import com.equestrian.equis.support.TestMembers;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

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
@Import({MemberBookingService.class, AssessmentRules.class})
public class MemberBookingServiceTest {
    // The clock is fixed on this Monday at 13:00 school time (Africa/Cairo).
    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    MemberBookingService service;
    @Autowired
    AssessmentBookingRepository bookingRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    SessionTemplateRepository sessionTemplateRepository;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    LevelRepository levelRepository;

    Trainer sara;

    @BeforeEach
    void setUp() {
        sara = trainerRepository.saveAndFlush(trainer());
    }

    @Test
    void registersTheMemberAndBooksThemWithTheChosenAssessor() {
        Session slot = slot(null, TODAY, 4);

        AssessmentBooking booking = service.registerAndBook(
                new AdminNewMemberRequest("  Mona Hassan ", "01012345678", " Mona@Mail.com ",
                        slot.getId(), sara.getId()));

        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.UPCOMING);
        assertThat(booking.getSession()).isEqualTo(slot);
        assertThat(booking.getAssessor().getId()).isEqualTo(sara.getId());
        Member member = memberRepository.findById(booking.getMember().getId()).orElseThrow();
        assertThat(member.getName()).isEqualTo("Mona Hassan");
        assertThat(member.getPhone()).isEqualTo("01012345678");
        assertThat(member.getEmail()).isEqualTo("mona@mail.com");
    }

    @Test
    void unknownSlotAndLevelSessionAreNotFound() {
        Session levelSession = slot((short) 1, TODAY, 4);

        assertRefused(() -> book(UUID.randomUUID(), null), HttpStatus.NOT_FOUND, ErrorCodes.SLOT_NOT_FOUND);
        assertRefused(() -> book(levelSession.getId(), null), HttpStatus.NOT_FOUND, ErrorCodes.SLOT_NOT_FOUND);
    }

    @Test
    void startedAndCancelledSlotsAreClosed() {
        Session past = slot(null, TODAY.minusWeeks(1), 4);
        Session cancelled = Session.of(template(null, 4), TODAY.plusWeeks(1));
        cancelled.cancel(NOW);
        sessionRepository.saveAndFlush(cancelled);

        assertRefused(() -> book(past.getId(), null), HttpStatus.CONFLICT, ErrorCodes.SLOT_CLOSED);
        assertRefused(() -> book(cancelled.getId(), null), HttpStatus.CONFLICT, ErrorCodes.SLOT_CLOSED);
    }

    @Test
    void fullSlotIsRefusedButCancelledBookingsFreeTheirSeat() {
        Session slot = slot(null, TODAY, 1);
        AssessmentBooking taken = book(slot.getId(), null);

        assertRefused(() -> book(slot.getId(), null), HttpStatus.CONFLICT, ErrorCodes.SLOT_FULL);

        taken.cancel(NOW);
        bookingRepository.saveAndFlush(taken);
        assertThat(book(slot.getId(), null).getStatus()).isEqualTo(BookingStatus.UPCOMING);
    }

    @Test
    void registeredPhoneIsTakenWithAFieldErrorAndTheExistingPerson() {
        Session slot = slot(null, TODAY, 4);
        Member existing = book(slot.getId(), "01012345678").getMember();

        assertThatThrownBy(() -> book(slot.getId(), " 01012345678 "))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.code()).isEqualTo(ErrorCodes.PHONE_TAKEN);
                    assertThat(ex.properties().get("fieldErrors"))
                            .asInstanceOf(InstanceOfAssertFactories.list(ApiFieldError.class))
                            .containsExactly(new ApiFieldError("phone", ErrorCodes.PHONE_TAKEN));
                    assertThat(ex.properties())
                            .containsEntry("existingMemberId", existing.getId())
                            .containsEntry("existingMemberName", "Mona Hassan")
                            .containsEntry("existingClosed", false);
                });
    }

    // A null phone gets a made-up unique one; the email is always made up.
    private AssessmentBooking book(UUID slotId, String phone) {
        return service.registerAndBook(new AdminNewMemberRequest("Mona Hassan",
                phone != null ? phone : TestMembers.nextPhone(), TestMembers.nextEmail(), slotId, sara.getId()));
    }

    private void assertRefused(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
        });
    }

    private Session slot(Short levelId, LocalDate date, int capacity) {
        return sessionRepository.saveAndFlush(Session.of(template(levelId, capacity), date));
    }

    // A null levelId makes an assessment template.
    private SessionTemplate template(Short levelId, int capacity) {
        SessionTemplate template = new SessionTemplate();
        template.setLevel(levelId == null ? null : levelRepository.getReferenceById(levelId));
        template.setDayOfWeek(DayOfWeek.MONDAY);
        template.setStartTime(LocalTime.of(16, 0));
        template.setEndTime(LocalTime.of(17, 0));
        template.setCapacity(capacity);
        template.setValidFrom(LocalDate.of(2026, 9, 1));
        return sessionTemplateRepository.saveAndFlush(template);
    }

    private Trainer trainer() {
        Trainer trainer = new Trainer();
        trainer.setName("Sara Ali");
        trainer.setPhone("01999990001");
        trainer.setEmail("member-booking-test-sara@example.com");
        trainer.setStatus(TrainerStatus.ACTIVE);
        trainer.setPasswordHash("not-a-real-hash");
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
