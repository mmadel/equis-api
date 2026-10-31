package com.equestrian.equis.controller.riders;

import com.equestrian.equis.support.AdminOversightFixture;
import com.equestrian.equis.support.AdminOversightFixture.Coach;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.FIVE_PM;
import static com.equestrian.equis.support.AdminOversightFixture.LEVEL_TWO;
import static com.equestrian.equis.support.AdminOversightFixture.SIX_PM;
import static com.equestrian.equis.support.AdminOversightFixture.TUE_20;
import static com.equestrian.equis.support.AdminOversightFixture.riderRow;
import static com.equestrian.equis.support.TrainerAccountsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow W-D, reassignment journeys R1–R7: the admin moves riders between trainers on W3 and the trainers see it in
// My riders (4) and in a session's seats (3). Riders come from real assessments, so each has an assessor, except the
// seed riders R5 and R7 need. The single rules are in RiderReassignApiTest and AdminRiderApiTest.
@SpringBootTest(properties = "app.sessions.generate-cron=-")
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminOversightFixture.Config.class)
class RiderReassignJourneyTest {
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
    Coach karim;
    UUID tue20;
    UUID levelTwo;

    @BeforeEach
    void setUp() throws Exception {
        fx.reset();
        ahmed = fx.coach("Ahmed Said");
        mona = fx.coach("Mona Kamal");
        karim = fx.coach("Karim Nabil");
        fx.schedule();
        tue20 = fx.slotOn(TUE_20, FIVE_PM);
        levelTwo = fx.slotOn(TUE_20, SIX_PM);
    }

    // R1. A seat belongs to the trainer who gave it (E3), so after the move it stays Ahmed's: Mona sees it as another
    // coach's seat and the rider as hers but not in this session, and her save leaves the seat alone.
    @Test
    void aMovedRiderShowsInTheNewTrainersAppAndKeepsTheirSeat() throws Exception {
        UUID sara = placedAndSeatedByAhmedThenMovedToMona();

        fx.w3()
                .andExpect(jsonPath(riderRow(sara) + ".howAssigned").value("changed_manually"))
                .andExpect(jsonPath(riderRow(sara) + ".canResetToAssessor").value(true))
                .andExpect(jsonPath(riderRow(sara) + ".trainer.id").value(mona.id().toString()))
                .andExpect(jsonPath(riderRow(sara) + ".assessor.id").value(ahmed.id().toString()));
        fx.myRiders(mona).andExpect(jsonPath("$[*].id", contains(sara.toString())));
        fx.myRiders(ahmed).andExpect(jsonPath("$[*].id", not(hasItem(sara.toString()))));

        fx.session(mona, levelTwo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherCoachSeats").value(1))
                .andExpect(jsonPath("$.mySeats").isEmpty())
                .andExpect(jsonPath("$.riders[*].id", contains(sara.toString())))
                .andExpect(jsonPath("$.riders[0].inThisSession").value(false));
        fx.assignToSession(mona, levelTwo, List.of()).andExpect(status().isOk());
        assertThat(seatsIn(levelTwo)).containsExactly(Map.of("rider_id", sara, "assigned_by", ahmed.id()));
    }

    // R2. Another trainer's rider looks the same as a missing one (E5), so Ahmed gets 404, not 403.
    @Test
    void onlyTheNewTrainerCanChangeTheMovedRidersLevel() throws Exception {
        UUID sara = placedAndSeatedByAhmedThenMovedToMona();

        fx.promote(ahmed, sara, LEVEL_TWO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));
        fx.promote(mona, sara, LEVEL_TWO).andExpect(status().isOk());

        assertThat(fx.trainerOf(sara)).isEqualTo(mona.id());
        fx.w3().andExpect(jsonPath(riderRow(sara) + ".levelId").value(LEVEL_TWO + 1))
                .andExpect(jsonPath(riderRow(sara) + ".trainer.id").value(mona.id().toString()));
    }

    // R3
    @Test
    void resetToAssessorGivesTheRiderBackToAhmed() throws Exception {
        UUID sara = placedAndSeatedByAhmedThenMovedToMona();

        fx.resetToAssessor(List.of(sara))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riders[0].trainer.id").value(ahmed.id().toString()))
                .andExpect(jsonPath("$.riders[0].howAssigned").value("from_assessment"));

        fx.w3().andExpect(jsonPath(riderRow(sara) + ".howAssigned").value("from_assessment"));
        fx.myRiders(ahmed).andExpect(jsonPath("$[*].id", contains(sara.toString())));
        fx.myRiders(mona).andExpect(jsonPath("$[*].id", not(hasItem(sara.toString()))));
    }

