# CLAUDE.md

## Testing rules

All tests live in `src/test/java` and end in `*Test` (there are no `*IT` classes); Surefire runs them with Spring Boot's defaults.
Most tests start a PostgreSQL Testcontainer, so Docker must be running. Use the Maven wrapper:
`./mvnw` in Git Bash, `.\mvnw.cmd` in PowerShell.

1. **While implementing a ticket, run only the tests for the classes you changed**, e.g.
   `./mvnw test -Dtest='MemberBookingService*Test'` or `./mvnw test -Dtest='AdminMemberApiTest,MemberControllerTest'`.
   If the ticket lists "Tests to run", run exactly those.
2. **Run the full suite (`./mvnw test`) only once, at the end, before committing.**
   If it fails, fix the failures, rerun only the failing tests with `-Dtest=...`, then run the full suite once more.
3. **Tests never send real email.** The test properties (`src/test/resources/config/application.properties`)
   set `app.mail.mode=log`, so `LogNotificationSender` only prints. To check an email, `@Import(MailTestConfig.class)`:
   it swaps in `RecordingNotificationSender` (assert with `all()`, `last()`, `tokenFrom(...)`) and a synchronous
   `mailExecutor`, so the `@Async` `AccountLinkMailListener` has sent before the request returns. In slice tests,
   `@MockitoBean NotificationSender` and `verify(...)` is fine. Never point `spring.mail.*` at a real server.
4. **Tests that need real SMTP use GreenMail** (see `SmtpNotificationSenderTest`) and should be tagged `@Tag("email")`
   and left out of the default run. Run them only when the ticket touches email:
   `./mvnw test -Dtest='SmtpNotificationSenderTest,MailConfigTest,AccountLink*Test'`.
5. **Avoid extra Spring context reloads.** Reuse the existing test setup (`MailTestConfig`,
   `TrainerAccountsFixture.Config`, `AdminOversightFixture.Config`, `MutableClock`) instead of building new
   configurations, and don't add one-off `@MockitoBean` combinations to `@SpringBootTest` classes unless needed.
   Prefer `@WebMvcTest` / `@DataJpaTest` slices or plain unit tests when a full context isn't required.

### Known gaps (not set up yet)

1. No Surefire/Failsafe split: `pom.xml` configures neither plugin, so every test runs in `mvn test`.
2. No `@Tag("email")` or exclusion of it: `SmtpNotificationSenderTest` (GreenMail, in-process) runs in the default suite.
3. No shared `@SpringBootTest` base class: each class sets its own `adminEmail` placeholder, so each gets its own
   Spring context. All classes share one Postgres container through `support/TestPostgres`, which gives each
   class its own empty database (`TestPostgres.newDatabase()` + `@DynamicPropertySource`). New DB tests should do the same.
4. No test profile: test settings live in `src/test/resources/config/application.properties`, not `application-test`.
5. No dedicated no-op sender: `LogNotificationSender` (log mode) and `RecordingNotificationSender` fill that role.
