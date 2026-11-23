# Graph Report - equis-api  (2026-11-23)

## Corpus Check
- 235 files · ~225,939 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 6163 nodes · 25198 edges · 289 communities (93 shown, 196 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 1880 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Account & Onboarding Services
- Account Journey ITs & Fixtures
- Tenant Context & Mail ITs
- REST Controllers
- Assessment Booking Domain
- Error Codes & Constraints
- Repository & Config ITs
- Rider Balance & Usage ITs
- Rider App Queries & Payments
- Account Links & Tokens
- Session Templates
- Session Jobs & Listeners
- Session Entity
- Admin Oversight Journeys
- Report Rows
- Finish Session & My Week ITs
- Booking Controller & Security
- Rider & Level Changes
- Intake & Booking Oversight
- Admin Rider Management
- Admin Booking Change IT
- Club Access Filtering
- Admin Overview
- Rider Balance API & Security Tests
- Migration & Seed ITs
- Multi-Club Isolation ITs
- Platform Accounts
- API Exceptions & Journeys
- My Riders & Evaluations
- Session Evaluation IT
- Excel Report Writer
- Trainer Access
- Session Template IT
- Admin Booking IT
- Rider Accounts Scenarios
- Booking Actions & Phone Tests
- Rider Reassign IT
- Tenant Transactions
- Plan & Subscription IT
- CI Workflow
- Admin Overview IT
- Assessment Slot Query IT
- Request Inputs & Phone Normalizer
- Platform Account Action Service Test
- Platform Club Api IT
- Two Club Fixture
- Level Change Flow IT
- Invite Repository IT
- Booking Reminder Api IT
- Report Definition
- Subscription Service
- Admin Trainer Api IT
- Platform Subscription Api IT
- Auth Token Service IT
- Level Change Service IT
- Access Reason
- Admin Rider Api IT
- Assign On The Day Api IT
- Fresh Database
- release
- Prod Demo Data Cleanup IT
- Admin Member Close Api IT
- Club Access State IT
- Auth Service IT
- Admin Lockout Service IT
- Rider Balances Report
- Level Change Api IT
- Payments Scenario IT
- Admin Member Api IT
- Session Evaluations Scenario IT
- Rider Balances Report IT
- Rider App Api IT
- Assignment Service IT
- Rider Repository IT
- Platform Account Controller Test
- Evaluation Follow Up Query
- Platform Account Reset Demo Mode IT
- Session Usage Service IT
- Admin Trainer Actions Api IT
- Api Exception Handler
- Notice Type
- Expired Sessions Report IT
- Payments Report IT
- Smtp Notification Sender
- Account Link Emails Test
- Console Finished Sessions Api IT
- Platform Account Reset IT
- Club Repository IT
- Payment Repository IT
- My Riders Query Service IT
- Jwt Config
- Rider Level Query Service IT
- E19Demo IT
- Plan Limit Api IT
- Evaluation Follow Up Api IT
- Level Change Rules Test
- Prod Environment Check Test
- Tenant Isolation IT
- Admin Payment Void Api IT
- Report Export IT
- Xlsx Report Writer Test
- Booking Action
- Auth User Converter Test
- My Sessions Controller Test
- Auth User Converter
- Rider Evaluation History Api IT
- Session Template Rules IT
- Rider Balance Service IT
- Level Sessions Response
- Plan
- Assessment Booking Test
- Expired Sessions Report
- All Types Report
- Admin Payment Api IT
- Platform Account Api IT
- Session Template Controller Test
- Platform Sign In Journey IT
- Trainer Repository IT
- Platform Account Activity IT
- Endpoint Isolation IT
- Assessment Rules IT
- Token Generator Test
- Session Evaluation Repository IT
- Trainer Row Assembler
- Admin Member Edit Api IT
- Admin Oversight Fixture
- Session Repository IT
- Report Registry Test
- Payment State
- Member Status
- Club Admin Service
- Auth Controller
- Password Service Test
- Assessment Booking Repository IT
- Account Link Service IT
- Record Result Service IT
- Report Params Factory Test (1)
- Subject Type
- My Riders Controller Test
- Member
- Payments Report
- Admin Assessment Slot Api IT
- E17Demo IT
- Level Change Repository IT
- My Levels Service IT
- Session Detail Service IT
- Club Access Service Test
- Mail Settings
- Payment Test
- Assignment Repository IT
- Session Template Repository IT
- Member Booking Service IT
- Clock Config
- Tenant Auto Configuration
- E18Demo IT
- Account Link Emails
- Session Template
- Recording Notification Sender
- Session Generator IT
- Auth Service
- Auth Properties
- Invite
- compose
- Default Club Extension
- Cancel Reason
- Trainers Report
- Chunks
- My Levels Controller Test
- Auth Tokens (1)
- CHANGELOG (1)
- README (1)
- Auth Tokens (2)
- Payment
- Payment Method
- Club Sort
- CHANGELOG (2)
- Endpoint Coverage Guard IT
- Lockable Account
- Rider Account Status
- Report Params
- Tenant Identifier Resolver
- Mail Test Config
- Invite Purpose
- Notice
- Platform Account Action
- Mutable Clock
- CLAUDE (1)
- Subscription Status
- Limited Resource
- Report Params Factory Test (2)
- README (2)
- CHANGELOG (3)
- Club Usage Service
- Slot Booked Count
- Trainer Level Count
- CHANGELOG (4)
- CHANGELOG (5)
- Start Mode
- Level Rider Count
- CLAUDE (2)
- README (3)
- CHANGELOG (6)
- pom

## God Nodes (most connected - your core abstractions)
1. `RiderRepository` - 167 edges
2. `IntegrationTestBase` - 142 edges
3. `TrainerRepository` - 137 edges
4. `ApiException` - 116 edges
5. `Session` - 106 edges
6. `AdminOversightFixture` - 83 edges
7. `Rider` - 81 edges
8. `PostgresContainer` - 80 edges
9. `ErrorCodes` - 76 edges
10. `Trainer` - 71 edges

## Surprising Connections (you probably didn't know these)
- `Club access state (CLUB_BLOCKED, SUBSCRIPTION_EXPIRED read-only)` --semantically_similar_to--> `Plan on_expiry (BLOCK / READ_ONLY)`  [INFERRED] [semantically similar]
  CHANGELOG.md → ops/README.md
- `graphify skill trigger (/graphify)` --conceptually_related_to--> `graphify RAG index (graphify-out)`  [INFERRED]
  .claude/CLAUDE.md → CLAUDE.md
- `Platform super admin` --references--> `ops/create-super-admin.sql`  [EXTRACTED]
  CHANGELOG.md → ops/README.md
- `Plans (Trial, Basic, Pro, Max) and PLAN_LIMIT_REACHED` --references--> `ops/upsert-plan.sql`  [EXTRACTED]
  CHANGELOG.md → ops/README.md
- `SameDayJourneys` --references--> `AdminOversightFixture`  [EXTRACTED]
  src/test/java/com/equestrian/equis/support/SameDayJourneys.java → src/test/java/com/equestrian/equis/support/AdminOversightFixture.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Multi-tenant platform: tenancy, super admin, plans and access state** — changelog_e17_multi_tenancy, changelog_platform_super_admin, changelog_plans_and_limits, changelog_club_access_state, ops_readme_create_super_admin, ops_readme_upsert_plan [INFERRED 0.85]
- **Row-level security enforcement across migrations, report views and tests** — changelog_v24_row_level_security, changelog_security_invoker_report_views, claude_default_club_tests, changelog_e17_multi_tenancy [INFERRED 0.85]
- **Mail isolation in tests** — claude_no_real_email_in_tests, claude_email_tag_tests, claude_known_gaps [INFERRED 0.85]

## Communities (289 total, 196 thin omitted)

### Community 0 - "Account & Onboarding Services"
Cohesion: 0.04
Nodes (23): ClubUsageView, Count, AdminAccountRepository, TrainerRepository, MemberRepository, ClubRepository, PlatformAccountActionRepository, RiderRepository (+15 more)

### Community 1 - "Account Journey ITs & Fixtures"
Cohesion: 0.05
Nodes (10): DemoModeIT, DemoModeMailOnIT, TrainerAdminAccessIT, TrainerDisableJourneyIT, Request, TrainerInviteJourneyIT, TrainerResetJourneyIT, RiderAccountJourneyIT (+2 more)

### Community 2 - "Tenant Context & Mail ITs"
Cohesion: 0.07
Nodes (4): AccountLinkMailListenerIT, IntegrationTestBase, TestSubscriptions, ClubData

### Community 3 - "REST Controllers"
Cohesion: 0.04
Nodes (40): AuthUser, AdminTrainerController, AdminAssessmentSlotController, AdminBookingController, AssessmentSlotController, AdminClubUsageController, ClubStatusController, MyEvaluationsController (+32 more)

### Community 4 - "Assessment Booking Domain"
Cohesion: 0.03
Nodes (40): AdminMemberController, BookingController, AdminBookingList, AdminBookingRow, AdminNewMemberRequest, AssessmentSlotDto, BookingRowDto, BookingTotals (+32 more)

### Community 5 - "Error Codes & Constraints"
Cohesion: 0.05
Nodes (20): ApiException, ApiFieldError, Constraints, ErrorCodes, TenantTransactions, DateRanges, ClubPage, TrainerStatus (+12 more)

### Community 6 - "Repository & Config ITs"
Cohesion: 0.03
Nodes (14): AuthPropertiesTest, JwtConfigTest, MailConfigTest, MailDemoModeConfigTest, SameDaySessionsScenarioIT, CancelReasonTest, MemberStatusTest, PaymentEnumsTest (+6 more)

### Community 7 - "Rider Balance & Usage ITs"
Cohesion: 0.09
Nodes (8): FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, PostgresContainer, Config

### Community 9 - "Rider App Queries & Payments"
Cohesion: 0.04
Nodes (35): AdminPaymentController, BalanceDto, PaymentEntry, PaymentResult, PaymentUsage, RecordPaymentRequest, RiderPayments, VoidPaymentRequest (+27 more)

### Community 10 - "Account Links & Tokens"
Cohesion: 0.08
Nodes (5): InviteRepository, RefreshTokenRepository, SuperAdminRepository, AccountLinkService, IssuedLink

### Community 11 - "Session Templates"
Cohesion: 0.06
Nodes (21): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateUpdateRequest, LevelRepository, SessionRepository (+13 more)

### Community 12 - "Session Jobs & Listeners"
Cohesion: 0.05
Nodes (19): EvaluationRequest, PendingEvaluations, PendingSession, PendingEvaluationRow, SessionEvaluationRepository, AssignmentRepository, BookingCancellationListener, EvaluationService (+11 more)

### Community 13 - "Session Entity"
Cohesion: 0.07
Nodes (12): SessionDetailMapper, SeatEvaluation, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse, SessionEvaluation, Assignment (+4 more)

### Community 15 - "Admin Oversight Journeys"
Cohesion: 0.09
Nodes (6): AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture, Coach, Config

### Community 16 - "Report Rows"
Cohesion: 0.18
Nodes (8): TenantGuard, RefreshToken, Club, SuperAdmin, ExpiredPackageReportRow, PaymentReportRow, RiderReportRow, TrainerReportRow

### Community 17 - "Finish Session & My Week ITs"
Cohesion: 0.08
Nodes (7): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED, MyWeekApiIT, Request

### Community 18 - "Booking Controller & Security"
Cohesion: 0.09
Nodes (4): SecurityConfig, BookingControllerTest, ActiveClubAccess, Config

### Community 19 - "Rider & Level Changes"
Cohesion: 0.08
Nodes (8): Trainer, Level, LevelChange, Rider, NoSessionsLeftException, SessionEvaluationTest, LevelChangeTest, RiderTest

### Community 20 - "Intake & Booking Oversight"
Cohesion: 0.14
Nodes (4): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person

### Community 21 - "Admin Rider Management"
Cohesion: 0.08
Nodes (16): TrainerRef, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard, AdminRiderView (+8 more)

### Community 23 - "Club Access Filtering"
Cohesion: 0.07
Nodes (5): ClubAccessFilter, TenantFilter, ClubBlockedException, SubscriptionExpiredException, ClubAccessFilterTest

### Community 24 - "Admin Overview"
Cohesion: 0.07
Nodes (14): SchoolWeek, AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill (+6 more)

### Community 25 - "Rider Balance API & Security Tests"
Cohesion: 0.08
Nodes (3): SecurityConfigTest, AssessmentSlotControllerTest, AdminRiderBalanceApiIT

### Community 26 - "Migration & Seed ITs"
Cohesion: 0.08
Nodes (4): SeedDataIT, EmptyDatabaseStartIT, MigrationFromRelease2IT, TestPostgres

### Community 28 - "Platform Accounts"
Cohesion: 0.11
Nodes (15): AccountFilter, AccountPage, AccountType, ADMIN, RIDER, TRAINER, PlatformAccountRow, PlatformAccountStatus (+7 more)

### Community 29 - "API Exceptions & Journeys"
Cohesion: 0.09
Nodes (4): ApiExceptionHandlerTest, PaymentJourneys, Check, SameDayJourneys

### Community 30 - "My Riders & Evaluations"
Cohesion: 0.09
Nodes (14): MyRidersController, RiderEvaluation, RiderEvaluations, LevelChanged, MyRiderDto, RiderLevelDto, UpcomingSeat, EvaluationHistoryRow (+6 more)

### Community 31 - "Session Evaluation IT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 32 - "Excel Report Writer"
Cohesion: 0.10
Nodes (9): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, ReportColumn (+1 more)

### Community 36 - "Rider Accounts Scenarios"
Cohesion: 0.18
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 37 - "Booking Actions & Phone Tests"
Cohesion: 0.10
Nodes (4): BookingActions, PhoneNormalizerTest, LevelRepositoryIT, BookingActionsTest

### Community 39 - "Tenant Transactions"
Cohesion: 0.10
Nodes (6): TenantContext, Outcome, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, ClubThreads

### Community 41 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 44 - "Request Inputs & Phone Normalizer"
Cohesion: 0.12
Nodes (3): PhoneNormalizer, ClubAdminInput, CreateClubRequest

### Community 47 - "Two Club Fixture"
Cohesion: 0.14
Nodes (3): RowLevelSecurityIT, Builder, TwoClubFixture

### Community 50 - "Booking Reminder Api IT"
Cohesion: 0.17
Nodes (3): BookingReminderApiIT, NoticeRow, Request

### Community 51 - "Report Definition"
Cohesion: 0.12
Nodes (6): ReportController, ReportDefinition, ReportExportService, ReportRegistry, ReportWriter, XlsxReportWriter

### Community 52 - "Subscription Service"
Cohesion: 0.13
Nodes (5): SubscriptionResult, SubscriptionView, Subscription, SubscriptionRepository, SubscriptionService

### Community 55 - "Auth Token Service IT"
Cohesion: 0.14
Nodes (3): AuthTokenService, Issued, AuthTokenServiceIT

### Community 57 - "Access Reason"
Cohesion: 0.11
Nodes (25): ChangeClubStatusRequest, ClubAccessView, Admin, ClubDetail, ClubRow, AdminStatus, ACTIVE, INVITED (+17 more)

### Community 59 - "Assign On The Day Api IT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 61 - "release"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 62 - "Prod Demo Data Cleanup IT"
Cohesion: 0.15
Nodes (4): ProdDemoDataCleanupIT, ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT

### Community 67 - "Rider Balances Report"
Cohesion: 0.15
Nodes (4): RiderReportRepository, RiderBalanceReportRow, RiderBalancesReport, RiderBalancesReportTest

### Community 76 - "Platform Account Controller Test"
Cohesion: 0.14
Nodes (3): AccountActivityPage, AccountActivityRow, PlatformAccountControllerTest

### Community 77 - "Evaluation Follow Up Query"
Cohesion: 0.15
Nodes (15): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, SessionStatus (+7 more)

### Community 82 - "Notice Type"
Cohesion: 0.11
Nodes (7): DbConverter, DbConverter, DbConverter, DbConverter, DbConverter, DayOfWeekConverter, DbConverter

### Community 92 - "Jwt Config"
Cohesion: 0.13
Nodes (3): JwtConfig, AccessToken, JwtService

### Community 97 - "Level Change Rules Test"
Cohesion: 0.14
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 98 - "Prod Environment Check Test"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 102 - "Xlsx Report Writer Test"
Cohesion: 0.18
Nodes (3): Line, XlsxReportWriterTest, AllTypesReport

### Community 103 - "Booking Action"
Cohesion: 0.12
Nodes (10): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+2 more)

