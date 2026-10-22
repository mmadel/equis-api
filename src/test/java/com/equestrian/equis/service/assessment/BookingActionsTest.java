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
        assertThat(BookingActions.allowed("upcoming", true)).containsExactly(
                BookingAction.RESCHEDULE, BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("waiting", true)).containsExactly(
                BookingAction.CHANGE_ASSESSOR, BookingAction.REMIND, BookingAction.EDIT);
        assertThat(BookingActions.allowed("waiting", false)).containsExactly(
                BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("noshow", true)).containsExactly(
                BookingAction.REBOOK, BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("cancelled", true)).containsExactly(
                BookingAction.REBOOK, BookingAction.CHANGE_ASSESSOR, BookingAction.EDIT);
        assertThat(BookingActions.allowed("placed", true)).containsExactly(BookingAction.VIEW_RIDER);
    }

    // E11 · Admin edits a person's details, case 9.
    @ParameterizedTest
    @ValueSource(strings = {"upcoming", "waiting", "noshow", "cancelled"})
    void everyRowButPlacedCanBeEdited(String status) {
        assertThat(BookingActions.allowed(status, true)).contains(BookingAction.EDIT);
        assertThat(BookingActions.allowed(status, false)).contains(BookingAction.EDIT);
    }

    @Test
    void aPlacedRowCannotBeEdited() {
        assertThat(BookingActions.allowed("placed", true)).doesNotContain(BookingAction.EDIT);
    }

    @Test
    void anUnknownStatusIsABug() {
        assertThatThrownBy(() -> BookingActions.allowed("lost", true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editIsSentLowercase() {
        assertThat(BookingAction.EDIT.value()).isEqualTo("edit");
    }
}
