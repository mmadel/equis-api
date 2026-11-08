# Graph Report - equis-api  (2026-11-08)

## Corpus Check
- 585 files · ~194,462 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5269 nodes · 22879 edges · 271 communities (86 shown, 185 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2534 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5c57a61f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AuthUser
- org.springframework.transaction.annotation.Transactional
- assertthat
- .status
- AdminOversightFixture
- BookingReminderApiIT
- AssessmentBooking
- TrainerAccountsFixture
- .admin
- .accounts
- Coach
- org.junit.jupiter.api.BeforeEach
- FinishSessionApiIT
- PlanLimitService
- Session
- org.junit.jupiter.api.Test
- .slot
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- RiderAppQueryService.java
- SessionRepository
- Session.java
- Repository Layer
- AdminOverviewService.java
- .status
- .convert
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- AuthServiceIT
- ApiException
- SessionTemplateRulesIT
- org.junit.jupiter.params.ParameterizedTest
- PlanAndSubscriptionIT
- TestPostgres
- AdminBookingChangeApiIT
- LevelChangeFlowIT
- TrainerAccessServiceIT
- RiderRepositoryIT
- org.springframework.test.web.servlet.ResultActions
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- AdminTrainerApiIT
- .book
- Release Workflow
- .issue
- RepositoryIsolationIT.java
- FreshDatabase.java
- AssignmentServiceIT
- API Exception Handler
- org.slf4j.Logger
- EndpointIsolationIT
- Tenant Context
- AdminOverviewService
- locale
- .status
- AssignOnTheDayApiIT
- TokenGenerator
- SubscriptionService
- uuid
- ClubAccessService
- ClubRepositoryIT
- Booking Actions
- TenantIsolationIT
- PaymentState
- SessionTest
- BillingPeriod
- ClubAccessStateIT
- Prod Environment Check
- Plan
- SessionUsageServiceIT
- LevelChangeRulesTest.java
- PerClubSignInIT
- TenantTransactions
- AdminTrainerApiIT.java
- org.springframework.stereotype.Service
- CI Workflow
- notblank
- ClubAdminService
- SessionEvaluationRepository
- .hash
- TrainerRepositoryIT
- LevelChangeServiceIT
- AccountLinkServiceIT
- .of
- SecurityConfig.java
- ClubRepository
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthTokenService
- AuthProperties
- .voidPayment
- MyWeekApiIT
- PlanLimitApiIT
- SessionTemplateRepository
- RiderEvaluationHistoryApiIT
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- .balanceOn
- .reset
- SessionTemplateController.java
- .admin
- AdminPaymentApiIT
- InvitePurpose
- RiderAccountStatus
- RiderLevelQueryServiceIT
- PaymentMethod
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- Cancel Reasons
- Docker Compose
- Default Club Extension
- AdminAccountRepositoryIT
- Trainer
- AdminMemberEditApiIT
- org.flywaydb.core.Flyway
- SessionTemplateRepositoryIT
- TrainerDisableJourneyIT
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
- Changelog Epics A
- RecordingNotificationSender
- SubjectType
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- PaymentRepositoryIT
- .set
- Test Mail Rules Docs
- Test Club Rules Docs
- AdminAssessmentSlotApiIT
- Changelog Epics B
- AuthTokens
- Image Release Docs
- Notification Sender Docs
- ProdDemoDataCleanupIT
- TenantIsolationIT.java
- LevelChangeApiIT
- LevelChangeRepositoryIT
- Graphify Rules Docs
- AuthService
- Release 2.0.1
- Maven POM
- .get
- LimitedResource
- .allowed
- E17DemoIT
- .status
- EndpointCoverageGuardIT
- TenantFilter.java
- SmtpNotificationSender.java
- NoticeRepositoryIT
- SessionEvaluationRepositoryIT
- RiderReassignService
- Invite
- MigrationFromRelease2IT
- AssignmentRepositoryIT
- AccountLinkMailListenerIT.java
- AssessmentSlotControllerTest
- MailTestConfig.java
- PaymentTest
- ClubAccessFilter.java
- BookingControllerTest
- ClubSort
- TenantIdentifierResolver
- NoticeType
- SessionTemplateRulesOverlapsTest
- JwtServiceTest
- .instant
- com.equestrian.equis.service.platform.AccessState
- .refresh
- SlotBookedCount
- ClubBlockedException
- RowLevelSecurityIT
- FixedClock

## God Nodes (most connected - your core abstractions)
1. `ApiException` - 126 edges
2. `IntegrationTestBase` - 116 edges
3. `Session` - 107 edges
4. `Rider` - 100 edges
5. `RiderRepository` - 91 edges
6. `Trainer` - 86 edges
7. `AuthUser` - 84 edges
8. `AdminOversightFixture` - 83 edges
9. `PostgresContainer` - 79 edges
10. `AssessmentBookingRepository` - 75 edges

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

## Communities (271 total, 185 thin omitted)

### Community 0 - "AuthUser"
Cohesion: 0.06
Nodes (29): AuthUser, AdminAssessmentSlotController, AdminBookingController, AssessmentSlotController, MyEvaluationsController, MyLevelsController, PlatformSubscriptionController, RiderAppController (+21 more)

### Community 1 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.04
Nodes (27): AdminTrainerController, AdminRiderAccountController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow (+19 more)

### Community 2 - "assertthat"
Cohesion: 0.07
Nodes (13): MemberRepositoryIT, FixedClock, MemberBookingServiceIT, FixedClock, MyLevelsServiceIT, FixedClock, RiderBalanceServiceIT, FixedClock (+5 more)

### Community 4 - "AdminOversightFixture"
Cohesion: 0.15
Nodes (4): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.06
Nodes (6): AdminMemberApiIT, AdminMemberCloseApiIT, BookingReminderApiIT, NoticeRow, Request, ClubThreads

### Community 6 - "AssessmentBooking"
Cohesion: 0.09
Nodes (11): Constraints, AssessmentBooking, BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING, DbConverter (+3 more)

### Community 7 - "TrainerAccountsFixture"
Cohesion: 0.13
Nodes (3): Request, TrainerAdminAccessIT, TrainerInviteJourneyIT

### Community 9 - ".accounts"
Cohesion: 0.18
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.13
Nodes (4): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.15
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.24
Nodes (4): AdminClubUsageController, ClubUsageView, Count, PlanLimitService

### Community 15 - "Session"
Cohesion: 0.07
Nodes (16): SessionDetailMapper, SeatEvaluation, SessionEvaluation, Rider, Assignment, Session, DbConverter, SessionStatus (+8 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (9): AuthPropertiesTest, TestConfig, JwtConfigTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, EquisApiApplicationIT, InviteRepositoryIT (+1 more)

### Community 17 - ".slot"
Cohesion: 0.21
Nodes (3): BookingRowDto, SlotDetailDto, AssessmentSlotMapper

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.10
Nodes (4): MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess, Config

### Community 24 - "RiderAppQueryService.java"
Cohesion: 0.06
Nodes (21): ExpiringSoon, RiderBanners, RiderMe, RiderPackage, RiderPayment, RiderPayments, RiderProgress, RiderSession (+13 more)

### Community 25 - "SessionRepository"
Cohesion: 0.08
Nodes (13): DateRanges, SchoolWeek, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyWeek (+5 more)

### Community 26 - "Session.java"
Cohesion: 0.23
Nodes (7): TenantGuard, AdminAccount, RefreshToken, Club, Payment, SessionUsage, SuperAdmin

### Community 27 - "Repository Layer"
Cohesion: 0.11
Nodes (4): TrainerRiderCount, RefreshTokenRepository, PlanRepository, SuperAdminRepository

### Community 28 - "AdminOverviewService.java"
Cohesion: 0.10
Nodes (15): TrainerRef, ActivityItem, PendingInvite, AdminRiderList, AdminRiderRow, RiderTotals, TrainerCard, AdminRiderView (+7 more)

### Community 30 - ".convert"
Cohesion: 0.13
Nodes (3): AuthUserAuthentication, AuthUserConverter, AuthUserConverterTest

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.16
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceIT

### Community 35 - "AuthServiceIT"
Cohesion: 0.12
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "ApiException"
Cohesion: 0.04
Nodes (15): ApiException, ApiFieldError, AdminBookingRow, BookingTotals, CancelBookingRequest, MemberRef, SlotRef, UpdateMemberRequest (+7 more)

### Community 37 - "SessionTemplateRulesIT"
Cohesion: 0.18
Nodes (5): OverlapRef, SessionTemplateInput, SessionTemplateRules, Warning, SessionTemplateRulesIT

### Community 38 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.12
Nodes (3): PhoneNormalizerTest, ClubAccessFilterTest, LevelRepositoryIT

### Community 43 - "TestPostgres"
Cohesion: 0.22
Nodes (3): ApplicantIntakeMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 48 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.09
Nodes (7): Request, AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, Request, AdminOversightFixture, Coach

### Community 50 - "RiderReassignApiIT"
Cohesion: 0.08
Nodes (4): AdminRiderApiIT, Request, Request, RiderReassignApiIT

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.16
Nodes (6): BalanceDto, MyRiderDto, RiderLevelDto, UpcomingSeat, FixedClock, MyRidersQueryServiceIT

### Community 54 - ".book"
Cohesion: 0.06
Nodes (7): RecordResultRequest, ResultRecorded, AssessmentBookingTest, AssessmentBookingRepositoryIT, AssessmentRulesIT, FixedClock, RecordResultServiceIT

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 62 - "org.slf4j.Logger"
Cohesion: 0.11
Nodes (5): AccountLinkMailListener, LogNotificationSender, NotificationSender, AssignmentCancellationListener, SessionGenerationJob

### Community 63 - "EndpointIsolationIT"
Cohesion: 0.16
Nodes (13): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+5 more)

### Community 64 - "Tenant Context"
Cohesion: 0.15
Nodes (4): TenantContext, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 65 - "AdminOverviewService"
Cohesion: 0.15
Nodes (7): AdminOverviewController, OverviewResponse, TodaySession, TrackFill, WeekFill, SeatHolder, AdminOverviewService

### Community 66 - "locale"
Cohesion: 0.15
Nodes (4): RiderPaymentStatus, ACTIVE, AWAITING_PAYMENT, DayOfWeekConverter

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 69 - "TokenGenerator"
Cohesion: 0.18
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 70 - "SubscriptionService"
Cohesion: 0.09
Nodes (19): AddSubscriptionRequest, ClubAccessView, PlanView, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionStatus (+11 more)

### Community 71 - "uuid"
Cohesion: 0.05
Nodes (3): ErrorCodes, RiderEvaluation, SessionSeatAssembler

### Community 74 - "Booking Actions"
Cohesion: 0.12
Nodes (10): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+2 more)