### Community 106 - "Auth User Converter"
Cohesion: 0.14
Nodes (7): AuthUserAuthentication, AuthUserConverter, Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER

### Community 110 - "Level Sessions Response"
Cohesion: 0.17
Nodes (10): LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyWeek, WeekDay, WeekSession (+2 more)

### Community 111 - "Plan"
Cohesion: 0.13
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 113 - "Expired Sessions Report"
Cohesion: 0.17
Nodes (4): ExpiredPackageReportRepository, ExpiredSessionsReport, ReportTotal, ExpiredSessionsReportTest

### Community 121 - "Endpoint Isolation IT"
Cohesion: 0.18
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 123 - "Token Generator Test"
Cohesion: 0.17
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 125 - "Trainer Row Assembler"
Cohesion: 0.12
Nodes (5): LevelCount, PendingLink, RiderCounts, AssessorCount, TrainerRowAssembler

### Community 129 - "Report Registry Test"
Cohesion: 0.18
Nodes (3): Named, ReportRegistryTest, Totalled

### Community 130 - "Payment State"
Cohesion: 0.18
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 131 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 132 - "Club Admin Service"
Cohesion: 0.24
Nodes (4): ClubAdminService, Entry, ClubFilter, Usage

### Community 133 - "Auth Controller"
Cohesion: 0.14
Nodes (5): AuthController, InviteAccepted, InviteAcceptRequest, LoginRequest, RefreshRequest

