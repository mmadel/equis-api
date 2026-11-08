# Graph Report - equis-api  (2026-11-08)

## Corpus Check
- 571 files · ~186,392 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: .properties 6, (none) 4, .imports 2)

## Summary
- 5230 nodes · 24284 edges · 201 communities (83 shown, 118 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 2891 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ea47f1cc`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.springframework.web.bind.annotation.RestController
- Spring Boot Test Wiring
- Session Templates & Test Asserts
- SubscriptionService
- Admin Oversight Journeys
- Admin Member API Tests
- Error Codes & Validation
- TrainerAccountsFixture
- RecordResultServiceTest
- .evaluate
- Scenario Journey Tests
- Roles, Clubs & Threads
- AdminTrainerApiTest
- Test Fixtures & Clock
- .status
- uuid
- Exception Handler & Rules Tests
- RiderRepository
- AuthTokenService
- Assessment Slot Query
- org.springframework.test.web.servlet.MockMvc
- Admin Rider Rows & Reassign
- PlatformSignInJourneyTest
- Admin Booking Change Tests
- Rider App & Payments Query
- .sessions
- Rider
- Repository Layer
- Admin Overview & Bookings
- Trainer Admin Service
- Auth User Conversion
- Payments Scenario Tests
- Session Evaluation API Tests
- SessionTemplateServiceTest
- Admin Booking API Tests
- Auth Service & Invites
- AdminBookingRow
- Finish Session API Tests
- Phone Normalizer & Params
- Rider Reassign API Tests
- Plan & Subscription Model
- Intake Scenario Tests
- Admin Overview API Tests
- Flyway Migration Tests
- LevelChangeRepositoryTest
- Level Change Flow
- Trainer Access Service
- Rider Entity
- Level Change Service
- Console Finished Sessions
- Invite Repository Tests
- My Riders & Balance
- Rider App API & JWT
- ClubAccessStateTest
- AssessmentBooking
- Release Workflow
- Session Usage Service
- Rider Balance Service
- Rider Level Counts
- Assignment Service
- My Week API Tests
- API Exception Handler
- Jobs & Event Listeners
- Endpoint Isolation Tests
- Tenant Context
- Assign On The Day Tests
- InvitePurpose
- My Sessions & Evaluation
- Level Change API Tests
- Tenant-Aware DataSource
- Row-Level Security Tests
- Account Link Events
- Evaluation Follow-Up Tests
- Club Repository Tests
- Booking Actions
- Tenant Isolation Tests
- Payment State & Usage
- Session Repository
- BillingPeriod
- Rider Eval History Tests
- Payment Void API Tests
- Prod Environment Check
- Plans & Expiry Policy
- Payment Entity
- Payment Repository Tests
- Per-Club Sign-In Tests
- Admin Payment API Tests
- Mail & SMTP Tests
- Level Change Rules
- CI Workflow
- notblank
- PlatformClubController.java
- Evaluation Follow-Up
- Admin Lockout
- Trainer Repository
- AdminTrainerActionsApiTest
- Account Link Service
- Admin Member Edit Tests
- SecurityConfig.java
- Rider Level Query
- Rider Balance API Tests
- Two-Club Fixture
- Member Status
- Auth Tokens
- JWT Config
- Repository Isolation
- Roles & Subject Types
- Payment
- AuthUser
- .registerAndBook
- Notices
- Session Generator
- Admin Slot API Tests
- SecurityConfigTest
- SMTP Sender
- Template Mapper
- Template Controller Tests
- Evaluation Repository Tests
- Account Link Mail
- Rider Account Service
- Assignment Repository
- AssessmentRulesTest
- Session Detail Service
- Member Booking Tests
- org.springframework.context.annotation.Bean
- TrainerDisableJourneyTest
- AssessmentBookingRepositoryTest
- Cancel Reasons
- Docker Compose
- Default Club Extension
- Admin Account Repo
- Password Service
- .summaries
- Pending Evaluations
- SessionTemplateRepositoryTest
- Session Done Rules
- TenantAutoConfiguration.java
- MyRidersControllerTest
- Changelog Epics A
- LevelChangeRules
- .of
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- Tenant Identifier Resolver
- EndpointCoverageGuardTest
- Test Mail Rules Docs
- Test Club Rules Docs
- org.junit.jupiter.api.AfterEach
- Changelog Epics B
- AuthService.java
- RiderBalanceService
- Image Release Docs
- Notification Sender Docs
- SlotBookedCount
- Graphify Rules Docs
- Release 2.0.1
- Maven POM

## God Nodes (most connected - your core abstractions)
1. `Trainer` - 211 edges
2. `TestPostgres` - 188 edges
3. `Rider` - 185 edges
4. `Session` - 180 edges
5. `AuthUser` - 164 edges
6. `ApiException` - 146 edges
7. `SessionRepository` - 138 edges
8. `SessionTemplate` - 137 edges
9. `RiderRepository` - 135 edges
10. `LevelRepository` - 100 edges

## Surprising Connections (you probably didn't know these)
- `create-super-admin.sql` --semantically_similar_to--> `APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH`  [INFERRED] [semantically similar]
  ops/README.md → README.md
- `graphify skill (/graphify trigger)` --conceptually_related_to--> `graphify usage rules`  [INFERRED]
  .claude/CLAUDE.md → CLAUDE.md
- `E10 Release packaging` --conceptually_related_to--> `Docker image (Java 21 JRE + app.jar)`  [INFERRED]
  CHANGELOG.md → README.md
- `APP_RIDER_BASE_URL` --semantically_similar_to--> `APP_BASE_URL`  [INFERRED] [semantically similar]
  CHANGELOG.md → README.md
- `Tests never send real email (app.mail.mode=log)` --references--> `APP_MAIL_MODE (smtp|log)`  [INFERRED]
  CLAUDE.md → README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Test-time mail handling** — claude_mailtestconfig, claude_recordingnotificationsender, claude_lognotificationsender, claude_accountlinkmaillistener, claude_email_tag_tests [EXTRACTED 1.00]
- **Multi-tenant club isolation (RLS, equis_app role, test club context)** — readme_row_level_security, readme_equis_app_role, claude_default_club_in_tests, claude_defaultclubextension, claude_tenantcontext_runas, claude_clubthreads [INFERRED 0.85]
- **Platform operator setup via ops scripts** — ops_readme_create_super_admin, ops_readme_platform_login, ops_readme_upsert_plan, ops_readme_plan_tiers [INFERRED 0.85]

## Communities (201 total, 118 thin omitted)

### Community 0 - "org.springframework.web.bind.annotation.RestController"
Cohesion: 0.07
Nodes (15): AdminAssessmentSlotController, MyLevelsController, AdminPaymentController, PlatformAuthController, AdminRiderAccountController, AdminRiderController, MyWeekController, RefreshRequest (+7 more)

### Community 2 - "Session Templates & Test Asserts"
Cohesion: 0.09
Nodes (12): Constraints, AdminAccount, SessionTemplate, LevelRepository, AssignmentRepository, SessionRepository, SessionTemplateRepository, FixedClock (+4 more)

### Community 3 - "SubscriptionService"
Cohesion: 0.10
Nodes (15): PlatformSubscriptionController, AddSubscriptionRequest, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionStatus, CURRENT (+7 more)

### Community 4 - "Admin Oversight Journeys"
Cohesion: 0.06
Nodes (8): Request, AdminOversightAccessTest, ReminderJourneyTest, SessionEvaluationsScenarioTest, SharedSession, RiderReassignJourneyTest, AdminOversightFixture, Coach

### Community 5 - "Admin Member API Tests"
Cohesion: 0.06
Nodes (5): AdminMemberApiTest, AdminMemberCloseApiTest, BookingReminderApiTest, NoticeRow, Request

### Community 6 - "Error Codes & Validation"
Cohesion: 0.10
Nodes (8): ApiException, ApiFieldError, ErrorCodes, DateRanges, Levels, AccountErrors, TrainerErrors, ClubErrors

### Community 7 - "TrainerAccountsFixture"
Cohesion: 0.05
Nodes (14): AuthTokens, OutgoingEmail, Request, TrainerAdminAccessTest, TrainerInviteJourneyTest, TrainerResetJourneyTest, RiderAccountJourneyTest, OmarAndSara (+6 more)

### Community 8 - "RecordResultServiceTest"
Cohesion: 0.22
Nodes (3): RecordResultRequest, ResultRecorded, RecordResultServiceTest

### Community 9 - ".evaluate"
Cohesion: 0.12
Nodes (5): EvaluationService, AssignmentRules, AssignmentService, FinishSessionService, SessionAccess

### Community 11 - "Roles, Clubs & Threads"
Cohesion: 0.18
Nodes (8): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, Club, JwtService, ClubThreads

### Community 13 - "Test Fixtures & Clock"
Cohesion: 0.09
Nodes (5): TenantContext, Body, RowLevelSecurityTest, MutableClock, TestSubscriptions

### Community 15 - "uuid"
Cohesion: 0.06
Nodes (8): LevelChanged, UpcomingSeat, SessionStatus, CANCELLED, DONE, FINISHED, SCHEDULED, SessionSeatAssembler

### Community 16 - "Exception Handler & Rules Tests"
Cohesion: 0.04
Nodes (13): AuthPropertiesTest, JwtConfigTest, MailConfigTest, ApiExceptionHandlerTest, RoleTest, CancelReasonTest, MemberStatusTest, SessionEvaluationTest (+5 more)

### Community 17 - "RiderRepository"
Cohesion: 0.07
Nodes (29): AdminMemberController, BookingController, AdminAccountRepository, InviteRepository, TrainerRepository, AssessmentBookingRepository, MemberRepository, NoticeRepository (+21 more)

### Community 18 - "AuthTokenService"
Cohesion: 0.13
Nodes (5): ClubStatusController, ClubRef, AuthTokenService, Issued, ClubAccessService

### Community 19 - "Assessment Slot Query"
Cohesion: 0.12
Nodes (6): AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, AssessmentSlotQueryServiceTest

### Community 20 - "org.springframework.test.web.servlet.MockMvc"
Cohesion: 0.09
Nodes (6): MyLevelSummary, AssessmentSlotControllerTest, BookingControllerTest, MyLevelsControllerTest, ActiveClubAccess, Config

### Community 21 - "Admin Rider Rows & Reassign"
Cohesion: 0.08
Nodes (18): TrainerRef, AdminRiderRow, RiderTotals, TrainerCard, DbConverter, TrainerStatus, ACTIVE, DISABLED (+10 more)

### Community 24 - "Rider App & Payments Query"
Cohesion: 0.06
Nodes (20): ExpiringSoon, RiderAssessment, RiderBanners, RiderMe, RiderPackage, RiderPayment, RiderPayments, RiderProgress (+12 more)

### Community 25 - ".sessions"
Cohesion: 0.18
Nodes (9): LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyWeek, WeekDay, WeekSession (+1 more)

### Community 26 - "Rider"
Cohesion: 0.09
Nodes (14): TenantGuard, SeatEvaluation, RefreshToken, Member, SessionEvaluation, Level, Notice, SessionUsage (+6 more)

### Community 27 - "Repository Layer"
Cohesion: 0.11
Nodes (3): SuperAdmin, RefreshTokenRepository, SuperAdminRepository

### Community 28 - "Admin Overview & Bookings"
Cohesion: 0.10
Nodes (11): AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill, TrainerRiderCount (+3 more)

### Community 29 - "Trainer Admin Service"
Cohesion: 0.04
Nodes (14): AdminTrainerController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals (+6 more)

### Community 30 - "Auth User Conversion"
Cohesion: 0.15
Nodes (3): AuthUserAuthentication, AuthUserConverter, AuthUserConverterTest

### Community 32 - "Session Evaluation API Tests"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiTest, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceTest"
Cohesion: 0.06
Nodes (12): AssessmentSeatCounts, BookedSeats, DeleteImpact, OverlapRef, SessionsCancelledEvent, SessionTemplateInput, SessionTemplateRules, SessionTemplateService (+4 more)

### Community 35 - "Auth Service & Invites"
Cohesion: 0.15
Nodes (3): InviteAcceptRequest, LoginRequest, AuthServiceTest

### Community 36 - "AdminBookingRow"
Cohesion: 0.08
Nodes (12): AdminBookingController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest, ChangeBookingRequest, MemberRef, SlotRef (+4 more)

### Community 37 - "Finish Session API Tests"
Cohesion: 0.14
Nodes (5): FinishSessionApiTest, Request, State, CANCELLED, SCHEDULED

### Community 38 - "Phone Normalizer & Params"
Cohesion: 0.12
Nodes (3): PhoneNormalizerTest, LevelRepositoryTest, BookingActionsTest

### Community 39 - "Rider Reassign API Tests"
Cohesion: 0.08
Nodes (4): AdminRiderApiTest, Request, Request, RiderReassignApiTest

### Community 40 - "Plan & Subscription Model"
Cohesion: 0.11
Nodes (3): Subscription, SubscriptionRepository, PlanAndSubscriptionTest

### Community 41 - "Intake Scenario Tests"
Cohesion: 0.13
Nodes (4): BookingOversightJourneyTest, NoShowRebooked, IntakeScenarioTest, Person

### Community 43 - "Flyway Migration Tests"
Cohesion: 0.13
Nodes (4): ProdDemoDataCleanupTest, ApplicantIntakeMigrationTest, RiderAccountMigrationTest, SessionFinishedMigrationTest

### Community 51 - "My Riders & Balance"
Cohesion: 0.21
Nodes (3): BalanceDto, MyRiderDto, MyRidersQueryServiceTest

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 62 - "Jobs & Event Listeners"
Cohesion: 0.08
Nodes (11): BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING, DbConverter, ClubRepository, BookingCancellationListener (+3 more)

### Community 63 - "Endpoint Isolation Tests"
Cohesion: 0.14
Nodes (13): Case, EndpointIsolationTest, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+5 more)

### Community 64 - "Tenant Context"
Cohesion: 0.13
Nodes (3): TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 65 - "Assign On The Day Tests"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiTest, State, CANCELLED, FINISHED, SCHEDULED

### Community 66 - "InvitePurpose"
Cohesion: 0.09
Nodes (10): DbConverter, InvitePurpose, INVITE, RESET, DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED (+2 more)

### Community 70 - "Row-Level Security Tests"
Cohesion: 0.16
Nodes (3): CountingEncoder, Config, TwoClubFixture

### Community 74 - "Booking Actions"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 76 - "Payment State & Usage"
Cohesion: 0.09
Nodes (17): PaymentEntry, PaymentUsage, DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET (+9 more)

### Community 78 - "BillingPeriod"
Cohesion: 0.07
Nodes (25): ChangeClubStatusRequest, ClubAccessView, Admin, ClubRow, AdminStatus, ACTIVE, INVITED, ClubStatus (+17 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.15
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plans & Expiry Policy"
Cohesion: 0.14
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 88 - "Level Change Rules"
Cohesion: 0.14
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 89 - "CI Workflow"
Cohesion: 0.09
Nodes (21): env, runs-on, steps, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (+13 more)

### Community 90 - "notblank"
Cohesion: 0.11
Nodes (3): SessionProperties, PhoneNormalizer, ReassignRidersRequest

### Community 91 - "PlatformClubController.java"
Cohesion: 0.07
Nodes (16): Outcome, TenantTransactions, PlatformClubController, ClubAdminInput, ClubDetail, ClubPage, CreateClubRequest, ClubAdminService (+8 more)

### Community 92 - "Evaluation Follow-Up"
Cohesion: 0.19
Nodes (10): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, EvaluationFollowUpQuery (+2 more)

### Community 96 - "Account Link Service"
Cohesion: 0.13
Nodes (3): GeneratedToken, AccountLinkServiceTest, TokenGeneratorTest

### Community 98 - "SecurityConfig.java"
Cohesion: 0.07
Nodes (6): SecurityConfig, ClubAccessFilter, TenantFilter, ClubBlockedException, SubscriptionExpiredException, ClubAccessFilterTest

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 104 - "JWT Config"
Cohesion: 0.09
Nodes (9): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, JwtConfig (+1 more)

### Community 106 - "Roles & Subject Types"
Cohesion: 0.27
Nodes (6): DbConverter, SubjectType, ADMIN, RIDER, SUPER_ADMIN, TRAINER

### Community 107 - "Payment"
Cohesion: 0.16
Nodes (4): PaymentResult, Payment, PaymentRepository, AdminPaymentService

### Community 108 - "AuthUser"
Cohesion: 0.07
Nodes (20): AuthUser, AssessmentSlotController, RiderAppController, MyRidersController, MySessionsController, SessionDetailMapper, RiderEvaluation, RiderEvaluations (+12 more)

### Community 109 - ".registerAndBook"
Cohesion: 0.09
Nodes (3): UpdateMemberRequest, MemberTest, MemberRepositoryTest

### Community 114 - "SMTP Sender"
Cohesion: 0.11
Nodes (6): MailSettings, Mode, LOG, SMTP, LogNotificationSender, SmtpNotificationSender

### Community 115 - "Template Mapper"
Cohesion: 0.20
Nodes (9): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+1 more)

### Community 118 - "Account Link Mail"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 119 - "Rider Account Service"
Cohesion: 0.15
Nodes (7): RiderAccount, DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE, RiderAccountService

### Community 123 - "Member Booking Tests"
Cohesion: 0.29
Nodes (3): AdminNewMemberRequest, FixedClock, MemberBookingServiceTest

### Community 124 - "org.springframework.context.annotation.Bean"
Cohesion: 0.06
Nodes (14): ClockConfig, SchedulingConfig, SecurityBeans, FixedClock, FixedClock, FixedClock, FixedClock, FixedClock (+6 more)

### Community 127 - "Cancel Reasons"
Cohesion: 0.21
Nodes (9): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+1 more)

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "Password Service"
Cohesion: 0.24
Nodes (4): AdminLockoutService, PasswordService, PlatformAuthService, PasswordServiceTest

### Community 132 - ".summaries"
Cohesion: 0.14
Nodes (3): SchoolWeek, LevelSeatCount, LevelSessionCount

### Community 133 - "Pending Evaluations"
Cohesion: 0.39
Nodes (4): MyEvaluationsController, PendingEvaluations, PendingSession, PendingEvaluationQuery

### Community 136 - "TenantAutoConfiguration.java"
Cohesion: 0.16
Nodes (3): TenantAutoConfiguration, BeanPostProcessor, HibernateClock

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 140 - "LevelChangeRules"
Cohesion: 0.23
Nodes (3): LevelChangeRules, RiderLevelQueryService, RiderSeatQuery

### Community 142 - "Base URLs & Accounts Docs"
Cohesion: 0.29
Nodes (8): APP_RIDER_BASE_URL, E15 Rider accounts and data, E1 Auth & accounts, E6 Trainer accounts, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (smtp|log), Production environment variables

### Community 143 - "Ops Scripts & Plans Docs"
Cohesion: 0.25
Nodes (8): V5__seed_admin.sql, create-super-admin.sql, on_expiry policy (BLOCK | READ_ONLY), Ops scripts (run by hand), Plans TRIAL / BASIC / PRO / MAX, POST /api/platform/auth/login, upsert-plan.sql, APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH

### Community 147 - "Test Club Rules Docs"
Cohesion: 0.33
Nodes (6): ClubThreads, DefaultClubExtension, TenantContext.runAs, APP_DEFAULT_CLUB_CODE / APP_DEFAULT_CLUB_NAME, equis_app database role (SET ROLE), Row-level security per club (V23, V24)

### Community 149 - "Changelog Epics B"
Cohesion: 0.47
Nodes (6): E11 Admin intake, E12 Payments and balance, E13 Same-day sessions, E14 Session evaluations, E4 Assessment, Release 2.0.0

### Community 150 - "AuthService.java"
Cohesion: 0.09
Nodes (6): AuthController, Access, InviteAccepted, ClubStatusView, AuthService, BlockedSignIn

### Community 151 - "RiderBalanceService"
Cohesion: 0.11
Nodes (9): PendingEvaluationRow, SessionEvaluationRepository, SessionUsageRepository, UsedCount, PaymentEntryAssembler, RiderBalanceService, SessionUsageService, MyRidersQueryService (+1 more)

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

## Knowledge Gaps
- **115 isolated node(s):** `ADMIN`, `TRAINER`, `RIDER`, `LIST`, `OTHER` (+110 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 485 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **118 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `Error Codes & Validation` to `org.springframework.web.bind.annotation.RestController`, `Session Templates & Test Asserts`, `Password Service`, `SubscriptionService`, `.evaluate`, `Roles, Clubs & Threads`, `LevelChangeRules`, `Test Fixtures & Clock`, `.status`, `uuid`, `RiderRepository`, `Admin Rider Rows & Reassign`, `Rider App & Payments Query`, `.sessions`, `Rider`, `SessionTemplateServiceTest`, `AdminBookingRow`, `Plan & Subscription Model`, `Trainer Access Service`, `Assignment Service`, `API Exception Handler`, `My Sessions & Evaluation`, `Level Change Rules`, `PlatformClubController.java`, `Evaluation Follow-Up`, `Admin Lockout`, `Account Link Service`, `Payment`, `AuthUser`, `.registerAndBook`, `Template Mapper`, `Rider Account Service`, `AssessmentRulesTest`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **What connects `ADMIN`, `TRAINER`, `RIDER` to the rest of the system?**
  _115 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.springframework.web.bind.annotation.RestController` be split into smaller, more focused modules?**
  _Cohesion score 0.07055630936227951 - nodes in this community are weakly interconnected._
- **Why does `Session` connect `Rider` to `Session Templates & Test Asserts`, `Error Codes & Validation`, `Session Done Rules`, `.evaluate`, `Roles, Clubs & Threads`, `LevelChangeRules`, `Test Fixtures & Clock`, `uuid`, `Exception Handler & Rules Tests`, `RiderRepository`, `Assessment Slot Query`, `Rider App & Payments Query`, `.sessions`, `Repository Layer`, `Admin Overview & Bookings`, `SessionTemplateServiceTest`, `AdminBookingRow`, `Level Change Flow`, `Level Change Service`, `My Riders & Balance`, `AssessmentBooking`, `Session Usage Service`, `Rider Balance Service`, `Assignment Service`, `InvitePurpose`, `My Sessions & Evaluation`, `Level Change API Tests`, `Session Repository`, `Payment Repository Tests`, `Evaluation Follow-Up`, `Rider Level Query`, `Evaluation Repository Tests`, `Assignment Repository`, `AssessmentRulesTest`, `Session Detail Service`, `Member Booking Tests`, `AssessmentBookingRepositoryTest`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **Should `Spring Boot Test Wiring` be split into smaller, more focused modules?**
  _Cohesion score 0.05454545454545454 - nodes in this community are weakly interconnected._
- **Why does `Rider` connect `Rider` to `Session Templates & Test Asserts`, `Error Codes & Validation`, `RecordResultServiceTest`, `.evaluate`, `Roles, Clubs & Threads`, `LevelChangeRules`, `.of`, `Test Fixtures & Clock`, `uuid`, `Exception Handler & Rules Tests`, `RiderRepository`, `AdminTrainerApiTest`, `AuthService.java`, `Rider App & Payments Query`, `Repository Layer`, `Trainer Admin Service`, `SessionTemplateServiceTest`, `LevelChangeRepositoryTest`, `Level Change Flow`, `Trainer Access Service`, `Rider Entity`, `Level Change Service`, `My Riders & Balance`, `Session Usage Service`, `Rider Balance Service`, `Rider Level Counts`, `Assignment Service`, `Level Change API Tests`, `Payment Entity`, `Payment Repository Tests`, `Level Change Rules`, `AdminTrainerActionsApiTest`, `Rider Level Query`, `Payment`, `AuthUser`, `.registerAndBook`, `Evaluation Repository Tests`, `Rider Account Service`, `Assignment Repository`, `AssessmentRulesTest`, `Session Detail Service`, `TrainerDisableJourneyTest`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **Should `Session Templates & Test Asserts` be split into smaller, more focused modules?**
  _Cohesion score 0.0937031484257871 - nodes in this community are weakly interconnected._