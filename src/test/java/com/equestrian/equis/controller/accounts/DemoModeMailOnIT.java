package com.equestrian.equis.controller.accounts;

import com.equestrian.equis.common.config.MailSettings;
import com.equestrian.equis.support.IntegrationTestBase;
import com.equestrian.equis.support.TrainerAccountsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// E23 case 1: with app.mail.enabled not set (the shared test context), an invite works as before E23: a pending
// trainer, one open INVITE link and one email. The demo-mode cases are in DemoModeIT.
class DemoModeMailOnIT extends IntegrationTestBase {
    @Autowired
    TrainerAccountsFixture fx;
    @Autowired
    MailSettings mail;

    @BeforeEach
    void setUp() {
        fx.reset();
    }

    // 1
    @Test
    void withTheFlagUnsetAnInviteStillSendsALink() throws Exception {
        assertThat(mail.enabled()).isTrue();

        fx.inviteRequest("Coach Omar", "01060000001", "trainer1@demo.equis")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("invited"));

        UUID omar = fx.jdbc().queryForObject("select id from trainer where email = 'trainer1@demo.equis'", UUID.class);
        assertThat(fx.jdbc().queryForObject("""
                select count(*) from invite
                where trainer_id = ? and purpose = 'invite' and accepted_at is null""", Integer.class, omar))
                .isEqualTo(1);
        assertThat(fx.jdbc().queryForObject("select password_hash from trainer where id = ?", String.class, omar))
                .isNull();
        assertThat(fx.recorder().all()).singleElement()
                .satisfies(email -> assertThat(email.to()).isEqualTo("trainer1@demo.equis"));
    }
}
