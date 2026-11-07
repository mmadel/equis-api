package com.equestrian.equis.common.tenant;

import java.util.UUID;
import java.util.function.Supplier;

// The club the current thread works for. Set per request by TenantFilter, per club by jobs.
// Root mode reads across clubs (sign-in lookups, listing clubs); writes of school rows are refused there.
//
// The database connection takes the club when it is checked out (TenantAwareDataSource), and Hibernate when a
// session opens, so change the club only outside a transaction. TenantTransactions does both in the right order.
public final class TenantContext {
    // Hibernate's tenant id for root mode; never a real club.
    public static final UUID ROOT = new UUID(0, 0);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    // The current club, or null in root mode and when no club is set.
    public static UUID current() {
        UUID club = CURRENT.get();
        return ROOT.equals(club) ? null : club;
    }

    public static boolean isRoot() {
        return ROOT.equals(CURRENT.get());
    }

    public static void set(UUID clubId) {
        if (clubId == null || ROOT.equals(clubId)) {
            throw new IllegalArgumentException("Not a club id: " + clubId);
        }
        CURRENT.set(clubId);
    }

    public static void setRoot() {
        CURRENT.set(ROOT);
    }

    public static void clear() {
        CURRENT.remove();
    }

    // Runs work for one club and puts the previous club (or none) back afterwards, even when work throws.
    // A null club runs with no club, where school tables show no rows.
    public static <T> T runAs(UUID clubId, Supplier<T> work) {
        return with(clubId, work);
    }

    public static <T> T runAsRoot(Supplier<T> work) {
        return with(ROOT, work);
    }

    private static <T> T with(UUID value, Supplier<T> work) {
        UUID previous = CURRENT.get();
        if (value == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(value);
        }
        try {
            return work.get();
        } finally {
            restore(previous);
        }
    }

    // Puts back a value read with raw(): null clears.
    static void restore(UUID previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    // The stored value as is, ROOT included, for code that saves and restores it.
    static UUID raw() {
        return CURRENT.get();
    }
}
