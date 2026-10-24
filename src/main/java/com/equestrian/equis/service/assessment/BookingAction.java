package com.equestrian.equis.service.assessment;

import java.util.Locale;

// What the admin may do on a W2 row.
public enum BookingAction {
    RESCHEDULE,
    CANCEL,
    REBOOK,
    CHANGE_ASSESSOR,
    REMIND,
    VIEW_RIDER,
    CLOSE,
    REOPEN,
    EDIT;

    // The form sent to the console: "reschedule", "change_assessor", ...
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