### Community 134 - "Password Service Test"
Cohesion: 0.22
Nodes (3): PasswordService, CountingEncoder, PasswordServiceTest

### Community 139 - "Subject Type"
Cohesion: 0.17
Nodes (7): DbConverter, SubjectType, ADMIN, RIDER, SUPER_ADMIN, TRAINER, RoleTest

### Community 142 - "Payments Report"
Cohesion: 0.22
Nodes (3): PaymentReportRepository, PaymentsReport, PaymentsReportTest

### Community 149 - "Mail Settings"
Cohesion: 0.18
Nodes (5): MailConfig, MailSettings, Mode, LOG, SMTP

### Community 154 - "Clock Config"
Cohesion: 0.26
Nodes (4): ClockConfig, SchedulingConfig, SecurityBeans, TestConfig

### Community 157 - "Account Link Emails"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 162 - "Auth Properties"
Cohesion: 0.23
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, SessionProperties

### Community 164 - "compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 166 - "Cancel Reason"
Cohesion: 0.20
Nodes (8): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER

### Community 168 - "Chunks"
Cohesion: 0.23
Nodes (3): Chunks, Iterator<>, ChunksTest

### Community 170 - "Auth Tokens (1)"
Cohesion: 0.27
Nodes (3): AuthTokens, ClubRef, PlatformLoginRequest

