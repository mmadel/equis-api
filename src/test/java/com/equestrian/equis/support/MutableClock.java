package com.equestrian.equis.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

// A clock a test can set and move, for code that reads the time more than once.
public class MutableClock extends Clock {
    private volatile Instant now;
    private final ZoneId zone;

    public MutableClock(Instant now) {
        this(now, ZoneOffset.UTC);
    }

    private MutableClock(Instant now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    public void set(Instant now) {
        this.now = now;
    }

    public void advance(Duration by) {
        this.now = now.plus(by);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    // The copy does not follow later moves; the code under test only uses it for one reading.
    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now, zone);
    }
}