### Community 76 - "PaymentState"
Cohesion: 0.22
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 78 - "BillingPeriod"
Cohesion: 0.11
Nodes (20): ChangeClubStatusRequest, Admin, ClubDetail, ClubPage, ClubRow, AdminStatus, ACTIVE, INVITED (+12 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plan"
Cohesion: 0.15
Nodes (5): OnExpiry, BLOCK, READ_ONLY, Plan, Subscription

### Community 84 - "LevelChangeRulesTest.java"
Cohesion: 0.13
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 86 - "TenantTransactions"
Cohesion: 0.16
Nodes (5): Outcome, TenantTransactions, ClubAdminInput, CreateClubRequest, ClubOnboardingService

### Community 88 - "org.springframework.stereotype.Service"
Cohesion: 0.08
Nodes (32): AdminMemberController, BookingController, AssessmentBookingRepository, MemberRepository, LevelRepository, NoticeRepository, LevelChangeRepository, RiderRepository (+24 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 91 - "ClubAdminService"
Cohesion: 0.14
Nodes (6): PlatformClubController, ClubAdminService, Entry, ClubFilter, ClubUsageService, Usage

### Community 92 - "SessionEvaluationRepository"
Cohesion: 0.07
Nodes (20): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, PendingEvaluations (+12 more)

### Community 93 - ".hash"
Cohesion: 0.22
Nodes (3): PasswordService, CountingEncoder, PasswordServiceTest

### Community 99 - "ClubRepository"
Cohesion: 0.11
Nodes (4): ClubRepository, SubscriptionRepository, ClubAccessService, ClubErrors

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "AuthTokenService"
Cohesion: 0.19
Nodes (3): AuthTokenService, Issued, AuthTokenServiceIT

### Community 104 - "AuthProperties"
Cohesion: 0.10
Nodes (9): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, JwtConfig (+1 more)

### Community 106 - ".voidPayment"
Cohesion: 0.13
Nodes (8): AdminPaymentController, PaymentEntry, PaymentResult, PaymentUsage, RecordPaymentRequest, RiderPayments, VoidPaymentRequest, UsageLine

### Community 109 - "SessionTemplateRepository"
Cohesion: 0.16
Nodes (6): SessionProperties, SessionTemplateRepository, AssessmentSeatCounts, GenerationResult, SessionGenerator, TemplateBookedSeats

### Community 115 - "SessionTemplateController.java"
Cohesion: 0.11
Nodes (11): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+3 more)

### Community 118 - "InvitePurpose"
Cohesion: 0.12
Nodes (8): DbConverter, InvitePurpose, INVITE, RESET, AccountLinkCreatedEvent, AccountLinkEmails, Texts, AccountLinkEmailsTest

### Community 119 - "RiderAccountStatus"
Cohesion: 0.31
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 121 - "PaymentMethod"
Cohesion: 0.25
Nodes (7): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 127 - "Cancel Reasons"
Cohesion: 0.21
Nodes (9): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+1 more)

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "Trainer"
Cohesion: 0.10
Nodes (5): Trainer, Level, LevelChange, LevelChangeTest, RiderTest

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 140 - "RecordingNotificationSender"
Cohesion: 0.18
Nodes (3): OutgoingEmail, TrainerResetJourneyIT, RecordingNotificationSender

