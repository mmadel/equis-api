# Graph Report - equis-api  (2026-11-08)

## Corpus Check
- 578 files · ~188,945 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: .properties 6, (none) 4, .imports 2)

## Summary
- 5345 nodes · 23711 edges · 216 communities (83 shown, 133 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2555 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `34af5fcd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AuthUser
- TrainerRepository
- SubscriptionService.java
- Admin Oversight Journeys
- Admin Member API Tests
- Error Codes & Validation
- TrainerAccountsFixture
- RecordResultServiceTest
- RiderAccountsScenarioTest
- Scenario Journey Tests
- org.springframework.jdbc.core.JdbcTemplate
- AdminTrainerApiTest
- PlatformClubApiTest.java
- PlatformClubApiTest
- Exception Handler & Rules Tests
- ClubOnboardingService
- PlatformSubscriptionApiTest
- Assessment Slot Query
- MySessionsControllerTest.java
- Admin Rider Rows & Reassign
- PlatformSignInJourneyTest
- Admin Booking Change Tests
- RiderAppQueryService.java
- MyLevelsServiceTest
- instant
- org.springframework.transaction.annotation.Transactional
- Admin Overview & Bookings
- .admin
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
- .booking
- Release Workflow
- SessionUsageServiceTest
- TestPostgres
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
- FinishSessionApiTest
- PlanLimitService
- InvitePurpose
- Evaluation Follow-Up Tests
- Club Repository Tests
- Booking Actions
- Tenant Isolation Tests
- PaymentState
- .of
- ClubAdminService.java
- Rider Eval History Tests
- Payment Void API Tests
- Prod Environment Check
- Plans & Expiry Policy
- Payment Entity
- SessionTemplateService
- Per-Club Sign-In Tests
- Admin Payment API Tests
- MailTestConfig.java
- Rider
- CI Workflow
- notblank
- ClubAdminService
- Evaluation Follow-Up
- Admin Lockout
- Trainer Repository
- .convert
- Account Link Service
- Admin Member Edit Tests
- SecurityConfig.java
- RiderLevelQueryServiceTest
- Rider Balance API Tests
- Two-Club Fixture
- Member Status
- Auth Tokens
- JWT Config
- Repository Isolation
- RefreshTokenRepositoryTest
- AuthProperties
- Session
- .register
- Notices
- Session Generator
- Admin Slot API Tests
- TwoClubFixture
- SmtpNotificationSenderTest.java
- Template Mapper
- SessionTemplateControllerTest
- Evaluation Repository Tests
- SmtpNotificationSender.java
- RiderAccountStatus
- Assignment Repository
- PaymentMethod
- java.time.YearMonth
- Member Booking Tests
- org.springframework.boot.context.properties.EnableConfigurationProperties
- AdminNewMemberRequest.java
- AdminTrainerActionsApiTest
- Cancel Reasons
- Docker Compose
- Default Club Extension
- Admin Account Repo
- TemplateBookedSeats
- PlanLimitApiTest
- RiderBalanceServiceTest
- SessionTemplate
- TenantAutoConfiguration.java
- AssessmentRulesTest
- Changelog Epics A
- TrainerDisableJourneyTest
- SubjectType
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- AssessmentSlotControllerTest
- RecordingNotificationSender
- Test Mail Rules Docs
- Test Club Rules Docs
- .reset
- Changelog Epics B
- AuthService.java
- AssessmentBookingRepositoryTest
- Image Release Docs
- Notification Sender Docs
- AccountLinkEmails.java
- LevelChange
- .stateOf
- SessionTest
- .status
- Graphify Rules Docs
- ClubSort
- Release 2.0.1
- Maven POM
- TenantTransactions
- .of
- EndpointCoverageGuardTest
- AdminStatus
- EquisApiApplicationTests.java

## God Nodes (most connected - your core abstractions)
1. `TestPostgres` - 198 edges
2. `ApiException` - 120 edges
3. `Session` - 106 edges
4. `TrainerRepository` - 103 edges
5. `Rider` - 100 edges
6. `AuthUser` - 84 edges
7. `AdminOversightFixture` - 83 edges
8. `RiderRepository` - 82 edges
9. `TrainerAccountsFixture` - 77 edges
10. `AdminBookingChangeApiTest` - 71 edges

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

## Communities (216 total, 133 thin omitted)

### Community 0 - "AuthUser"
Cohesion: 0.06
Nodes (27): AuthUser, AdminTrainerController, AdminAssessmentSlotController, AssessmentSlotController, MyEvaluationsController, MyLevelsController, AdminPaymentController, PlatformSubscriptionController (+19 more)

### Community 2 - "TrainerRepository"
Cohesion: 0.08
Nodes (9): TrainerRepository, TrainerAccessService, HibernateClock, FixedClock, FixedClock, FixedClock, FixedClock, Config (+1 more)

### Community 3 - "SubscriptionService.java"
Cohesion: 0.09
Nodes (14): AddSubscriptionRequest, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionStatus, CURRENT, ENDED (+6 more)

### Community 4 - "Admin Oversight Journeys"
Cohesion: 0.09
Nodes (6): AdminOversightAccessTest, ReminderJourneyTest, RiderReassignJourneyTest, Request, AdminOversightFixture, Coach

### Community 5 - "Admin Member API Tests"
Cohesion: 0.06
Nodes (6): AdminMemberApiTest, AdminMemberCloseApiTest, BookingReminderApiTest, NoticeRow, Request, ClubThreads

### Community 6 - "Error Codes & Validation"
Cohesion: 0.09
Nodes (8): ApiException, ApiFieldError, ErrorCodes, DateRanges, Trainer, Levels, AccountErrors, TrainerErrors

### Community 7 - "TrainerAccountsFixture"
Cohesion: 0.08
Nodes (6): Request, TrainerAdminAccessTest, Request, TrainerInviteJourneyTest, TrainerResetJourneyTest, TrainerAccountsFixture

### Community 8 - "RecordResultServiceTest"
Cohesion: 0.28
Nodes (3): RecordResultRequest, ResultRecorded, RecordResultServiceTest

### Community 9 - "RiderAccountsScenarioTest"
Cohesion: 0.16
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioTest, RiderLogin

### Community 10 - "Scenario Journey Tests"
Cohesion: 0.11
Nodes (5): SessionEvaluationsScenarioTest, SharedSession, SameDaySessionsScenarioTest, Check, SameDayJourneys

### Community 16 - "Exception Handler & Rules Tests"
Cohesion: 0.04
Nodes (9): AuthPropertiesTest, JwtConfigTest, SecurityConfigTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, SeedDataTest, RiderRepositoryTest (+1 more)

### Community 17 - "ClubOnboardingService"
Cohesion: 0.14
Nodes (5): PlatformClubController, ChangeClubStatusRequest, ClubDetail, ClubErrors, ClubOnboardingService

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.10
Nodes (5): SecurityConfig, BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess

### Community 21 - "Admin Rider Rows & Reassign"
Cohesion: 0.07
Nodes (20): TrainerRef, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard, TrainerStatus (+12 more)

### Community 24 - "RiderAppQueryService.java"
Cohesion: 0.04
Nodes (34): RiderAppController, PaymentEntry, PaymentResult, PaymentUsage, RiderPayments, VoidPaymentRequest, ExpiringSoon, RiderAssessment (+26 more)

### Community 26 - "instant"
Cohesion: 0.17
Nodes (6): TenantGuard, AdminAccount, RefreshToken, Club, Notice, SessionUsage

### Community 27 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.04
Nodes (13): LevelCount, PendingLink, RiderCounts, TrainerTotals, Invite, InviteRepository, AssessorCount, SlotBookedCount (+5 more)

### Community 28 - "Admin Overview & Bookings"
Cohesion: 0.07
Nodes (14): SchoolWeek, AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill (+6 more)

### Community 30 - "Auth User Conversion"
Cohesion: 0.12
Nodes (9): AuthUserAuthentication, AuthUserConverter, Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, SuperAdmin (+1 more)

### Community 32 - "Session Evaluation API Tests"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiTest, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceTest"
Cohesion: 0.16
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceTest

### Community 35 - "Auth Service & Invites"
Cohesion: 0.11
Nodes (6): AuthController, InviteAccepted, InviteAcceptRequest, LoginRequest, RefreshRequest, AuthServiceTest

### Community 36 - "RiderRepository"
Cohesion: 0.03
Nodes (48): Constraints, AdminBookingController, AdminMemberController, BookingController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest (+40 more)

### Community 37 - "SessionTemplateRulesTest"
Cohesion: 0.22
Nodes (4): OverlapRef, SessionTemplateInput, Warning, SessionTemplateRulesTest

### Community 38 - "Phone Normalizer & Params"
Cohesion: 0.09
Nodes (4): PhoneNormalizerTest, ClubAccessFilterTest, LevelRepositoryTest, BookingActionsTest

### Community 39 - "Rider Reassign API Tests"
Cohesion: 0.08
Nodes (4): AdminRiderApiTest, Request, Request, RiderReassignApiTest

### Community 41 - "Intake Scenario Tests"
Cohesion: 0.13
Nodes (4): BookingOversightJourneyTest, NoShowRebooked, IntakeScenarioTest, Person

### Community 43 - "Flyway Migration Tests"
Cohesion: 0.16
Nodes (4): ProdDemoDataCleanupTest, ApplicantIntakeMigrationTest, RiderAccountMigrationTest, SessionFinishedMigrationTest

### Community 51 - "My Riders & Balance"
Cohesion: 0.19
Nodes (5): BalanceDto, MyRiderDto, UpcomingSeat, FixedClock, MyRidersQueryServiceTest

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "TestPostgres"
Cohesion: 0.07
Nodes (3): ClockConfig, RowLevelSecurityTest, TestPostgres

### Community 63 - "Endpoint Isolation Tests"
Cohesion: 0.12
Nodes (13): Case, EndpointIsolationTest, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+5 more)

### Community 64 - "Tenant Context"
Cohesion: 0.09
Nodes (5): TenantContext, TenantIdentifierResolver, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 65 - "Assign On The Day Tests"
Cohesion: 0.11
Nodes (4): State, CANCELLED, FINISHED, SCHEDULED

### Community 66 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.11
Nodes (9): DbConverter, DbConverter, DbConverter, DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED, DayOfWeekConverter (+1 more)

### Community 69 - "FinishSessionApiTest"
Cohesion: 0.14
Nodes (5): FinishSessionApiTest, Request, State, CANCELLED, SCHEDULED

### Community 70 - "PlanLimitService"
Cohesion: 0.11
Nodes (10): AdminClubUsageController, ClubStatusView, ClubUsageView, Count, BlockedSignIn, LimitedResource, RIDERS, TRAINERS (+2 more)

### Community 71 - "InvitePurpose"
Cohesion: 0.15
Nodes (6): DbConverter, InvitePurpose, INVITE, RESET, AccountLinkCreatedEvent, AccountLinkEmailsTest

### Community 74 - "Booking Actions"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 76 - "PaymentState"
Cohesion: 0.22
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 77 - ".of"
Cohesion: 0.12
Nodes (3): SessionRepositoryTest, FixedClock, SessionDetailServiceTest

### Community 78 - "ClubAdminService.java"
Cohesion: 0.10
Nodes (20): ClubAccessView, ClubPage, ClubRow, ClubStatus, ACTIVE, SUSPENDED, BillingPeriod, ANNUAL (+12 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plans & Expiry Policy"
Cohesion: 0.11
Nodes (7): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository, SubscriptionService

### Community 84 - "SessionTemplateService"
Cohesion: 0.30
Nodes (6): SessionTemplateController, SessionTemplateRepository, BookedSeats, SessionGenerator, SessionTemplateRules, SessionTemplateService

### Community 88 - "Rider"
Cohesion: 0.05
Nodes (14): RiderEvaluation, RiderEvaluations, Direction, DOWN, UP, LevelChangeRequest, RiderAccount, Rider (+6 more)

### Community 89 - "CI Workflow"
Cohesion: 0.09
Nodes (21): env, runs-on, steps, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (+13 more)

### Community 91 - "ClubAdminService"
Cohesion: 0.19
Nodes (5): AdminAccountRepository, ClubAdminService, Entry, ClubUsageService, Usage

### Community 92 - "Evaluation Follow-Up"
Cohesion: 0.22
Nodes (10): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, EvaluationFollowUpQuery (+2 more)

### Community 93 - "Admin Lockout"
Cohesion: 0.09
Nodes (8): PlatformAuthController, PlatformLoginRequest, LockableAccount, AdminLockoutService, PasswordService, PlatformAuthService, AdminLockoutServiceTest, PasswordServiceTest

### Community 96 - "Account Link Service"
Cohesion: 0.12
Nodes (4): GeneratedToken, TokenGenerator, AccountLinkServiceTest, TokenGeneratorTest

### Community 98 - "SecurityConfig.java"
Cohesion: 0.09
Nodes (4): ClubAccessFilter, TenantFilter, ClubBlockedException, SubscriptionExpiredException

### Community 102 - "Member Status"
Cohesion: 0.13
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "Auth Tokens"
Cohesion: 0.13
Nodes (5): Access, AuthTokens, ClubRef, Issued, AuthTokenServiceTest

### Community 107 - "AuthProperties"
Cohesion: 0.26
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, SessionProperties

### Community 108 - "Session"
Cohesion: 0.04
Nodes (39): MySessionsController, SessionDetailMapper, EvaluationRequest, SeatEvaluation, LevelRef, Day, LevelSessionsResponse, Seat (+31 more)

### Community 113 - "TwoClubFixture"
Cohesion: 0.11
Nodes (5): CountingEncoder, MutableClock, TestSubscriptions, Config, TwoClubFixture

### Community 114 - "SmtpNotificationSenderTest.java"
Cohesion: 0.09
Nodes (4): AccessToken, MailConfigTest, JwtServiceTest, SmtpNotificationSenderTest

### Community 115 - "Template Mapper"
Cohesion: 0.18
Nodes (8): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse, SessionTemplateUpdateRequest

### Community 118 - "SmtpNotificationSender.java"
Cohesion: 0.16
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 119 - "RiderAccountStatus"
Cohesion: 0.31
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 121 - "PaymentMethod"
Cohesion: 0.27
Nodes (7): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 123 - "Member Booking Tests"
Cohesion: 0.20
Nodes (4): AdminNewMemberRequest, FixedClock, MemberBookingServiceTest, TestMembers

### Community 124 - "org.springframework.boot.context.properties.EnableConfigurationProperties"
Cohesion: 0.24
Nodes (4): MailConfig, SchedulingConfig, SecurityBeans, TestConfig

### Community 127 - "Cancel Reasons"
Cohesion: 0.20
Nodes (8): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "TemplateBookedSeats"
Cohesion: 0.24
Nodes (3): BookingSeatCounts, AssessmentSeatCounts, TemplateBookedSeats

### Community 136 - "TenantAutoConfiguration.java"
Cohesion: 0.12
Nodes (3): TenantAutoConfiguration, BeanPostProcessor, TenantAwareDataSource

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 141 - "SubjectType"
Cohesion: 0.17
Nodes (7): DbConverter, SubjectType, ADMIN, RIDER, SUPER_ADMIN, TRAINER, RoleTest

### Community 142 - "Base URLs & Accounts Docs"
Cohesion: 0.29
Nodes (8): APP_RIDER_BASE_URL, E15 Rider accounts and data, E1 Auth & accounts, E6 Trainer accounts, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (smtp|log), Production environment variables

### Community 143 - "Ops Scripts & Plans Docs"
Cohesion: 0.25
Nodes (8): V5__seed_admin.sql, create-super-admin.sql, on_expiry policy (BLOCK | READ_ONLY), Ops scripts (run by hand), Plans TRIAL / BASIC / PRO / MAX, POST /api/platform/auth/login, upsert-plan.sql, APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH

### Community 144 - "AssessmentSlotControllerTest"
Cohesion: 0.22
Nodes (5): AssessmentSlotDayDto, AssessmentSlotDto, BookingRowDto, SlotDetailDto, AssessmentSlotControllerTest

### Community 145 - "RecordingNotificationSender"
Cohesion: 0.17
Nodes (3): InviteTrainerRequest, OutgoingEmail, RecordingNotificationSender

### Community 147 - "Test Club Rules Docs"
Cohesion: 0.33
Nodes (6): ClubThreads, DefaultClubExtension, TenantContext.runAs, APP_DEFAULT_CLUB_CODE / APP_DEFAULT_CLUB_NAME, equis_app database role (SET ROLE), Row-level security per club (V23, V24)

### Community 149 - "Changelog Epics B"
Cohesion: 0.47
Nodes (6): E11 Admin intake, E12 Payments and balance, E13 Same-day sessions, E14 Session evaluations, E4 Assessment, Release 2.0.0

### Community 150 - "AuthService.java"
Cohesion: 0.12
Nodes (4): ClubStatusController, AuthService, AuthTokenService, ClubAccessService

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

### Community 154 - "AccountLinkEmails.java"
Cohesion: 0.17
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 155 - "LevelChange"
Cohesion: 0.21
Nodes (3): LevelHistoryEntry, RiderLevelDto, LevelChange

### Community 162 - "ClubSort"
Cohesion: 0.22
Nodes (4): ClubSort, CODE, ENDS_AT, NAME

### Community 208 - "AdminStatus"
Cohesion: 0.40
Nodes (4): Admin, AdminStatus, ACTIVE, INVITED

## Knowledge Gaps
- **117 isolated node(s):** `com.equestrian:equis-api`, `TRAINERS`, `RIDERS`, `SCHEDULED`, `CANCELLED` (+112 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 512 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **133 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `Error Codes & Validation` to `TrainerRepository`, `SubscriptionService.java`, `SessionTemplate`, `AssessmentRulesTest`, `ClubOnboardingService`, `Admin Rider Rows & Reassign`, `RiderAppQueryService.java`, `org.springframework.transaction.annotation.Transactional`, `.status`, `RiderRepository`, `SessionTemplateRulesTest`, `Assignment Service`, `API Exception Handler`, `TenantTransactions`, `ClubAdminService.java`, `Plans & Expiry Policy`, `SessionTemplateService`, `Rider`, `Evaluation Follow-Up`, `Admin Lockout`, `Session`?**
  _High betweenness centrality (0.040) - this node is a cross-community bridge._
- **What connects `com.equestrian:equis-api`, `TRAINERS`, `RIDERS` to the rest of the system?**
  _117 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthUser` be split into smaller, more focused modules?**
  _Cohesion score 0.06334735857877513 - nodes in this community are weakly interconnected._
- **Why does `TestPostgres` connect `TestPostgres` to `Spring Boot Test Wiring`, `TrainerRepository`, `Admin Oversight Journeys`, `Admin Member API Tests`, `TrainerAccountsFixture`, `RecordResultServiceTest`, `RiderAccountsScenarioTest`, `Scenario Journey Tests`, `org.springframework.jdbc.core.JdbcTemplate`, `AdminTrainerApiTest`, `PlatformClubApiTest.java`, `PlatformClubApiTest`, `uuid`, `Exception Handler & Rules Tests`, `PlatformSubscriptionApiTest`, `Assessment Slot Query`, `MySessionsControllerTest.java`, `PlatformSignInJourneyTest`, `Admin Booking Change Tests`, `MyLevelsServiceTest`, `.admin`, `Payments Scenario Tests`, `Session Evaluation API Tests`, `SessionTemplateServiceTest`, `Admin Booking API Tests`, `Auth Service & Invites`, `SessionTemplateRulesTest`, `Phone Normalizer & Params`, `Rider Reassign API Tests`, `Plan & Subscription Model`, `Intake Scenario Tests`, `Admin Overview API Tests`, `Flyway Migration Tests`, `LevelChangeRepositoryTest`, `Level Change Flow`, `Trainer Access Service`, `Level Change Service`, `Console Finished Sessions`, `Invite Repository Tests`, `My Riders & Balance`, `Rider App API & JWT`, `ClubAccessStateTest`, `SessionUsageServiceTest`, `Assignment Service`, `My Week API Tests`, `Endpoint Isolation Tests`, `Level Change API Tests`, `FinishSessionApiTest`, `Evaluation Follow-Up Tests`, `Club Repository Tests`, `Tenant Isolation Tests`, `.of`, `Rider Eval History Tests`, `Payment Void API Tests`, `Per-Club Sign-In Tests`, `Admin Payment API Tests`, `Admin Lockout`, `Trainer Repository`, `Account Link Service`, `Admin Member Edit Tests`, `RiderLevelQueryServiceTest`, `Rider Balance API Tests`, `Auth Tokens`, `Repository Isolation`, `RefreshTokenRepositoryTest`, `.register`, `Notices`, `Session Generator`, `Admin Slot API Tests`, `Evaluation Repository Tests`, `Assignment Repository`, `java.time.YearMonth`, `Member Booking Tests`, `AdminTrainerActionsApiTest`, `Admin Account Repo`, `PlanLimitApiTest`, `RiderBalanceServiceTest`, `SessionTemplate`, `AssessmentRulesTest`, `TrainerDisableJourneyTest`, `AssessmentBookingRepositoryTest`, `EndpointCoverageGuardTest`, `EquisApiApplicationTests.java`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Should `Spring Boot Test Wiring` be split into smaller, more focused modules?**
  _Cohesion score 0.07136404697380307 - nodes in this community are weakly interconnected._
- **Why does `TrainerRepository` connect `TrainerRepository` to `Error Codes & Validation`, `RecordResultServiceTest`, `AssessmentRulesTest`, `org.springframework.jdbc.core.JdbcTemplate`, `AdminTrainerApiTest`, `PlatformClubApiTest.java`, `uuid`, `Exception Handler & Rules Tests`, `Assessment Slot Query`, `MySessionsControllerTest.java`, `Admin Rider Rows & Reassign`, `AssessmentBookingRepositoryTest`, `MyLevelsServiceTest`, `org.springframework.transaction.annotation.Transactional`, `Admin Overview & Bookings`, `Auth User Conversion`, `SessionTemplateServiceTest`, `Auth Service & Invites`, `RiderRepository`, `LevelChangeRepositoryTest`, `Level Change Flow`, `Trainer Access Service`, `Level Change Service`, `Invite Repository Tests`, `My Riders & Balance`, `Assignment Service`, `Level Change API Tests`, `PlanLimitService`, `.of`, `Rider`, `Trainer Repository`, `.convert`, `Account Link Service`, `RiderLevelQueryServiceTest`, `Session`, `Notices`, `Evaluation Repository Tests`, `Assignment Repository`, `Member Booking Tests`, `AdminTrainerActionsApiTest`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **Should `TrainerRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.07780772686433064 - nodes in this community are weakly interconnected._