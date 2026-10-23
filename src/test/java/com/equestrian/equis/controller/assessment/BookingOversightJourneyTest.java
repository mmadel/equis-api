package com.equestrian.equis.controller.assessment;

import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.equestrian.equis.support.AdminOversightFixture.Schedule;
import com.equestrian.equis.support.MutableClock;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.FIVE_PM;
import static com.equestrian.equis.support.AdminOversightFixture.LEVEL_TWO;
import static com.equestrian.equis.support.AdminOversightFixture.SAT_17;
import static com.equestrian.equis.support.AdminOversightFixture.SAT_24;
import static com.equestrian.equis.support.AdminOversightFixture.TEN;
import static com.equestrian.equis.support.AdminOversightFixture.TUE_20;
import static com.equestrian.equis.support.AdminOversightFixture.read;
import static com.equestrian.equis.support.AdminOversightFixture.riderRow;
import static com.equestrian.equis.support.AdminOversightFixture.rowOf;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow W-C, booking journeys B1–B10: what the admin sees and changes on W2 and what the trainers then see in the
// app (A1 booking, A3 slot, A5 result). The single rules are in AdminBookingChangeApiTest and AdminBookingApiTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminOversightFixture.Config.class)
class BookingOversightJourneyTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void admin(DynamicPropertyRegistry registry) {
        TrainerAccountsFixture.adminAccount(registry);
    }

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(START);
    }

    @Autowired
    AdminOversightFixture fx;

    Coach ahmed;
    Coach mona;
    Schedule schedule;
    UUID sat17;
    UUID tue20;
    UUID sat24;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        schedule = fx.schedule();
        sat17 = fx.slotOn(SAT_17, TEN);
        tue20 = fx.slotOn(TUE_20, FIVE_PM);
        sat24 = fx.slotOn(SAT_24, TEN);
    }

    // B1
    @Test
    void aBookingMadeInTheAppShowsOnW2AsUpcoming() throws Exception {
        UUID sara = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);

        fx.w2()
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].id").value(sara.toString()))
                .andExpect(jsonPath("$.bookings[0].status").value("upcoming"))
                .andExpect(jsonPath("$.bookings[0].assessor.id").value(ahmed.id().toString()))
                .andExpect(jsonPath("$.bookings[0].actions", containsInAnyOrder("reschedule", "change_assessor", "edit")))
                .andExpect(jsonPath("$.totals.upcoming").value(1));
    }

    // B2
    @Test
    void onceTheSlotStartsTheBookingIsWaiting() throws Exception {
        UUID sara = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);
        fx.at(SAT_17, LocalTime.of(10, 5));

        fx.w2()
                .andExpect(jsonPath("$.bookings[0].status").value("waiting"))
                .andExpect(jsonPath("$.bookings[0].actions", containsInAnyOrder("change_assessor", "remind", "edit")))
                .andExpect(jsonPath("$.totals.waiting").value(1))
                .andExpect(jsonPath("$.totals.upcoming").value(0));
        fx.w2("waiting", null)
                .andExpect(jsonPath("$.bookings[*].id", contains(sara.toString())));
    }

    // B3
    @Test
    void aRescheduleMovesTheBookingBetweenTheTrainersSlots() throws Exception {
        UUID sara = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);

        fx.patchBooking(sara, "{ \"slotId\": \"%s\" }".formatted(tue20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sara.toString()));

        fx.slotBookings(ahmed, sat17)
                .andExpect(jsonPath("$.bookings[*].id", not(hasItem(sara.toString()))));
        fx.slotBookings(ahmed, tue20)
                .andExpect(jsonPath("$.bookings[*].id", contains(sara.toString())))
                .andExpect(jsonPath("$.bookings[0].assessorId").value(ahmed.id().toString()))
                .andExpect(jsonPath("$.bookings[0].mine").value(true));
    }

    // B4
    @Test
    void aRescheduleIntoAFullSlotIsRefused() throws Exception {
        fx.bookMember(ahmed, "First In", fx.nextPhone(), sat17);
        fx.bookMember(ahmed, "Second In", fx.nextPhone(), sat17);
        UUID third = fx.bookMember(ahmed, "Third Member", fx.nextPhone(), tue20);

        fx.patchBooking(third, "{ \"slotId\": \"%s\" }".formatted(sat17))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slot_full"));

        assertThat(fx.slotOf(third)).isEqualTo(tue20);
        assertThat(fx.upcomingIn(sat17)).isEqualTo(2);
    }

    // B5
    @Test
    void aNoShowIsRebookedWithAnotherAssessorAndPlacedByThem() throws Exception {
        NoShowRebooked omar = noShowRebookedWithMona();

        fx.w2(null, "Omar")
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].id").value(omar.fresh().toString()))
                .andExpect(jsonPath("$.bookings[0].status").value("upcoming"))
                .andExpect(jsonPath("$.bookings[0].assessor.id").value(mona.id().toString()))
                .andExpect(jsonPath("$.totals.noshow").value(0));
        assertThat(fx.bookingStatus(omar.old())).isEqualTo("noshow");

        fx.at(SAT_24, LocalTime.of(10, 30));
        fx.recordResult(ahmed, omar.fresh(), true, LEVEL_TWO)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("not_assessor"));
        String rider = read(fx.recordResult(mona, omar.fresh(), true, LEVEL_TWO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("placed")), "$.riderId");

        UUID riderId = UUID.fromString(rider);
        fx.w3()
                .andExpect(jsonPath(riderRow(riderId) + ".howAssigned").value("from_assessment"))
                .andExpect(jsonPath(riderRow(riderId) + ".trainer.id").value(mona.id().toString()))
                .andExpect(jsonPath(riderRow(riderId) + ".assessor.id").value(mona.id().toString()))
                .andExpect(jsonPath(riderRow(riderId) + ".levelId").value(LEVEL_TWO));
    }

    // B6
    @Test
    void theOldNoShowRowCannotBeRebookedAgain() throws Exception {
        NoShowRebooked omar = noShowRebookedWithMona();

        fx.patchBooking(omar.old(), "{ \"slotId\": \"%s\" }".formatted(tue20))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_superseded"));
        assertThat(fx.upcomingIn(tue20)).isZero();
    }

    // B7
    @Test
    void aBookingCancelledByATemplateDeleteIsRebookedFromW2() throws Exception {
        UUID sara = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);

        fx.deleteTemplate(schedule.saturdayAssessment())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBookings").value(1));
        fx.w2()
                .andExpect(jsonPath(rowOf(sara) + ".status").value("cancelled"))
                .andExpect(jsonPath(rowOf(sara) + ".actions[*]", hasItem("rebook")));

        // A few minutes later, so the new booking is the newer one; at the same instant W2 would pick by id.
        fx.at(LocalDate.of(2026, 10, 16), LocalTime.of(10, 5));
        String fresh = read(fx.patchBooking(sara, "{ \"slotId\": \"%s\" }".formatted(tue20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.slot.id").value(tue20.toString())), "$.id");

        assertThat(fresh).isNotEqualTo(sara.toString());
        assertThat(fx.bookingStatus(sara)).isEqualTo("cancelled");
        fx.w2().andExpect(jsonPath("$.bookings[*].id", contains(fresh)));
    }

    // B8
    @Test
    void aWaitingBookingCannotBeRescheduled() throws Exception {
        UUID sara = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);
        fx.at(SAT_17, LocalTime.of(10, 5));

        fx.patchBooking(sara, "{ \"slotId\": \"%s\" }".formatted(tue20))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("slot_started"));
        assertThat(fx.slotOf(sara)).isEqualTo(sat17);
    }

    // B9
    @Test
    void aPlacedBookingKeepsItsAssessorAndTheRiderTheirTrainer() throws Exception {
        UUID sara = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);
        fx.at(SAT_17, LocalTime.of(10, 30));
        UUID rider = UUID.fromString(read(fx.recordResult(ahmed, sara, true, LEVEL_TWO)
                .andExpect(status().isOk()), "$.riderId"));

        fx.patchBooking(sara, "{ \"assessorId\": \"%s\" }".formatted(mona.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("booking_closed"));

        assertThat(fx.assessorOf(sara)).isEqualTo(ahmed.id());
        assertThat(fx.trainerOf(rider)).isEqualTo(ahmed.id());
    }

    // B10
    @Test
    void anAdminRescheduleAndATrainerBookingRaceForTheLastSeat() throws Exception {
        UUID sara = fx.bookMember(ahmed, "Sara Ali", fx.nextPhone(), sat17);
        for (int i = 1; i <= 3; i++) {
            fx.bookMember(ahmed, "Seat Taker " + i, fx.nextPhone(), tue20);
        }
        String walkInPhone = fx.nextPhone();
        fx.accounts().adminToken();

        List<String> outcomes = AdminOversightFixture.raceOutcomes(
                () -> AdminOversightFixture.outcome(
                        fx.patchBooking(sara, "{ \"slotId\": \"%s\" }".formatted(tue20)).andReturn()),
                () -> fx.bookMemberOutcome(mona, "Walk In", walkInPhone, tue20));

        assertThat(outcomes).containsExactlyInAnyOrder("ok", "slot_full");
        assertThat(fx.upcomingIn(tue20)).isEqualTo(4);
        assertThat(fx.upcomingIn(sat17)).isEqualTo(outcomes.get(0).equals("ok") ? 0 : 1);
    }

    private record NoShowRebooked(UUID old, UUID fresh) {
    }

    // Ahmed books Omar for Sat 17 Oct, records a no-show at 10:30, and the admin rebooks him for Sat 24 Oct with Mona.
    private NoShowRebooked noShowRebookedWithMona() throws Exception {
        UUID old = fx.bookMember(ahmed, "Omar Hassan", fx.nextPhone(), sat17);
        fx.at(SAT_17, LocalTime.of(10, 30));
        fx.recordResult(ahmed, old, false, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("noshow"));
        fx.w2()
                .andExpect(jsonPath(rowOf(old) + ".status").value("noshow"))
                .andExpect(jsonPath(rowOf(old) + ".actions[*]", hasItem("rebook")));

        UUID fresh = UUID.fromString(read(fx.patchBooking(old,
                        "{ \"slotId\": \"%s\", \"assessorId\": \"%s\" }".formatted(sat24, mona.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("upcoming"))
                .andExpect(jsonPath("$.assessor.id").value(mona.id().toString())), "$.id"));
        assertThat(fresh).isNotEqualTo(old);
        return new NoShowRebooked(old, fresh);
    }
}