### Community 141 - "SubjectType"
Cohesion: 0.12
Nodes (12): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, DbConverter, SubjectType, ADMIN (+4 more)

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

### Community 150 - "AuthTokens"
Cohesion: 0.13
Nodes (7): PlatformAuthController, Access, AuthTokens, ClubRef, PlatformLoginRequest, AdminLockoutService, PlatformAuthService

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

### Community 155 - "TenantIsolationIT.java"
Cohesion: 0.07
Nodes (3): ClockConfig, SecurityBeans, Config

### Community 162 - "AuthService"
Cohesion: 0.20
Nodes (3): AuthController, AuthService, BlockedSignIn

### Community 206 - "LimitedResource"
Cohesion: 0.38
Nodes (4): LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException

### Community 214 - "SmtpNotificationSender.java"
Cohesion: 0.15
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 217 - "RiderReassignService"
Cohesion: 0.22
Nodes (4): AdminRiderController, ReassignResult, ReassignRidersRequest, RiderReassignService

### Community 249 - "AssessmentSlotControllerTest"
Cohesion: 0.24
Nodes (3): AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotControllerTest

### Community 252 - "ClubAccessFilter.java"
Cohesion: 0.31
Nodes (3): ClubAccessFilter, ClubStatusController, SubscriptionExpiredException

