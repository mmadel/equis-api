package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.platform.AccountFilter;
import com.equestrian.equis.dto.platform.AccountPage;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountRow;
import com.equestrian.equis.dto.platform.PlatformAccountStatus;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.PlatformAccountQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlatformAccountServiceTest {
    private static final UUID CLUB = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final ClubRepository clubs = mock(ClubRepository.class);
    private final PlatformAccountQuery query = mock(PlatformAccountQuery.class);
    private final TenantTransactions tenants = mock(TenantTransactions.class);
    private final PlatformAccountService service = new PlatformAccountService(clubs, query, tenants);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void rootRunsTheWork() {
        when(tenants.asRoot(any())).thenAnswer(inv -> ((Supplier<Object>) inv.getArgument(0)).get());
    }

    @Test
    void unknownClubIsNotFound() {
        when(clubs.existsById(CLUB)).thenReturn(false);

        assertThatThrownBy(() -> service.list(CLUB, new AccountFilter(null, null, null), 0, 25))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("club_not_found"));
        verifyNoInteractions(query);
    }

    @Test
    void trimsTheSearchAndPassesTheFilters() {
        AccountFilter expected = new AccountFilter("laila", AccountType.RIDER, PlatformAccountStatus.ACTIVE);
        PlatformAccountRow laila = new PlatformAccountRow(UUID.randomUUID(), AccountType.RIDER, "Laila", null, null,
                PlatformAccountStatus.ACTIVE, null);
        when(clubs.existsById(CLUB)).thenReturn(true);
        when(query.count(CLUB, expected)).thenReturn(1L);
        when(query.find(CLUB, expected, 0, 25)).thenReturn(List.of(laila));

        AccountPage page = service.list(CLUB,
                new AccountFilter("  laila ", AccountType.RIDER, PlatformAccountStatus.ACTIVE), 0, 25);

        assertThat(page).isEqualTo(new AccountPage(List.of(laila), 0, 25, 1));
    }

    @Test
    void aSearchShorterThanTwoCharactersIsIgnored() {
        assertThat(PlatformAccountService.normalise(null)).isNull();
        assertThat(PlatformAccountService.normalise("   ")).isNull();
        assertThat(PlatformAccountService.normalise(" a ")).isNull();
        assertThat(PlatformAccountService.normalise(" om ")).isEqualTo("om");
    }

    @Test
    void aPagePastTheEndIsEmptyWithTheTotal() {
        AccountFilter none = new AccountFilter(null, null, null);
        when(clubs.existsById(CLUB)).thenReturn(true);
        when(query.count(CLUB, none)).thenReturn(7L);

        assertThat(service.list(CLUB, none, 4, 2)).isEqualTo(new AccountPage(List.of(), 4, 2, 7));
        verify(query, never()).find(any(), any(), anyInt(), anyInt());
    }
}
