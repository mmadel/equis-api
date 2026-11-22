package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.platform.AccountFilter;
import com.equestrian.equis.dto.platform.AccountPage;
import com.equestrian.equis.dto.platform.PlatformAccountRow;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.PlatformAccountQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// The platform's Accounts tab: one club's admins, trainers and riders, searched, filtered and paged in the database.
// Runs in root mode, one transaction per call. Only account details: no payments, sessions, balances or evaluations.
@Service
public class PlatformAccountService {
    // A shorter search is ignored, so one typed letter doesn't filter the list.
    static final int MIN_QUERY = 2;

    private final ClubRepository clubs;
    private final PlatformAccountQuery accounts;
    private final TenantTransactions tenants;

    public PlatformAccountService(ClubRepository clubs, PlatformAccountQuery accounts, TenantTransactions tenants) {
        this.clubs = clubs;
        this.accounts = accounts;
        this.tenants = tenants;
    }

    public AccountPage list(UUID clubId, AccountFilter filter, int page, int size) {
        AccountFilter f = new AccountFilter(normalise(filter.q()), filter.type(), filter.status());
        return tenants.asRoot(() -> {
            if (!clubs.existsById(clubId)) {
                throw ClubErrors.notFound();
            }
            long total = accounts.count(clubId, f);
            List<PlatformAccountRow> rows = (long) page * size >= total ? List.of() : accounts.find(clubId, f, page, size);
            return new AccountPage(rows, page, size, total);
        });
    }

    // Trimmed; null when blank or shorter than MIN_QUERY.
    static String normalise(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.length() < MIN_QUERY ? null : trimmed;
    }
}
