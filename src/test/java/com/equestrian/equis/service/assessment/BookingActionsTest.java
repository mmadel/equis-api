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
                BookingAction.RESCHEDULE, BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("waiting", true, false)).containsExactly(
                BookingAction.CHANGE_ASSESSOR, BookingAction.REMIND, BookingAction.EDIT);
        assertThat(BookingActions.allowed("waiting", false, false)).containsExactly(
                BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("noshow", true, false)).containsExactly(
                BookingAction.REBOOK, BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("cancelled", true, false)).containsExactly(
                BookingAction.REBOOK, BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("placed", true, false)).containsExactly(BookingAction.VIEW_RIDER);
    }

    // E11 · Admin edits a person's details, case 9.
    @ParameterizedTest
    @ValueSource(strings = {"upcoming", "waiting", "noshow", "cancelled"})
    void everyRowButPlacedCanBeEdited(String status) {
        assertThat(BookingActions.allowed(status, true, false)).contains(BookingAction.EDIT);
        assertThat(BookingActions.allowed(status, false, false)).contains(BookingAction.EDIT);
    }

    // E11 · Admin sees all applicants: a closed person's row only allows edit.
    @ParameterizedTest
    @ValueSource(strings = {"upcoming", "waiting", "noshow", "cancelled", "placed"})
    void aClosedPersonsRowOnlyAllowsEdit(String status) {
        assertThat(BookingActions.allowed(status, true, true)).containsExactly(BookingAction.EDIT);
        assertThat(BookingActions.allowed(status, false, true)).containsExactly(BookingAction.EDIT);
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
