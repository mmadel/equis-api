package com.equestrian.equis.service.payments;

import com.equestrian.equis.entity.riders.Rider;

import java.util.Comparator;
import java.util.List;

// Some riders have no session left on the session date, so nothing was deducted for anyone.
public class NoSessionsLeftException extends RuntimeException {
    private final List<Rider> riders;

    public NoSessionsLeftException(List<Rider> riders) {
        super("No sessions left for: " + riders.stream().map(Rider::getName).sorted().toList());
        this.riders = riders.stream().sorted(Comparator.comparing(Rider::getName)).toList();
    }

    // Sorted by name.
    public List<Rider> getRiders() {
        return riders;
    }
}
