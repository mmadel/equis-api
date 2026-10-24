package com.equestrian.equis.service.assessment;

import java.util.EnumSet;
import java.util.Set;

import static com.equestrian.equis.service.assessment.BookingAction.CANCEL;
import static com.equestrian.equis.service.assessment.BookingAction.CHANGE_ASSESSOR;
import static com.equestrian.equis.service.assessment.BookingAction.CLOSE;
import static com.equestrian.equis.service.assessment.BookingAction.EDIT;
import static com.equestrian.equis.service.assessment.BookingAction.REBOOK;
import static com.equestrian.equis.service.assessment.BookingAction.REMIND;
import static com.equestrian.equis.service.assessment.BookingAction.REOPEN;
import static com.equestrian.equis.service.assessment.BookingAction.RESCHEDULE;
import static com.equestrian.equis.service.assessment.BookingAction.VIEW_RIDER;

// The one definition of the W2 row actions. The list W2 shows and the server's checks on each action both use it,
// so they can't drift apart.
public final class BookingActions {
    private BookingActions() {}

    // displayStatus is AssessmentBooking.displayStatus(...). Remind needs an active assessor to remind.
    // Cancel only before the slot starts.
    // Edit is on every row but placed: a rider's details live on the rider.
    // Close only when nothing is booked (no-show or cancelled); a closed person's row allows reopen and edit.
    public static Set<BookingAction> allowed(String displayStatus, boolean assessorActive, boolean memberClosed) {
        if (memberClosed) {
            return EnumSet.of(REOPEN, EDIT);
        }
        return switch (displayStatus) {
            case "upcoming" -> EnumSet.of(RESCHEDULE, CANCEL, CHANGE_ASSESSOR, EDIT);
            case "waiting" -> assessorActive ? EnumSet.of(CHANGE_ASSESSOR, REMIND, EDIT)
                    : EnumSet.of(CHANGE_ASSESSOR, EDIT);
            case "noshow", "cancelled" -> EnumSet.of(REBOOK, CHANGE_ASSESSOR, CLOSE, EDIT);
            case "placed" -> EnumSet.of(VIEW_RIDER);
            default -> throw new IllegalArgumentException("Unknown booking status: " + displayStatus);
        };
    }
}
