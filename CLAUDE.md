# CLAUDE.md

## Testing rules

All tests live in `src/test/java`. Unit and slice tests (no database) end in `*Test` and run with Surefire
(`./mvnw test`); database tests end in `*IT` and run with Failsafe (`./mvnw verify`; `-DskipUnitTests` skips the
`*Test` ones). The `*IT` classes start a PostgreSQL Testcontainer, so Docker must be running. `./mvnw verify` also
merges JaCoCo coverage into `target/site/jacoco-merged/` and fails below the floors in `pom.xml`
(`coverage.minimum.*`). Use the Maven wrapper: `./mvnw` in Git Bash, `.\mvnw.cmd` in PowerShell.

1. **While implementing a ticket, run only the tests for the classes you changed**, e.g.
   `./mvnw test -Dtest='MyRidersControllerTest'` for `*Test` classes, or
   `./mvnw verify -DskipUnitTests -Dit.test='MemberBookingServiceIT,AdminMemberApiIT'` for `*IT` classes.
   If the ticket lists "Tests to run", run exactly those.
2. **Run the full suite (`./mvnw verify`) only once, at the end, before committing.**
   If it fails, fix the failures, rerun only the failing tests with `-Dtest=...` / `-Dit.test=...`, then run the full
   suite once more.
3. **Tests never send real email.** The test properties (`src/test/resources/config/application.properties`)
   set `app.mail.mode=log`, so `LogNotificationSender` only prints. To check an email, `@Import(MailTestConfig.class)`:
   it swaps in `RecordingNotificationSender` (assert with `all()`, `last()`, `tokenFrom(...)`) and a synchronous
   `mailExecutor`, so the `@Async` `AccountLinkMailListener` has sent before the request returns. In slice tests,
   `@MockitoBean NotificationSender` and `verify(...)` is fine. Never point `spring.mail.*` at a real server.
4. **Mail tests are tagged `@Tag("email")` and never run by default**, not even when named in `-Dtest`
   (Surefire and Failsafe `excludedGroups` in `pom.xml`, so CI skips them too). Tagged today: `MailConfigTest`,
   `SmtpNotificationSenderTest` (GreenMail), `AccountLinkEmailsTest`, `AccountLinkMailListenerIT`. Tag any new mail test
   the same way. Run them only when the ticket touches email, with the `email-tests` profile (runs only tagged tests,
   no coverage check): `./mvnw verify -Pemail-tests`.
5. **Avoid extra Spring context reloads.** A full-app database test extends `support/IntegrationTestBase` and adds
   no annotations of its own: it gets the shared context with MockMvc, the fixtures, `MailTestConfig` and one
   `MutableClock` (`CLOCK`, set the start time in `@BeforeEach`). Its own `@Import`, properties or `@MockitoBean`s
   give it a context of its own, so avoid them. A `@DataJpaTest` implements `support/PostgresContainer`. Both share
   one database per JVM; `support/FreshDatabase` puts the migrated rows back before each class, so each class starts
   from a freshly migrated database (the seeded admin is `TrainerAccountsFixture.ADMIN_EMAIL`). Only tests that
   migrate by hand use `TestPostgres.newDatabase()`. Prefer `@WebMvcTest` slices or plain unit tests (`*Test`) when
   no database is needed.
6. **Every test runs in the default club.** School tables have row-level security (V24), and the app's connections
   work as the role `equis_app`, so SQL without a club sees no rows and inserts are refused.
   `support/DefaultClubExtension` (registered for all tests) sets `Club.DEFAULT_ID` around each test, and MockMvc
   requests run in their token's club. For another club use `TenantContext.runAs(clubId, ...)` (outside a
   transaction) or, inside a `@DataJpaTest` transaction, `select set_config('app.bypass_rls', 'on', true)`.
   New threads have no club: use `support/ClubThreads.pool(n)` / `ClubThreads.inDefaultClub()`. Code that opens its
   own connection uses `PostgresContainer.superuser()` (or, on its own database, `TestPostgres.getJdbcUrlSeeingAllClubs()`). DDL through the app's
   connection needs the owner: start it with `reset role;`. `TRUNCATE` works (test-only grant in `db/testgrants`).

### Known gaps (not set up yet)

1. No test profile: test settings live in `src/test/resources/config/application.properties`, not `application-test`.
2. No dedicated no-op sender: `LogNotificationSender` (log mode) and `RecordingNotificationSender` fill that role.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