### Community 171 - "CHANGELOG (1)"
Cohesion: 0.22
Nodes (10): E10 Release packaging (Docker image, prod demo-data removal), E2 Session setup, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, GHCR image release pipeline (GitHub Actions), Release 1.0.0 (first release, E1-E10) (+2 more)

### Community 172 - "README (1)"
Cohesion: 0.22
Nodes (10): E1 Auth & accounts, E6 Trainer accounts, APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (smtp|log), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend) (+2 more)

### Community 173 - "Auth Tokens (2)"
Cohesion: 0.33
Nodes (3): Access, ClubStatusView, BlockedSignIn

### Community 176 - "Payment Method"
Cohesion: 0.24
Nodes (6): PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 177 - "Club Sort"
Cohesion: 0.22
Nodes (4): ClubSort, CODE, ENDS_AT, NAME

### Community 178 - "CHANGELOG (2)"
Cohesion: 0.28
Nodes (9): APP_RIDER_BASE_URL configuration, E11 Admin intake, E12 Payments and balance, E13 Same-day sessions, E14 Session evaluations, E15 Rider accounts and data, E3 Scheduling riders, E4 Assessment (+1 more)

### Community 181 - "Rider Account Status"
Cohesion: 0.31
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 185 - "Invite Purpose"
Cohesion: 0.36
Nodes (4): DbConverter, InvitePurpose, INVITE, RESET

