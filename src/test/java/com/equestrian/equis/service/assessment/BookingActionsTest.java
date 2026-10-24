package com.equestrian.equis.service.assessment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The W2 row actions per display status.
class BookingActionsTest {
    @Test
    void eachStatusHasItsActions() {
        assertThat(BookingActions.allowed("upcoming", true, false)).containsExactly(
                BookingAction.RESCHEDULE, BookingAction.CANCEL, BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("waiting", true, false)).containsExactly(
                BookingAction.CHANGE_ASSESSOR, BookingAction.REMIND, BookingAction.EDIT);
        assertThat(BookingActions.allowed("waiting", false, false)).containsExactly(
                BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("noshow", true, false)).containsExactly(
                BookingAction.REBOOK, BookingAction.CHANGE_ASSESSOR, BookingAction.CLOSE, BookingAction.EDIT);
        assertThat(BookingActions.allowed("cancelled", true, false)).containsExactly(
                BookingAction.REBOOK, BookingAction.CHANGE_ASSESSOR, BookingAction.CLOSE, BookingAction.EDIT);
        assertThat(BookingActions.allowed("placed", true, false)).containsExactly(BookingAction.VIEW_RIDER);
    }

    // E11 · Admin edits a person's details, case 9.
    @ParameterizedTest
    @ValueSource(strings = {"upcoming", "waiting", "noshow", "cancelled"})
    void everyRowButPlacedCanBeEdited(String status) {
        assertThat(BookingActions.allowed(status, true, false)).contains(BookingAction.EDIT);
        assertThat(BookingActions.allowed(status, false, false)).contains(BookingAction.EDIT);
    }

    // E11 · Admin closes or reopens a person: a closed person's row only allows reopen and edit.
    @ParameterizedTest
    @ValueSource(strings = {"upcoming", "waiting", "noshow", "cancelled", "placed"})
    void aClosedPersonsRowOnlyAllowsReopenAndEdit(String status) {
        assertThat(BookingActions.allowed(status, true, true)).containsExactly(BookingAction.REOPEN, BookingAction.EDIT);
        assertThat(BookingActions.allowed(status, false, true)).containsExactly(BookingAction.REOPEN, BookingAction.EDIT);
    }

    // E11 · Admin closes or reopens a person: close only once nothing is booked, and never a rider.
    @Test
    void onlyANoShowOrCancelledRowCanBeClosed() {
        assertThat(BookingActions.allowed("noshow", false, false)).contains(BookingAction.CLOSE);
        assertThat(BookingActions.allowed("cancelled", false, false)).contains(BookingAction.CLOSE);
        for (String status : new String[] {"upcoming", "waiting", "placed"}) {
            assertThat(BookingActions.allowed(status, true, false)).as(status).doesNotContain(BookingAction.CLOSE);
        }
        assertThat(BookingActions.allowed("noshow", true, true)).doesNotContain(BookingAction.CLOSE);
        for (String status : new String[] {"upcoming", "waiting", "noshow", "cancelled", "placed"}) {
            assertThat(BookingActions.allowed(status, true, false)).as(status).doesNotContain(BookingAction.REOPEN);
        }
        assertThat(BookingAction.CLOSE.value()).isEqualTo("close");
        assertThat(BookingAction.REOPEN.value()).isEqualTo("reopen");
    }

    // E11 · Admin manages an assessment: cancel only before the slot starts, and not for a closed person.
    @Test
    void onlyAnUpcomingRowCanBeCancelled() {
        assertThat(BookingActions.allowed("upcoming", true, false)).contains(BookingAction.CANCEL);
        assertThat(BookingActions.allowed("upcoming", false, false)).contains(BookingAction.CANCEL);
        for (String status : new String[] {"waiting", "noshow", "cancelled", "placed"}) {
            assertThat(BookingActions.allowed(status, true, false)).as(status).doesNotContain(BookingAction.CANCEL);
        }
        assertThat(BookingActions.allowed("upcoming", true, true)).doesNotContain(BookingAction.CANCEL);
        assertThat(BookingAction.CANCEL.value()).isEqualTo("cancel");
    }

    @Test
    void aPlacedRowCannotBeEdited() {
        assertThat(BookingActions.allowed("placed", true, false)).doesNotContain(BookingAction.EDIT);
    }

    @Test
    void anUnknownStatusIsABug() {
        assertThatThrownBy(() -> BookingActions.allowed("lost", true, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editIsSentLowercase() {
        assertThat(BookingAction.EDIT.value()).isEqualTo("edit");
    }
}
