package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.platform.AccountType;
import com.equestrian.equis.dto.platform.PlatformAccountReset;
import com.equestrian.equis.entity.accounts.AdminAccount;
import com.equestrian.equis.entity.accounts.AdminStatus;
import com.equestrian.equis.entity.accounts.InvitePurpose;
import com.equestrian.equis.entity.accounts.SubjectType;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.club.Club;
import com.equestrian.equis.entity.platform.AccountActionType;
import com.equestrian.equis.entity.platform.PlatformAccountAction;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.riders.RiderAccountStatus;
import com.equestrian.equis.repository.accounts.AdminAccountRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.repository.club.ClubRepository;
import com.equestrian.equis.repository.platform.PlatformAccountActionRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AccountLinkService;
import com.equestrian.equis.service.accounts.AccountLinkService.IssuedLink;
import com.equestrian.equis.service.accounts.LinkSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlatformAccountActionServiceTest {
    private static final UUID CLUB = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID SUPER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    // Sat 21 Nov 2026, 17:40 in Cairo (UTC+2).
    private static final Instant NOW = Instant.parse("2026-11-21T15:40:00Z");
    private static final Instant EXPIRES = NOW.plus(24, ChronoUnit.HOURS);
    private static final String TOKEN = "raw-token";

    private final ClubRepository clubs = mock(ClubRepository.class);
    private final AdminAccountRepository admins = mock(AdminAccountRepository.class);
    private final TrainerRepository trainers = mock(TrainerRepository.class);
    private final RiderRepository riders = mock(RiderRepository.class);
    private final PlatformAccountActionRepository actions = mock(PlatformAccountActionRepository.class);
    private final AccountLinkService links = mock(AccountLinkService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TenantTransactions tenants = mock(TenantTransactions.class);
    private final PlatformAccountActionService service = new PlatformAccountActionService(clubs, admins, trainers,
            riders, actions, links, events, tenants, Clock.fixed(NOW, ZoneOffset.UTC));

    private final Club club = mock(Club.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void transactionsRunTheWork() {
        when(tenants.asRoot(any())).thenAnswer(inv -> ((Supplier<Object>) inv.getArgument(0)).get());
        when(tenants.inClub(eq(CLUB), any())).thenAnswer(inv -> ((Supplier<Object>) inv.getArgument(1)).get());
        when(club.getId()).thenReturn(CLUB);
        when(club.getName()).thenReturn("Cairo Equestrian");
        when(club.getCode()).thenReturn("cairo-eq");
        when(clubs.findById(CLUB)).thenReturn(Optional.of(club));
    }

    @Test
    void resetsAnActiveAdminInTheClubAndRecordsIt() {
        AdminAccount hany = admin(AdminStatus.ACTIVE);
        when(admins.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(hany));
        when(links.issue(hany, InvitePurpose.RESET)).thenReturn(new IssuedLink(TOKEN, EXPIRES));

        PlatformAccountReset reset = service.resetPassword(CLUB, AccountType.ADMIN, ACCOUNT, SUPER);

        assertThat(reset).isEqualTo(new PlatformAccountReset(ACCOUNT, AccountType.ADMIN, "hany@cairo-eq.com",
                EXPIRES));
        verify(tenants).inClub(eq(CLUB), any());
        PlatformAccountAction row = savedAction();
        assertThat(row.getClubId()).isEqualTo(CLUB);
        assertThat(row.getAccountType()).isEqualTo(AccountType.ADMIN);
        assertThat(row.getAccountId()).isEqualTo(ACCOUNT);
        assertThat(row.getAccountName()).isEqualTo("Hany Saeed");
        assertThat(row.getAccountEmail()).isEqualTo("hany@cairo-eq.com");
        assertThat(row.getAction()).isEqualTo(AccountActionType.RESET_PASSWORD);
        assertThat(row.getDoneBy()).isEqualTo(SUPER);
        assertThat(row.getDoneAt()).isEqualTo(NOW);
        assertThat(publishedEvent()).isEqualTo(new AccountLinkCreatedEvent(SubjectType.ADMIN, ACCOUNT, "Hany Saeed",
                "hany@cairo-eq.com", InvitePurpose.RESET, TOKEN, EXPIRES, "Cairo Equestrian", "cairo-eq",
                LinkSender.PLATFORM));
    }

    @Test
    void resetsAnActiveTrainer() {
        Trainer omar = trainer(TrainerStatus.ACTIVE);
        when(trainers.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(omar));
        when(links.issue(omar, InvitePurpose.RESET)).thenReturn(new IssuedLink(TOKEN, EXPIRES));

        PlatformAccountReset reset = service.resetPassword(CLUB, AccountType.TRAINER, ACCOUNT, SUPER);

        assertThat(reset).isEqualTo(new PlatformAccountReset(ACCOUNT, AccountType.TRAINER, "omar@cairo-eq.com",
                EXPIRES));
        assertThat(savedAction().getAccountType()).isEqualTo(AccountType.TRAINER);
        assertThat(publishedEvent()).isEqualTo(new AccountLinkCreatedEvent(SubjectType.TRAINER, ACCOUNT, "Omar Adel",
                "omar@cairo-eq.com", InvitePurpose.RESET, TOKEN, EXPIRES, LinkSender.PLATFORM));
    }

    @Test
    void resetsAnActiveRider() {
        Rider laila = rider(RiderAccountStatus.ACTIVE, "laila@example.com");
        when(riders.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(laila));
        when(links.issue(laila, InvitePurpose.RESET)).thenReturn(new IssuedLink(TOKEN, EXPIRES));

        PlatformAccountReset reset = service.resetPassword(CLUB, AccountType.RIDER, ACCOUNT, SUPER);

        assertThat(reset).isEqualTo(new PlatformAccountReset(ACCOUNT, AccountType.RIDER, "laila@example.com",
                EXPIRES));
        assertThat(savedAction().getAccountName()).isEqualTo("Laila Mostafa");
        assertThat(publishedEvent().sentBy()).isEqualTo(LinkSender.PLATFORM);
        assertThat(publishedEvent().subjectType()).isEqualTo(SubjectType.RIDER);
    }

    @Test
    void unknownClubIsNotFoundAndNothingRunsInAClub() {
        when(clubs.findById(CLUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(CLUB, AccountType.ADMIN, ACCOUNT, SUPER))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("club_not_found"));
        verify(tenants, never()).inClub(any(), any());
        verifyNoInteractions(admins, links, actions, events);
    }

    @Test
    void anAccountNotInTheClubIsNotFound() {
        when(admins.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.empty());
        when(trainers.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.empty());
        when(riders.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.empty());

        for (AccountType type : AccountType.values()) {
            assertThatThrownBy(() -> service.resetPassword(CLUB, type, ACCOUNT, SUPER))
                    .isInstanceOfSatisfying(ApiException.class, e -> {
                        assertThat(e.status().value()).isEqualTo(404);
                        assertThat(e.code()).isEqualTo("account_not_found");
                    });
        }
        verifyNoInteractions(links, actions, events);
    }

    @Test
    void anInvitedAdminIsRefused() {
        when(admins.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(admin(AdminStatus.INVITED)));

        assertInvalidStatus(AccountType.ADMIN, "INVITED");
    }

    @Test
    void anInvitedOrDisabledTrainerIsRefused() {
        Trainer invited = trainer(TrainerStatus.INVITED);
        when(trainers.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(invited));
        assertInvalidStatus(AccountType.TRAINER, "INVITED");

        Trainer disabled = trainer(TrainerStatus.DISABLED);
        when(trainers.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(disabled));
        assertInvalidStatus(AccountType.TRAINER, "DISABLED");
    }

    @Test
    void aRiderWithoutAnActiveAccountIsRefused() {
        Rider invited = rider(RiderAccountStatus.INVITED, "mona@example.com");
        when(riders.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(invited));
        assertInvalidStatus(AccountType.RIDER, "INVITED");

        Rider none = rider(RiderAccountStatus.NONE, null);
        when(riders.findByIdAndClubIdForUpdate(ACCOUNT, CLUB)).thenReturn(Optional.of(none));
        assertInvalidStatus(AccountType.RIDER, "NO_ACCOUNT");
    }

    private void assertInvalidStatus(AccountType type, String status) {
        assertThatThrownBy(() -> service.resetPassword(CLUB, type, ACCOUNT, SUPER))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(409);
                    assertThat(e.code()).isEqualTo("invalid_account_status");
                    assertThat(e.properties()).isEqualTo(Map.of("status", status, "action", "reset_password"));
                });
        verifyNoInteractions(links, actions, events);
    }

    private PlatformAccountAction savedAction() {
        ArgumentCaptor<PlatformAccountAction> captor = ArgumentCaptor.forClass(PlatformAccountAction.class);
        verify(actions).save(captor.capture());
        return captor.getValue();
    }

    private AccountLinkCreatedEvent publishedEvent() {
        ArgumentCaptor<AccountLinkCreatedEvent> captor = ArgumentCaptor.forClass(AccountLinkCreatedEvent.class);
        verify(events).publishEvent(captor.capture());
        return captor.getValue();
    }

    private static AdminAccount admin(AdminStatus status) {
        AdminAccount admin = new AdminAccount();
        admin.setId(ACCOUNT);
        admin.setName("Hany Saeed");
        admin.setEmail("hany@cairo-eq.com");
        admin.setStatus(status);
        return admin;
    }

    private static Trainer trainer(TrainerStatus status) {
        Trainer trainer = mock(Trainer.class);
        when(trainer.getId()).thenReturn(ACCOUNT);
        when(trainer.getName()).thenReturn("Omar Adel");
        when(trainer.getEmail()).thenReturn("omar@cairo-eq.com");
        when(trainer.getStatus()).thenReturn(status);
        return trainer;
    }

    private static Rider rider(RiderAccountStatus status, String email) {
        Rider rider = mock(Rider.class);
        when(rider.getId()).thenReturn(ACCOUNT);
        when(rider.getName()).thenReturn("Laila Mostafa");
        when(rider.getEmail()).thenReturn(email);
        when(rider.getAccountStatus()).thenReturn(status);
        return rider;
    }
}
