package com.equestrian.equis.common.tenant;

import com.equestrian.equis.common.error.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Supplier;

// Runs work in one club inside its own transaction: the club is set first, so the connection and the Hibernate
// session of that transaction both get it (one club per transaction). Call it outside a transaction; inside one
// the work joins it and keeps that transaction's club.
@Component
public class TenantTransactions {
    private final TransactionTemplate transaction;

    public TenantTransactions(PlatformTransactionManager transactionManager) {
        this.transaction = new TransactionTemplate(transactionManager);
    }

    // A null club runs with no club. Any exception rolls the transaction back.
    public <T> T inClub(UUID clubId, Supplier<T> work) {
        return TenantContext.runAs(clubId, () -> transaction.execute(status -> work.get()));
    }

    // Like inClub, but an ApiException (a 4xx answer) commits what was written before it is rethrown,
    // like @Transactional(noRollbackFor = ApiException.class). Sign-in uses it to keep the failed-attempt counter.
    public <T> T inClubKeepingWritesOnApiError(UUID clubId, Supplier<T> work) {
        return rethrow(TenantContext.runAs(clubId, () -> executeKeepingWrites(work)));
    }

    // Reads across clubs. Writes of school rows are refused (TenantGuard); RLS lets every row through.
    public <T> T asRoot(Supplier<T> work) {
        return TenantContext.runAsRoot(() -> transaction.execute(status -> work.get()));
    }

    // asRoot that keeps writes on an ApiException, like inClubKeepingWritesOnApiError. The platform sign-in uses it.
    public <T> T asRootKeepingWritesOnApiError(Supplier<T> work) {
        return rethrow(TenantContext.runAsRoot(() -> executeKeepingWrites(work)));
    }

    private <T> Outcome<T> executeKeepingWrites(Supplier<T> work) {
        return transaction.execute(status -> {
            try {
                return new Outcome<>(work.get(), null);
            } catch (ApiException e) {
                return new Outcome<>(null, e);
            }
        });
    }

    private static <T> T rethrow(Outcome<T> outcome) {
        if (outcome.error() != null) {
            throw outcome.error();
        }
        return outcome.value();
    }

    private record Outcome<T>(T value, ApiException error) {
    }
}
