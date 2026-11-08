# Graph Report - equis-api  (2026-11-08)

## Corpus Check
- 571 files · ~186,780 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: .properties 6, (none) 4, .imports 2)

## Summary
- 5242 nodes · 24257 edges · 203 communities (85 shown, 118 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 2913 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1e81c680`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AuthUser
- Spring Boot Test Wiring
- Session Templates & Test Asserts
- Subscription
- Admin Oversight Journeys
- Admin Member API Tests
- Error Codes & Validation
- TrainerAccountsFixture
- RecordResultServiceTest
- RiderAccountsScenarioTest
- Scenario Journey Tests
- clock
- AdminTrainerApiTest
- org.springframework.context.annotation.Import
- PlatformClubApiTest
- uuid
- Exception Handler & Rules Tests
- ClubOnboardingService.java
- PlatformSubscriptionApiTest
- Assessment Slot Query
- org.springframework.test.web.servlet.MockMvc
- Admin Rider Rows & Reassign
- PlatformSignInJourneyTest
- Admin Booking Change Tests
- RiderAppQueryService.java
- MyLevelsServiceTest
- Rider
- Repository Layer
- Admin Overview & Bookings
- TrainerRepository
- Auth User Conversion
- Payments Scenario Tests
- Session Evaluation API Tests
- SessionTemplateServiceTest
- Admin Booking API Tests
- Auth Service & Invites
- RiderRepository
- SessionTemplateRulesTest
- Phone Normalizer & Params
- Rider Reassign API Tests
- Plan & Subscription Model
- Intake Scenario Tests
- Admin Overview API Tests
- Flyway Migration Tests
- Level
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
- SessionUsageServiceTest
- RepositoryIsolationTest.java
- LevelRiderCount
- Assignment Service
- My Week API Tests
- API Exception Handler
- ClubRepository
- Endpoint Isolation Tests
- Tenant Context
- Assign On The Day Tests
- jakarta.persistence.AttributeConverter
- My Sessions & Evaluation
- Level Change API Tests
- TokenGenerator
- SubscriptionService
- InvitePurpose
- Evaluation Follow-Up Tests
- Club Repository Tests
- Booking Actions
- Tenant Isolation Tests
- PaymentState
- Session
- BillingPeriod
- Rider Eval History Tests
- Payment Void API Tests
- Prod Environment Check
- Plans & Expiry Policy
- Payment Entity
- SessionTemplateService
- Per-Club Sign-In Tests
- Admin Payment API Tests
- AccountLinkMailListenerTest.java
- Level Change Rules
- CI Workflow
- notblank
- PlatformClubController.java
- Evaluation Follow-Up
- Admin Lockout
- Trainer Repository
- .convert
- Account Link Service
- Admin Member Edit Tests
- SecurityConfig.java
- Rider Level Query
- Rider Balance API Tests
- Two-Club Fixture
- Member Status
- Auth Tokens
- Repository Isolation
- RefreshTokenRepositoryTest
- AuthProperties
- SessionEvaluationRepository
- .register
- Notices
- Session Generator
- Admin Slot API Tests
- SecurityConfigTest
- SmtpNotificationSenderTest.java
- Template Mapper
- .admin
- Evaluation Repository Tests
- SessionProperties
- RiderAccountStatus
- Assignment Repository
- Session Detail Service
- Member Booking Tests
- org.springframework.boot.context.properties.EnableConfigurationProperties
- AdminNewMemberRequest.java
- Invite
- Cancel Reasons
- Docker Compose
- Default Club Extension
- Admin Account Repo
- TemplateBookedSeats
- .summaries
- NoticeType
- SessionTemplateRepositoryTest
- SessionTemplateRulesOverlapsTest
- TenantAutoConfiguration.java
- JwtServiceTest.java
- Changelog Epics A
- AssignmentRepository
- TrainerStatus
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- AssessorCount
- Test Mail Rules Docs
- Test Club Rules Docs
- org.junit.jupiter.api.AfterEach
- Changelog Epics B
- AuthService.java
- Request
- Image Release Docs
- Notification Sender Docs
- StartMode
- BookingCancellationListener
- FixedClock
- Graphify Rules Docs
- Release 2.0.1
- Maven POM

## God Nodes (most connected - your core abstractions)
1. `Trainer` - 208 edges
2. `Rider` - 185 edges
3. `Session` - 177 edges
4. `TestPostgres` - 172 edges
5. `AuthUser` - 161 edges
6. `ApiException` - 144 edges
7. `RiderRepository` - 135 edges
8. `SessionTemplate` - 134 edges
9. `SessionRepository` - 132 edges
10. `TrainerRepository` - 97 edges

## Surprising Connections (you probably didn't know these)
- `create-super-admin.sql` --semantically_similar_to--> `APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH`  [INFERRED] [semantically similar]
  ops/README.md → README.md
- `graphify skill (/graphify trigger)` --conceptually_related_to--> `graphify usage rules`  [INFERRED]
  .claude/CLAUDE.md → CLAUDE.md
- `CreateClubRequest` --references--> `ClubAdminInput`  [EXTRACTED]
  src/main/java/com/equestrian/equis/dto/platform/CreateClubRequest.java → src/main/java/com/equestrian/equis/dto/platform/ClubAdminInput.java
- `E10 Release packaging` --conceptually_related_to--> `Docker image (Java 21 JRE + app.jar)`  [INFERRED]
  CHANGELOG.md → README.md
- `APP_RIDER_BASE_URL` --semantically_similar_to--> `APP_BASE_URL`  [INFERRED] [semantically similar]
  CHANGELOG.md → README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Test-time mail handling** — claude_mailtestconfig, claude_recordingnotificationsender, claude_lognotificationsender, claude_accountlinkmaillistener, claude_email_tag_tests [EXTRACTED 1.00]
- **Multi-tenant club isolation (RLS, equis_app role, test club context)** — readme_row_level_security, readme_equis_app_role, claude_default_club_in_tests, claude_defaultclubextension, claude_tenantcontext_runas, claude_clubthreads [INFERRED 0.85]
- **Platform operator setup via ops scripts** — ops_readme_create_super_admin, ops_readme_platform_login, ops_readme_upsert_plan, ops_readme_plan_tiers [INFERRED 0.85]

## Communities (203 total, 118 thin omitted)

### Community 0 - "AuthUser"
Cohesion: 0.06
Nodes (25): AuthUser, AdminAssessmentSlotController, AssessmentSlotController, MyEvaluationsController, MyLevelsController, AdminPaymentController, PlatformAuthController, PlatformClubController (+17 more)

### Community 2 - "Session Templates & Test Asserts"
Cohesion: 0.09
Nodes (22): Direction, DOWN, UP, LevelChanged, PaymentMethod, BANK_TRANSFER, CARD, CASH (+14 more)

### Community 3 - "Subscription"
Cohesion: 0.13
Nodes (9): SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED, SubscriptionView, Subscription (+1 more)

### Community 4 - "Admin Oversight Journeys"
Cohesion: 0.08
Nodes (6): Request, AdminOversightAccessTest, ReminderJourneyTest, RiderReassignJourneyTest, AdminOversightFixture, Coach

### Community 5 - "Admin Member API Tests"
Cohesion: 0.06
Nodes (7): AdminMemberApiTest, AdminMemberCloseApiTest, FinishSessionApiTest, Request, State, CANCELLED, SCHEDULED

### Community 6 - "Error Codes & Validation"
Cohesion: 0.09
Nodes (9): ApiException, ApiFieldError, Constraints, ErrorCodes, DateRanges, Levels, AccountErrors, TrainerErrors (+1 more)

### Community 7 - "TrainerAccountsFixture"
Cohesion: 0.06
Nodes (8): AuthTokens, OutgoingEmail, TrainerDisableJourneyTest, TrainerInviteJourneyTest, TrainerResetJourneyTest, RiderAccountJourneyTest, RecordingNotificationSender, TrainerAccountsFixture

### Community 8 - "RecordResultServiceTest"
Cohesion: 0.20
Nodes (4): RecordResultRequest, ResultRecorded, FixedClock, RecordResultServiceTest

### Community 9 - "RiderAccountsScenarioTest"
Cohesion: 0.16
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioTest, RiderLogin

### Community 10 - "Scenario Journey Tests"
Cohesion: 0.12
Nodes (4): SessionEvaluationsScenarioTest, SharedSession, SameDaySessionsScenarioTest, SameDayJourneys

### Community 11 - "clock"
Cohesion: 0.11
Nodes (11): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, Club, JwtService, Body (+3 more)

### Community 12 - "AdminTrainerApiTest"
Cohesion: 0.05
Nodes (5): AdminTrainerActionsApiTest, AdminTrainerApiTest, BookingReminderApiTest, NoticeRow, Request

### Community 13 - "org.springframework.context.annotation.Import"
Cohesion: 0.05
Nodes (6): HibernateClock, EndpointCoverageGuardTest, SeedDataTest, TestSubscriptions, Config, Config

### Community 16 - "Exception Handler & Rules Tests"
Cohesion: 0.04
Nodes (9): AuthPropertiesTest, JwtConfigTest, MailConfigTest, ApiExceptionHandlerTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, RiderRepositoryTest (+1 more)

### Community 17 - "ClubOnboardingService.java"
Cohesion: 0.13
Nodes (8): BookingController, RiderAccount, AdminAccountRepository, AccountLinkService, IssuedLink, RecordResultService, ClubOnboardingService, RiderAccountService

### Community 19 - "Assessment Slot Query"
Cohesion: 0.09
Nodes (9): AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, SlotBookedCount, AssessmentSlotMapper, AssessmentSlotQueryServiceTest (+1 more)

### Community 20 - "org.springframework.test.web.servlet.MockMvc"
Cohesion: 0.08
Nodes (7): SecurityConfig, AssessmentSlotControllerTest, BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, SessionTemplateControllerTest, ActiveClubAccess

### Community 21 - "Admin Rider Rows & Reassign"
Cohesion: 0.08
Nodes (16): TrainerRef, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard, AdminRiderView (+8 more)

### Community 24 - "RiderAppQueryService.java"
Cohesion: 0.05
Nodes (33): RiderAppController, PaymentEntry, PaymentResult, PaymentUsage, RiderPayments, VoidPaymentRequest, ExpiringSoon, RiderAssessment (+25 more)

### Community 25 - "MyLevelsServiceTest"
Cohesion: 0.11
Nodes (11): SessionDetailMapper, SeatEvaluation, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, SessionSeatAssembler (+3 more)

### Community 26 - "Rider"
Cohesion: 0.11
Nodes (10): TenantGuard, AdminAccount, RefreshToken, Member, SessionEvaluation, Notice, Payment, SessionUsage (+2 more)

### Community 28 - "Admin Overview & Bookings"
Cohesion: 0.09
Nodes (12): SchoolWeek, AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill (+4 more)

### Community 29 - "TrainerRepository"
Cohesion: 0.08
Nodes (13): AdminTrainerController, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals, InviteRepository (+5 more)

### Community 32 - "Session Evaluation API Tests"
Cohesion: 0.11
Nodes (5): Request, SessionEvaluationApiTest, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceTest"
Cohesion: 0.17
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceTest

### Community 35 - "Auth Service & Invites"
Cohesion: 0.13
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceTest

### Community 36 - "RiderRepository"
Cohesion: 0.04
Nodes (26): AdminBookingController, AdminMemberController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest, ChangeBookingRequest, MemberRef (+18 more)

### Community 38 - "Phone Normalizer & Params"
Cohesion: 0.09
Nodes (4): BookingActions, PhoneNormalizerTest, ClubAccessFilterTest, BookingActionsTest

### Community 39 - "Rider Reassign API Tests"
Cohesion: 0.08
Nodes (4): AdminRiderApiTest, Request, Request, RiderReassignApiTest

### Community 41 - "Intake Scenario Tests"
Cohesion: 0.13
Nodes (5): BookingOversightJourneyTest, NoShowRebooked, IntakeScenarioTest, Person, Schedule

### Community 43 - "Flyway Migration Tests"
Cohesion: 0.14
Nodes (4): ProdDemoDataCleanupTest, ApplicantIntakeMigrationTest, RiderAccountMigrationTest, SessionFinishedMigrationTest

### Community 44 - "Level"
Cohesion: 0.11
Nodes (3): Level, LevelChangeTest, LevelChangeRepositoryTest

### Community 51 - "My Riders & Balance"
Cohesion: 0.17
Nodes (5): BalanceDto, MyRiderDto, UpcomingSeat, FixedClock, MyRidersQueryServiceTest

### Community 54 - "AssessmentBooking"
Cohesion: 0.07
Nodes (8): AssessmentBooking, BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING, AssessmentBookingTest, AssessmentRulesTest

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 56 - "SessionUsageServiceTest"
Cohesion: 0.05
Nodes (6): UsedCount, PaymentRepositoryTest, FixedClock, RiderBalanceServiceTest, FixedClock, SessionUsageServiceTest

### Community 62 - "ClubRepository"
Cohesion: 0.09
Nodes (4): ClubRef, ClubRepository, AssignmentCancellationListener, SessionGenerationJob

### Community 63 - "Endpoint Isolation Tests"
Cohesion: 0.14
Nodes (13): Case, EndpointIsolationTest, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+5 more)

### Community 64 - "Tenant Context"
Cohesion: 0.10
Nodes (5): TenantContext, TenantIdentifierResolver, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 65 - "Assign On The Day Tests"
Cohesion: 0.13
Nodes (4): State, CANCELLED, FINISHED, SCHEDULED

### Community 66 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.14
Nodes (6): DbConverter, DbConverter, DbConverter, DbConverter, DayOfWeekConverter, DbConverter

### Community 69 - "TokenGenerator"
Cohesion: 0.17
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 70 - "SubscriptionService"
Cohesion: 0.17
Nodes (5): ClubAccessView, PlanView, SubscriptionResult, VoidSubscriptionRequest, SubscriptionService

### Community 71 - "InvitePurpose"
Cohesion: 0.08
Nodes (13): DbConverter, InvitePurpose, INVITE, RESET, DbConverter, SubjectType, ADMIN, RIDER (+5 more)

### Community 74 - "Booking Actions"
Cohesion: 0.12
Nodes (10): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+2 more)

### Community 76 - "PaymentState"
Cohesion: 0.22
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 77 - "Session"
Cohesion: 0.10
Nodes (4): Session, SessionAccess, SessionTest, SessionRepositoryTest

### Community 78 - "BillingPeriod"
Cohesion: 0.09
Nodes (19): ClubRow, ClubStatus, ACTIVE, SUSPENDED, BillingPeriod, ANNUAL, MONTHLY, TRIAL (+11 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plans & Expiry Policy"
Cohesion: 0.17
Nodes (5): OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 84 - "SessionTemplateService"
Cohesion: 0.18
Nodes (4): SessionTemplateController, BookedSeats, SessionTemplateRules, SessionTemplateService

### Community 87 - "AccountLinkMailListenerTest.java"
Cohesion: 0.13
Nodes (3): InviteTrainerRequest, AccountLinkMailListenerTest, MailTestConfig

### Community 89 - "CI Workflow"
Cohesion: 0.09
Nodes (21): env, runs-on, steps, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (+13 more)

### Community 91 - "PlatformClubController.java"
Cohesion: 0.06
Nodes (17): Outcome, TenantTransactions, ChangeClubStatusRequest, Admin, ClubDetail, AdminStatus, ACTIVE, INVITED (+9 more)

### Community 92 - "Evaluation Follow-Up"
Cohesion: 0.10
Nodes (18): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, RiderEvaluations (+10 more)

### Community 93 - "Admin Lockout"
Cohesion: 0.10
Nodes (6): LockableAccount, AdminLockoutService, PasswordService, AdminLockoutServiceTest, CountingEncoder, PasswordServiceTest

### Community 98 - "SecurityConfig.java"
Cohesion: 0.07
Nodes (7): ClubAccessFilter, TenantFilter, ClubStatusController, ClubAccessService, ClubBlockedException, SubscriptionExpiredException, Config

### Community 99 - "Rider Level Query"
Cohesion: 0.11
Nodes (5): LevelHistoryEntry, RiderLevelDto, Rider, NoSessionsLeftException, RiderLevelQueryServiceTest

### Community 102 - "Member Status"
Cohesion: 0.17
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "Auth Tokens"
Cohesion: 0.12
Nodes (5): AuthController, RefreshRequest, AuthTokenService, Issued, AuthTokenServiceTest

### Community 107 - "AuthProperties"
Cohesion: 0.23
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, JwtConfig

### Community 108 - "SessionEvaluationRepository"
Cohesion: 0.09
Nodes (14): MySessionsController, EvaluationRequest, PendingEvaluations, PendingSession, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse (+6 more)

### Community 114 - "SmtpNotificationSenderTest.java"
Cohesion: 0.06
Nodes (7): AccountLinkEmails, Texts, AccountLinkMailListener, LogNotificationSender, NotificationSender, SmtpNotificationSender, SmtpNotificationSenderTest

### Community 115 - "Template Mapper"
Cohesion: 0.24
Nodes (8): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse, SessionTemplateUpdateRequest

### Community 116 - ".admin"
Cohesion: 0.18
Nodes (3): OverlapRef, SaveResult, Warning

### Community 118 - "SessionProperties"
Cohesion: 0.29
Nodes (5): MailSettings, Mode, LOG, SMTP, SessionProperties

### Community 119 - "RiderAccountStatus"
Cohesion: 0.36
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 122 - "Session Detail Service"
Cohesion: 0.14
Nodes (4): Assignment, SessionDetail, FixedClock, SessionDetailServiceTest

### Community 123 - "Member Booking Tests"
Cohesion: 0.24
Nodes (3): AdminNewMemberRequest, FixedClock, MemberBookingServiceTest

### Community 124 - "org.springframework.boot.context.properties.EnableConfigurationProperties"
Cohesion: 0.24
Nodes (4): MailConfig, SchedulingConfig, SecurityBeans, TestConfig

### Community 127 - "Cancel Reasons"
Cohesion: 0.20
Nodes (8): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 133 - "NoticeType"
Cohesion: 0.39
Nodes (4): DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED

### Community 136 - "TenantAutoConfiguration.java"
Cohesion: 0.14
Nodes (3): TenantAutoConfiguration, BeanPostProcessor, TenantAwareDataSource

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 140 - "AssignmentRepository"
Cohesion: 0.08
Nodes (14): MyWeek, WeekDay, WeekSession, AssignmentRepository, MyLevelsService, LevelChangeRules, LevelChangeService, LevelHistoryQuery (+6 more)

### Community 141 - "TrainerStatus"
Cohesion: 0.40
Nodes (4): TrainerStatus, ACTIVE, DISABLED, INVITED

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
Cohesion: 0.11
Nodes (4): Access, ClubStatusView, AuthService, BlockedSignIn

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

### Community 154 - "StartMode"
Cohesion: 0.50
Nodes (3): StartMode, AFTER_CURRENT, NOW

## Knowledge Gaps
- **115 isolated node(s):** `CLOSED`, `NO_SHOW`, `PENDING_ASSESSMENT`, `RIDER`, `ADMIN` (+110 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 488 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **118 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Rider` connect `Rider Level Query` to `Session Templates & Test Asserts`, `Error Codes & Validation`, `TrainerAccountsFixture`, `clock`, `AssignmentRepository`, `AdminTrainerApiTest`, `Exception Handler & Rules Tests`, `ClubOnboardingService.java`, `AuthService.java`, `RiderAppQueryService.java`, `MyLevelsServiceTest`, `Rider`, `Repository Layer`, `SessionTemplateServiceTest`, `RiderRepository`, `Level`, `Level Change Flow`, `Trainer Access Service`, `Rider Entity`, `Level Change Service`, `My Riders & Balance`, `AssessmentBooking`, `SessionUsageServiceTest`, `Assignment Service`, `Level Change API Tests`, `Payment Entity`, `Level Change Rules`, `Evaluation Follow-Up`, `SessionEvaluationRepository`, `Evaluation Repository Tests`, `RiderAccountStatus`, `Assignment Repository`, `Session Detail Service`, `Invite`?**
  _High betweenness centrality (0.035) - this node is a cross-community bridge._
- **What connects `CLOSED`, `NO_SHOW`, `PENDING_ASSESSMENT` to the rest of the system?**
  _115 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthUser` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
- **Why does `ApiException` connect `Error Codes & Validation` to `AuthUser`, `Session Templates & Test Asserts`, `TrainerAccountsFixture`, `clock`, `AssignmentRepository`, `ClubOnboardingService.java`, `Admin Rider Rows & Reassign`, `RiderAppQueryService.java`, `MyLevelsServiceTest`, `TrainerRepository`, `SessionTemplateServiceTest`, `RiderRepository`, `SessionTemplateRulesTest`, `AssessmentBooking`, `RepositoryIsolationTest.java`, `Assignment Service`, `API Exception Handler`, `ClubRepository`, `My Sessions & Evaluation`, `SubscriptionService`, `SessionTemplateService`, `Level Change Rules`, `PlatformClubController.java`, `Evaluation Follow-Up`, `Admin Lockout`, `Rider Level Query`, `SessionEvaluationRepository`?**
  _High betweenness centrality (0.032) - this node is a cross-community bridge._
- **Should `Spring Boot Test Wiring` be split into smaller, more focused modules?**
  _Cohesion score 0.05230720606826802 - nodes in this community are weakly interconnected._
- **Why does `SessionRepository` connect `Session Templates & Test Asserts` to `AuthUser`, `.summaries`, `Admin Member API Tests`, `Error Codes & Validation`, `Admin Oversight Journeys`, `RecordResultServiceTest`, `clock`, `AssignmentRepository`, `AdminTrainerApiTest`, `Assessment Slot Query`, `Admin Booking Change Tests`, `MyLevelsServiceTest`, `Rider`, `Repository Layer`, `Admin Overview & Bookings`, `TrainerRepository`, `Payments Scenario Tests`, `Session Evaluation API Tests`, `SessionTemplateServiceTest`, `Admin Booking API Tests`, `RiderRepository`, `Rider Reassign API Tests`, `Admin Overview API Tests`, `Level Change Flow`, `Trainer Access Service`, `Level Change Service`, `Console Finished Sessions`, `My Riders & Balance`, `AssessmentBooking`, `SessionUsageServiceTest`, `RepositoryIsolationTest.java`, `Assignment Service`, `My Week API Tests`, `My Sessions & Evaluation`, `Level Change API Tests`, `Session`, `SessionTemplateService`, `Evaluation Follow-Up`, `Admin Member Edit Tests`, `Rider Level Query`, `SessionEvaluationRepository`, `Session Generator`, `Admin Slot API Tests`, `Evaluation Repository Tests`, `Assignment Repository`, `Session Detail Service`, `Member Booking Tests`?**
  _High betweenness centrality (0.026) - this node is a cross-community bridge._
- **Should `Session Templates & Test Asserts` be split into smaller, more focused modules?**
  _Cohesion score 0.09083007388092133 - nodes in this community are weakly interconnected._