    // R4. The coach leaves: their riders show as unassigned and go to Mona in one go, and stay there.
    @Test
    void aDisabledTrainersRidersAreBulkAssignedAndStayAfterEnable() throws Exception {
        List<UUID> riders = fx.placedRiders(karim, tue20, LEVEL_TWO, "Rider A", "Rider B", "Rider C");
        fx.disable(karim).andExpect(status().isOk());

        fx.w3(Map.of("unassigned", "true"))
                .andExpect(jsonPath("$.riders[*].id", containsInAnyOrder(ids(riders))))
                .andExpect(jsonPath("$.riders[*].howAssigned", everyItem(is("unassigned"))))
                .andExpect(jsonPath("$.totals.unassigned").value(3));

        fx.assignRiders(riders, mona.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(3));
        fx.w3()
                .andExpect(jsonPath("$.riders[*].howAssigned", everyItem(is("changed_manually"))))
                .andExpect(jsonPath("$.riders[*].trainer.id", everyItem(is(mona.id().toString()))))
                .andExpect(jsonPath("$.totals.unassigned").value(0));

        fx.enable(karim).andExpect(status().isOk());
        assertThat(riders).allSatisfy(id -> assertThat(fx.trainerOf(id)).isEqualTo(mona.id()));
        fx.w3(Map.of("unassigned", "true")).andExpect(jsonPath("$.riders").isEmpty());
    }

    // R5
    @Test
    void aSeedRiderHasNoAssessorToGoBackTo() throws Exception {
        UUID seed = fx.accounts().addRiders(ahmed.id(), LEVEL_TWO, 1).getFirst();
        UUID omar = fx.placedRiders(ahmed, tue20, LEVEL_TWO, "Omar Hassan").getFirst();
        fx.assignRiders(List.of(omar), mona.id()).andExpect(status().isOk());

        fx.w3()
                .andExpect(jsonPath(riderRow(seed) + ".howAssigned").value("set_manually"))
                .andExpect(jsonPath(riderRow(seed) + ".canResetToAssessor").value(false));
        fx.resetToAssessor(List.of(seed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("no_active_assessor"));
        fx.resetToAssessor(List.of(seed, omar))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("no_active_assessor"));

        assertThat(fx.trainerOf(seed)).isEqualTo(ahmed.id());
        assertThat(fx.trainerOf(omar)).isEqualTo(mona.id());
    }

    // R6
    @Test
    void aDisabledTargetOrAnUnknownRiderChangesNothing() throws Exception {
        UUID sara = fx.placedRiders(ahmed, tue20, LEVEL_TWO, "Sara Ali").getFirst();
        fx.disable(karim).andExpect(status().isOk());

        fx.assignRiders(List.of(sara), karim.id())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("trainer_not_active"));
        fx.assignRiders(List.of(sara, UUID.randomUUID()), mona.id())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("rider_not_found"));

        assertThat(fx.trainerOf(sara)).isEqualTo(ahmed.id());
        fx.w3().andExpect(jsonPath(riderRow(sara) + ".howAssigned").value("from_assessment"));
    }

    // R7
    @Test
    void aBulkAssignRacingTheTargetsDisableEndsConsistent() throws Exception {
        List<UUID> riders = fx.accounts().addRiders(karim.id(), LEVEL_TWO, 5);
        fx.accounts().addRiders(mona.id(), LEVEL_TWO, 1);
        fx.accounts().adminToken();

        List<String> outcomes = AdminOversightFixture.race(
                () -> fx.assignRiders(riders, mona.id()).andReturn(),
                () -> fx.disable(mona).andReturn());

        assertThat(outcomes.get(1)).isEqualTo("ok");
        if (outcomes.get(0).equals("ok")) {
            // The assign went first: the riders are Mona's, and with Mona disabled they need a trainer again.
            assertThat(riders).allSatisfy(id -> assertThat(fx.trainerOf(id)).isEqualTo(mona.id()));
            fx.w3(Map.of("unassigned", "true"))
                    .andExpect(jsonPath("$.riders[*].id", hasItem(riders.getFirst().toString())))
                    .andExpect(jsonPath("$.totals.unassigned").value(6));
        } else {
            assertThat(outcomes.get(0)).isEqualTo("trainer_not_active");
            assertThat(riders).allSatisfy(id -> assertThat(fx.trainerOf(id)).isEqualTo(karim.id()));
        }
    }

    // Ahmed places Sara at Level 2 on Tue 20 Oct 17:00 and seats her in Level 2 at 18:00 the same evening; the admin
    // then gives her to Mona. She has an October package, since only riders with a session left can be seated.
    private UUID placedAndSeatedByAhmedThenMovedToMona() throws Exception {
        UUID sara = fx.placedRiders(ahmed, tue20, LEVEL_TWO, "Sara Ali").getFirst();
        fx.jdbc().update("""
                        insert into payment (rider_id, amount_egp, method, start_month, months, sessions,
                                             valid_until, recorded_by)
                        values (?, 1200, 'cash', date '2026-10-01', 1, 8, date '2026-11-30',
                                (select id from admin_account limit 1))""",
                sara);
        fx.assignToSession(ahmed, levelTwo, List.of(sara)).andExpect(status().isOk());
        fx.assignRiders(List.of(sara), mona.id()).andExpect(status().isOk());
        return sara;
    }

    private List<Map<String, Object>> seatsIn(UUID sessionId) {
        return fx.jdbc().queryForList("select rider_id, assigned_by from assignment where session_id = ?",
                sessionId);
    }

    private static String[] ids(List<UUID> ids) {
        return ids.stream().map(UUID::toString).toArray(String[]::new);
    }
}