### Community 187 - "Platform Account Action"
Cohesion: 0.32
Nodes (3): AccountActionType, RESET_PASSWORD, PlatformAccountAction

### Community 189 - "CLAUDE (1)"
Cohesion: 0.38
Nodes (4): @Tag("email") mail tests and email-tests profile, JaCoCo merged coverage floors (coverage.minimum.*), Known test gaps (no test profile, no dedicated no-op sender), Testing rules (Surefire *Test / Failsafe *IT)

### Community 190 - "Subscription Status"
Cohesion: 0.29
Nodes (6): SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED

### Community 191 - "Limited Resource"
Cohesion: 0.38
Nodes (4): LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException

### Community 193 - "README (2)"
Cohesion: 0.47
Nodes (6): Club access state (CLUB_BLOCKED, SUBSCRIPTION_EXPIRED read-only), ops/create-super-admin.sql, Local development ops (Compose postgres localhost:5433), Plan on_expiry (BLOCK / READ_ONLY), Ops scripts (run by hand, never by Flyway), ops/upsert-plan.sql

### Community 194 - "CHANGELOG (3)"
Cohesion: 0.47
Nodes (6): E17 Multi-tenancy (club_id, @TenantId, row-level security), Per-club sign-in (club code required, breaking), Plans (Trial, Basic, Pro, Max) and PLAN_LIMIT_REACHED, Release 3.0.0 (Multi-tenancy), V24__row_level_security.sql (creates role equis_app), Every test runs in the default club (DefaultClubExtension, TenantContext.runAs)

