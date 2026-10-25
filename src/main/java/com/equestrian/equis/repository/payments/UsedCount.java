package com.equestrian.equis.repository.payments;

import java.util.UUID;

// How many sessions were used from one package.
public interface UsedCount {
    UUID getPaymentId();

    long getUsed();
}
