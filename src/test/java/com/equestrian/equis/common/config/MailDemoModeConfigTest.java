package com.equestrian.equis.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

// E23 demo mode: the startup checks of app.mail.enabled=false and app.mail.demo-password. Numbers are the ticket's
// test cases. Sends no email and needs no database, so it runs in the default build (not tagged "email").
@ExtendWith(OutputCaptureExtension.class)
class MailDemoModeConfigTest {
    private static final String DEMO_PASSWORD = "Demo-2026!";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            // Only the values set here: EQUIS_* or APP_MAIL_* variables on the machine must not leak in.
            .withInitializer(context -> {
                var sources = context.getEnvironment().getPropertySources();
                sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            })
            .withUserConfiguration(MailConfig.class);

    // 1 (the setting part; the invite part is in DemoModeMailOnIT)
    @Test
    void mailIsOnWhenTheFlagIsNotSet() {
        runner.withPropertyValues("app.mail.mode=log")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MailSettings.class).enabled()).isTrue();
                });
    }

    // Compose passes an unset EQUIS_MAIL_ENABLED as an empty value: mail stays on.
    @Test
    void anEmptyFlagMeansMailOn() {
        runner.withPropertyValues("app.mail.mode=log", "app.mail.enabled=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MailSettings.class).enabled()).isTrue();
                });
    }

    // 2
    @Test
    void mailOffWithoutADemoPasswordStopsTheApp() {
        for (String blank : new String[] {"", "   "}) {
            runner.withPropertyValues("app.mail.mode=log", "app.mail.enabled=false", "app.mail.demo-password=" + blank)
                    .run(context -> assertThat(context).getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("EQUIS_DEMO_PASSWORD is required when EQUIS_MAIL_ENABLED=false"));
        }
        runner.withPropertyValues("app.mail.mode=log", "app.mail.enabled=false")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessage("EQUIS_DEMO_PASSWORD is required when EQUIS_MAIL_ENABLED=false"));
    }

    // 3: 7 and 73 bytes; Arabic letters are 2 bytes each, so 37 of them are 74 bytes and 36 are exactly 72
    @Test
    void aDemoPasswordOutsideEightToSeventyTwoBytesStopsTheApp() {
        for (String wrong : new String[] {"Demo-26", "a".repeat(73), "ك".repeat(37)}) {
            runner.withPropertyValues("app.mail.mode=log", "app.mail.enabled=false", "app.mail.demo-password=" + wrong)
                    .run(context -> assertThat(context).getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("EQUIS_DEMO_PASSWORD must be 8 to 72 bytes"));
        }
        for (String right : new String[] {"Demo-202", "a".repeat(72), "ك".repeat(36)}) {
            runner.withPropertyValues("app.mail.mode=log", "app.mail.enabled=false", "app.mail.demo-password=" + right)
                    .run(context -> assertThat(context).hasNotFailed());
        }
    }

    // 4
    @Test
    void mailOffSkipsTheSmtpCheckAndWarnsOnceWithoutThePassword(CapturedOutput output) {
        runner.withPropertyValues("app.mail.mode=smtp", "app.mail.from=", "spring.mail.username=",
                        "spring.mail.password=", "app.mail.enabled=false", "app.mail.demo-password=" + DEMO_PASSWORD)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MailSettings.class).toString()).doesNotContain(DEMO_PASSWORD);
                });

        assertThat(output.getAll().lines()
                .filter(line -> line.contains("WARN") && line.contains("Mail is turned off (EQUIS_MAIL_ENABLED=false): "
                        + "invites and password resets set the demo password and send no email. Demo use only.")))
                .hasSize(1);
        assertThat(output.getAll()).doesNotContain(DEMO_PASSWORD);
    }

    // Mail on: the E6 smtp check still runs and nothing is logged about demo mode.
    @Test
    void mailOnKeepsTheSmtpCheck(CapturedOutput output) {
        runner.withPropertyValues("app.mail.mode=smtp", "app.mail.enabled=true", "spring.mail.password=")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessage("APP_MAIL_USERNAME, APP_MAIL_PASSWORD and APP_MAIL_FROM are required "
                                + "when APP_MAIL_MODE=smtp"));
        assertThat(output.getAll()).doesNotContain("Mail is turned off");
    }

    @Test
    void toStringNeverPrintsTheDemoPassword() {
        MailSettings settings = new MailSettings(MailSettings.Mode.LOG, "school@example.com", "Riding Schedule",
                false, DEMO_PASSWORD);

        assertThat(settings.toString()).doesNotContain(DEMO_PASSWORD).contains("enabled=false");
    }
}
