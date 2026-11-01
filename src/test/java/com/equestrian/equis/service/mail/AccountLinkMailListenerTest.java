package com.equestrian.equis.service.mail;

import com.equestrian.equis.support.TestPostgres;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import com.equestrian.equis.dto.accounts.InviteTrainerRequest;
import com.equestrian.equis.dto.accounts.TrainerRow;
import com.equestrian.equis.entity.accounts.Invite;
import com.equestrian.equis.repository.accounts.InviteRepository;
import com.equestrian.equis.repository.accounts.TrainerRepository;
import com.equestrian.equis.service.accounts.AccountLinkCreatedEvent;
import com.equestrian.equis.service.accounts.AccountLinkService;
import com.equestrian.equis.service.accounts.TokenGenerator;
import com.equestrian.equis.service.accounts.TrainerAdminService;
import com.equestrian.equis.support.MailTestConfig;
import com.equestrian.equis.support.RecordingNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// The listener in the full application: emails go to the recording sender on the calling thread.
@SpringBootTest(properties = {
        "spring.flyway.placeholders.adminEmail=mail-listener-admin@example.com",
        "spring.flyway.placeholders.adminPasswordHash=not-a-real-hash"})
@Import(MailTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@RecordApplicationEvents
public class AccountLinkMailListenerTest {
    static TestPostgres postgres = TestPostgres.newDatabase();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        postgres.register(registry);
    }

    @Autowired
    TrainerAdminService trainerAdminService;
    @Autowired
    AccountLinkService accountLinkService;
    @Autowired
    TrainerRepository trainerRepository;
    @Autowired
    InviteRepository inviteRepository;
    @Autowired
    TokenGenerator tokenGenerator;
    @Autowired
    RecordingNotificationSender sender;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    ApplicationEvents events;

    // The migrations seed trainers with riders, so tests don't empty the tables; each uses its own phone and email.
    @BeforeEach
    void setUp() {
        sender.clear();
    }

    @Test
    void committedInviteSendsOneEmailWithWorkingLink() {
        TrainerRow row = trainerAdminService.invite(
                new InviteTrainerRequest("Mona Fathy", "01012340001", "mona.mail1@example.com"));

        assertThat(sender.all()).hasSize(1);
        OutgoingEmail email = sender.last();
        assertThat(email.to()).isEqualTo("mona.mail1@example.com");
        assertThat(email.subject()).isEqualTo("You're invited to Riding Schedule");
        Invite invite = accountLinkService.openLink(RecordingNotificationSender.tokenFrom(email));
        assertThat(invite.getTrainer().getId()).isEqualTo(row.id());
    }

    @Test
    void rolledBackInviteSendsNothing() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            trainerAdminService.invite(
                    new InviteTrainerRequest("Mona Fathy", "01012340002", "mona.mail2@example.com"));
            status.setRollbackOnly();
        });

        assertThat(sender.all()).isEmpty();
        assertThat(trainerRepository.findByEmail("mona.mail2@example.com")).isEmpty();
    }

    @Test
    void mailFailureIsLoggedWithoutTokenAndKeepsTheInvite(CapturedOutput output) {
        sender.failWith(new MailSendException("SMTP down"));

        TrainerRow row = trainerAdminService.invite(
                new InviteTrainerRequest("Mona Fathy", "01012340003", "mona.mail3@example.com"));

        String token = events.stream(AccountLinkCreatedEvent.class).findFirst().orElseThrow().rawToken();
        assertThat(trainerRepository.findById(row.id())).isPresent();
        assertThat(inviteRepository.findByTokenHash(tokenGenerator.hash(token))).get()
                .satisfies(i -> assertThat(i.getAcceptedAt()).isNull());
        assertThat(output.getAll())
                .contains("Account link email failed TRAINER=" + row.id() + " purpose=INVITE")
                .contains("SMTP down")
                .doesNotContain(token)
                .doesNotContain("/invite/");
    }
}
