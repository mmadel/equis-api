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
4. **Mail tests are tagged `@Tag("email")` and never run by default**, not even when named in `-Dtest`
   (Surefire `excludedGroups` in `pom.xml`, so CI skips them too). Tagged today: `MailConfigTest`,
   `SmtpNotificationSenderTest` (GreenMail), `AccountLinkEmailsTest`, `AccountLinkMailListenerTest`. Tag any new mail test
   the same way. Run them only when the ticket touches email, with the `email-tests` profile (runs only tagged tests):
   `./mvnw test -Pemail-tests`.
5. **Avoid extra Spring context reloads.** Reuse the existing test setup (`MailTestConfig`,
   `TrainerAccountsFixture.Config`, `AdminOversightFixture.Config`, `MutableClock`) instead of building new
   configurations, and don't add one-off `@MockitoBean` combinations to `@SpringBootTest` classes unless needed.
   Prefer `@WebMvcTest` / `@DataJpaTest` slices or plain unit tests when a full context isn't required.
6. **Every test runs in the default club.** School tables have row-level security (V24), and the app's connections
   work as the role `equis_app`, so SQL without a club sees no rows and inserts are refused.
   `support/DefaultClubExtension` (registered for all tests) sets `Club.DEFAULT_ID` around each test, and MockMvc
   requests run in their token's club. For another club use `TenantContext.runAs(clubId, ...)` (outside a
   transaction) or, inside a `@DataJpaTest` transaction, `select set_config('app.bypass_rls', 'on', true)`.
   New threads have no club: use `support/ClubThreads.pool(n)` / `ClubThreads.inDefaultClub()`. Code that opens its
   own connection (hand-run Flyway tests) uses `TestPostgres.getJdbcUrlSeeingAllClubs()`. DDL through the app's
   connection needs the owner: start it with `reset role;`. `TRUNCATE` works (test-only grant in `db/testgrants`).

### Known gaps (not set up yet)

1. No Surefire/Failsafe split: there is no Failsafe, so every test except the `email` ones runs in `mvn test`.
2. No shared `@SpringBootTest` base class: each class sets its own `adminEmail` placeholder, so each gets its own
   Spring context. All classes share one Postgres container through `support/TestPostgres`, which gives each
   class its own empty database (`TestPostgres.newDatabase()` + `@DynamicPropertySource`). New DB tests should do the same.
3. No test profile: test settings live in `src/test/resources/config/application.properties`, not `application-test`.
4. No dedicated no-op sender: `LogNotificationSender` (log mode) and `RecordingNotificationSender` fill that role.
