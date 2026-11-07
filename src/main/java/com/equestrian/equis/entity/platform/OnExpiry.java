package com.equestrian.equis.entity.platform;

// What happens to a club when its subscription ends.
public enum OnExpiry {
    // The club can't be used until it is renewed.
    BLOCK,
    // The club can look at its data but not change it.
    READ_ONLY
}