### Community 254 - "ClubSort"
Cohesion: 0.22
Nodes (4): ClubSort, CODE, ENDS_AT, NAME

### Community 257 - "NoticeType"
Cohesion: 0.39
Nodes (4): DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED

## Knowledge Gaps
- **117 isolated node(s):** `ACTIVE`, `DISABLED`, `INVITED`, `CLOSED`, `NO_SHOW` (+112 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 490 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **185 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `ApiException` to `AuthUser`, `org.springframework.transaction.annotation.Transactional`, `assertthat`, `AssessmentBooking`, `org.junit.jupiter.api.BeforeEach`, `Session`, `.slot`, `AuthTokens`, `RiderAppQueryService.java`, `SessionRepository`, `AdminOverviewService.java`, `SessionTemplateServiceIT`, `SessionTemplateRulesIT`, `PlanAndSubscriptionIT`, `.book`, `AssignmentServiceIT`, `API Exception Handler`, `SubscriptionService`, `uuid`, `.get`, `.status`, `LevelChangeRulesTest.java`, `TenantTransactions`, `org.springframework.stereotype.Service`, `RiderReassignService`, `ClubAdminService`, `SessionEvaluationRepository`, `.hash`, `AccountLinkServiceIT`, `ClubRepository`, `.voidPayment`, `SessionTemplateController.java`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.022) - this node is a cross-community bridge._
- **What connects `ACTIVE`, `DISABLED`, `INVITED` to the rest of the system?**
  _117 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthUser` be split into smaller, more focused modules?**
  _Cohesion score 0.057692307692307696 - nodes in this community are weakly interconnected._
- **Why does `Notice` connect `Invite` to `NoticeType`, `Trainer`, `AssessmentBooking`, `NoticeRepositoryIT`, `org.springframework.stereotype.Service`, `Session.java`, `Repository Layer`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `org.springframework.transaction.annotation.Transactional` be split into smaller, more focused modules?**
  _Cohesion score 0.041113841113841115 - nodes in this community are weakly interconnected._
- **Why does `Rider` connect `Session` to `org.springframework.transaction.annotation.Transactional`, `Trainer`, `AssessmentBooking`, `TrainerDisableJourneyIT`, `PaymentRepositoryIT`, `RiderAppQueryService.java`, `Session.java`, `Repository Layer`, `AuthService`, `ApiException`, `RiderRepositoryIT`, `MyRidersQueryServiceIT`, `AdminTrainerApiIT`, `.book`, `uuid`, `SessionUsageServiceIT`, `LevelChangeRulesTest.java`, `AdminTrainerApiIT.java`, `org.springframework.stereotype.Service`, `Invite`, `AssignmentRepositoryIT`, `SessionEvaluationRepository`, `RiderAccountStatus`, `PaymentTest`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.07160445188614202 - nodes in this community are weakly interconnected._