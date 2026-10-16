package com.equestrian.equis.service.assessment;

import java.util.EnumSet;
import java.util.Set;

import static com.equestrian.equis.service.assessment.BookingAction.CHANGE_ASSESSOR;
import static com.equestrian.equis.service.assessment.BookingAction.REBOOK;
import static com.equestrian.equis.service.assessment.BookingAction.REMIND;
import static com.equestrian.equis.service.assessment.BookingAction.RESCHEDULE;
import static com.equestrian.equis.service.assessment.BookingAction.VIEW_RIDER;

// The one definition of the W2 row actions. The list W2 shows and the server's checks on each action both use it,
// so they can't drift apart.
public final class BookingActions {
    private BookingActions() {}

    // displayStatus is AssessmentBooking.displayStatus(...). Remind needs an active assessor to remind.
    public static Set<BookingAction> allowed(String displayStatus, boolean assessorActive) {
        return switch (displayStatus) {
            case "upcoming" -> EnumSet.of(RESCHEDULE, CHANGE_ASSESSOR);
            case "waiting" -> assessorActive ? EnumSet.of(CHANGE_ASSESSOR, REMIND) : EnumSet.of(CHANGE_ASSESSOR);
            case "noshow", "cancelled" -> EnumSet.of(REBOOK, CHANGE_ASSESSOR);
            case "placed" -> EnumSet.of(VIEW_RIDER);
            default -> throw new IllegalArgumentException("Unknown booking status: " + displayStatus);
        };
    }
}