### Community 198 - "CHANGELOG (4)"
Cohesion: 0.67
Nodes (3): E18 Reports foundation (.xlsx export via Apache POI), E19 Money reports (Payments, Rider balances, Expired sessions), Release 3.1.0 (Reports)

### Community 199 - "CHANGELOG (5)"
Cohesion: 0.67
Nodes (4): E22 Accounts administration (platform super admin acts on club accounts), Platform super admin, Release 3.2.0 (Accounts administration), V32__platform_account_action.sql

### Community 200 - "Start Mode"
Cohesion: 0.50
Nodes (3): StartMode, AFTER_CURRENT, NOW

### Community 204 - "README (3)"
Cohesion: 0.67
Nodes (3): APP_DEFAULT_CLUB_CODE / APP_DEFAULT_CLUB_NAME, equis_app database role (SET ROLE), Row-level security per club (V23, V24)

## Knowledge Gaps
- **131 isolated node(s):** `CLOSED`, `NO_SHOW`, `PENDING_ASSESSMENT`, `RIDER`, `INVITE` (+126 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 629 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **196 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RiderRepository` connect `Account & Onboarding Services` to `Account Journey ITs & Fixtures`, `Tenant Context & Mail ITs`, `Assessment Booking Domain`, `Error Codes & Constraints`, `Rider Balance & Usage ITs`, `Assessment Booking Repository IT`, `Rider App Queries & Payments`, `Account Links & Tokens`, `Session Templates`, `Session Jobs & Listeners`, `My Riders Controller Test`, `Auth Users & Roles`, `Record Result Service IT`, `Level Change Repository IT`, `Booking Controller & Security`, `Rider & Level Changes`, `My Levels Service IT`, `Admin Rider Management`, `Session Detail Service IT`, `Club Access Filtering`, `Assignment Repository IT`, `Rider Balance API & Security Tests`, `My Riders & Evaluations`, `Trainer Access`, `Auth Service`, `Session Template IT`, `My Levels Controller Test`, `Platform Account Action Service Test`, `Level Change Flow IT`, `Admin Trainer Api IT`, `Auth Token Service IT`, `Level Change Service IT`, `Admin Rider Api IT`, `Level Change Api IT`, `Assignment Service IT`, `Rider Repository IT`, `Platform Account Controller Test`, `Session Usage Service IT`, `Admin Trainer Actions Api IT`, `Payment Repository IT`, `My Riders Query Service IT`, `Rider Level Query Service IT`, `Auth User Converter Test`, `My Sessions Controller Test`, `Auth User Converter`, `Rider Balance Service IT`, `Session Template Controller Test`, `Assessment Rules IT`, `Session Evaluation Repository IT`, `Trainer Row Assembler`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **What connects `CLOSED`, `NO_SHOW`, `PENDING_ASSESSMENT` to the rest of the system?**
  _131 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Account & Onboarding Services` be split into smaller, more focused modules?**
  _Cohesion score 0.0392156862745098 - nodes in this community are weakly interconnected._
- **Why does `TrainerRepository` connect `Account & Onboarding Services` to `Account Journey ITs & Fixtures`, `Tenant Context & Mail ITs`, `Assessment Booking Domain`, `Error Codes & Constraints`, `Repository & Config ITs`, `Rider Balance & Usage ITs`, `Assessment Booking Repository IT`, `Account Link Service IT`, `Account Links & Tokens`, `Record Result Service IT`, `Session Jobs & Listeners`, `My Riders Controller Test`, `Auth Users & Roles`, `Level Change Repository IT`, `Booking Controller & Security`, `Rider & Level Changes`, `My Levels Service IT`, `Admin Rider Management`, `Session Detail Service IT`, `Club Access Filtering`, `Admin Overview`, `Rider Balance API & Security Tests`, `Migration & Seed ITs`, `Assignment Repository IT`, `Member Booking Service IT`, `My Riders & Evaluations`, `Trainer Access`, `Auth Service`, `Session Template IT`, `My Levels Controller Test`, `Assessment Slot Query IT`, `Platform Account Action Service Test`, `Level Change Flow IT`, `Invite Repository IT`, `Admin Trainer Api IT`, `Auth Token Service IT`, `Level Change Service IT`, `Auth Service IT`, `Level Change Api IT`, `Assignment Service IT`, `Rider Repository IT`, `Platform Account Controller Test`, `Platform Account Reset Demo Mode IT`, `Admin Trainer Actions Api IT`, `My Riders Query Service IT`, `Rider Level Query Service IT`, `Auth User Converter Test`, `My Sessions Controller Test`, `Auth User Converter`, `Session Template Controller Test`, `Trainer Repository IT`, `Assessment Rules IT`, `Session Evaluation Repository IT`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Should `Account Journey ITs & Fixtures` be split into smaller, more focused modules?**
  _Cohesion score 0.045745920745920744 - nodes in this community are weakly interconnected._
- **Why does `TwoClubFixture` connect `Two Club Fixture` to `Club Access State IT`, `Tenant Context & Mail ITs`, `Report Export IT`, `Rider Balance & Usage ITs`, `Platform Account Reset IT`, `Rider Balances Report IT`, `Assessment Slot DTOs`, `Auth Users & Roles`, `Expired Sessions Report IT`, `Platform Account Api IT`, `Payments Report IT`, `Platform Account Activity IT`, `Endpoint Isolation IT`, `Multi-Club Isolation ITs`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **Should `Tenant Context & Mail ITs` be split into smaller, more focused modules?**
  _Cohesion score 0.0679803982900636 - nodes in this community are weakly interconnected._