package com.equestrian.equis.service.assessment;

import com.equestrian.equis.entity.sessions.Session;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

// How a slot is named in messages: A3's "Booked …" and the result-overdue notice use the same words.
public final class SlotLabels {
    // "Sat 17 Oct, 10:00": the slot's date and start time as the school writes them.
    private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);

    private SlotLabels() {}

    public static String describe(Session slot) {
        return slot.getSessionDate().atTime(slot.getStartTime()).format(SLOT_FORMAT);
    }
}
