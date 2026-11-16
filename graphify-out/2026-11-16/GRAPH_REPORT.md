# Graph Report - equis-api  (2026-11-16)

## Corpus Check
- 610 files · ~201,565 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5526 nodes · 23714 edges · 260 communities (79 shown, 181 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2589 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `fbcf87be`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SheetWriter
- AssessorCount
- assertthat
- .status
- AdminTrainerApiIT
- BookingReminderApiIT
- AdminRiderApiIT
- RecordResultServiceIT
- TrainerAccountsFixture
- .accounts
- Coach
- org.junit.jupiter.api.BeforeEach
- FinishSessionApiIT
- PlanLimitService
- .of
- org.junit.jupiter.api.Test
- JwtServiceTest
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- org.springframework.stereotype.Service
- AdminOverviewService
- Session.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- .sessions
- AssessmentBookingRepositoryIT
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- AuthServiceIT
- AuthUser
- SessionTemplateRulesIT
- org.junit.jupiter.params.ParameterizedTest
- PlanAndSubscriptionIT
- TestPostgres
- AdminBookingChangeApiIT
- LevelChangeFlowIT
- TrainerAccessServiceIT
- RiderRepositoryIT
- AdminOversightFixture
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- AssessmentRulesIT
- .convert
- Release Workflow
- .issue
- RepositoryIsolationIT.java
- FreshDatabase.java
- AssignmentServiceIT
- API Exception Handler
- SeedDataIT
- Kind
- TenantContext
- RiderAppController
- AuthTokenService
- .status
- AssignOnTheDayApiIT
- TokenGenerator
- SubscriptionService.java
- ApiException
- ClubAccessService
- ClubRepositoryIT
- Booking Actions
- TenantIsolationIT
- PaymentState
- SessionTest
- ClubAdminService
- ClubAccessStateIT
- Prod Environment Check
- Plan
- SessionUsageServiceIT
- LevelChangeRulesTest.java
- PerClubSignInIT
- .register
- AdminTrainerApiIT.java
- AssessmentBookingRepository
- CI Workflow
- notblank
- Subscription
- EvaluationFollowUpQuery
- .hash
- TrainerRepositoryIT
- LevelChangeServiceIT
- AccountLinkServiceIT
- .of
- PaymentTest
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthTokenService
- AuthProperties
- TenantTransactions
- MyWeekApiIT
- PlanLimitApiIT
- .allowed
- SessionRepository
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- .balanceOn
- SessionTemplateController.java
- .admin
- AdminPaymentApiIT
- AccountLinkEmailsTest
- ClubAccessServiceTest.java
- RiderLevelQueryServiceIT
- Request
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- .of
- Docker Compose
- PaymentMethod
- AdminAccountRepositoryIT
- RiderTest
- AdminMemberEditApiIT
- uuid
- SessionTemplateRepositoryIT
- RiderPaymentStatus
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
- Changelog Epics A
- org.flywaydb.core.Flyway
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- PaymentRepositoryIT
- .set
- Test Mail Rules Docs
- Test Club Rules Docs
- AdminAssessmentSlotApiIT
- Changelog Epics B
- Image Release Docs
- Notification Sender Docs
- ProdDemoDataCleanupIT
- UsedCount
- LevelChangeApiIT
- LevelChangeRepositoryIT
- Graphify Rules Docs
- AuthTokens
- Release 2.0.1
- Maven POM
- .get
- ClubOnboardingService.java
- E17DemoIT
- EndpointCoverageGuardIT
- ReportDefinition
- SmtpNotificationSender.java
- NoticeRepositoryIT
- SessionEvaluationRepositoryIT
- RiderRepository
- Invite
- MigrationFromRelease2IT
- AssignmentRepositoryIT
- AccountLinkMailListenerIT.java
- ReportColumn
- org.springframework.context.annotation.Bean
- ReportExportIT
- TrainersReport
- org.springframework.stereotype.Component
- ReportParamsFactoryTest
- AuthProperties
- org.springframework.security.crypto.password.PasswordEncoder
- SecurityConfig.java
- Override
- XlsxReportWriterTest
- SubscriptionStatus
- Snapshot

## God Nodes (most connected - your core abstractions)
1. `ApiException` - 126 edges
2. `IntegrationTestBase` - 112 edges
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

## Communities (260 total, 181 thin omitted)

### Community 0 - "SheetWriter"
Cohesion: 0.12
Nodes (8): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, SheetWriter

### Community 2 - "assertthat"
Cohesion: 0.08
Nodes (12): MemberRepositoryIT, FixedClock, MyLevelsServiceIT, FixedClock, RiderBalanceServiceIT, FixedClock, MyRidersQueryServiceIT, FixedClock (+4 more)

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.06
Nodes (5): AdminMemberApiIT, AdminMemberCloseApiIT, BookingReminderApiIT, NoticeRow, Request

### Community 7 - "RecordResultServiceIT"
Cohesion: 0.19
Nodes (6): BookingController, RecordResultRequest, ResultRecorded, RecordResultService, FixedClock, RecordResultServiceIT

### Community 8 - "TrainerAccountsFixture"
Cohesion: 0.06
Nodes (7): TrainerDisableJourneyIT, TrainerResetJourneyIT, RiderAccountJourneyIT, Config, MutableClock, Config, TrainerAccountsFixture

### Community 9 - ".accounts"
Cohesion: 0.07
Nodes (10): OutgoingEmail, Request, TrainerAdminAccessIT, Request, TrainerInviteJourneyIT, OmarAndSara, Person, RiderAccountsScenarioIT (+2 more)

### Community 10 - "Coach"
Cohesion: 0.10
Nodes (6): ApiExceptionHandlerTest, SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Check, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.15
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.13
Nodes (9): AdminClubUsageController, ClubStatusView, ClubUsageView, Count, LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException (+1 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.07
Nodes (7): JwtConfigTest, CancelReasonTest, MemberStatusTest, EquisApiApplicationIT, InviteRepositoryIT, RefreshTokenRepositoryIT, SessionTemplateRulesOverlapsTest

### Community 19 - "AssessmentSlotQueryServiceIT"
Cohesion: 0.08
Nodes (10): AdminAssessmentSlotController, AssessmentSlotController, AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, AssessmentSlotMapper (+2 more)

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.09
Nodes (8): AssessmentSlotControllerTest, BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, MySessionsControllerTest, SessionTemplateControllerTest, ActiveClubAccess, Config

### Community 24 - "org.springframework.stereotype.Service"
Cohesion: 0.04
Nodes (34): MyRidersController, RiderEvaluations, PaymentEntry, PaymentUsage, RiderPayments, RiderPayment, RiderProgress, RiderSession (+26 more)

### Community 25 - "AdminOverviewService"
Cohesion: 0.11
Nodes (10): AdminOverviewController, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill, TrainerRiderCount, WeekFill (+2 more)

### Community 26 - "Session.java"
Cohesion: 0.08
Nodes (18): TenantGuard, SeatEvaluation, AdminAccount, RefreshToken, Member, Club, SessionEvaluation, Level (+10 more)

### Community 29 - ".sessions"
Cohesion: 0.20
Nodes (6): DateRanges, Day, LevelSessionsResponse, Seat, SessionSlot, SessionSeats

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.15
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceIT

### Community 35 - "AuthServiceIT"
Cohesion: 0.13
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "AuthUser"
Cohesion: 0.07
Nodes (22): AuthUser, AdminMemberController, MyEvaluationsController, MyLevelsController, AdminPaymentController, PlatformAuthController, MySessionsController, MyWeekController (+14 more)

### Community 43 - "TestPostgres"
Cohesion: 0.17
Nodes (4): ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 48 - "AdminOversightFixture"
Cohesion: 0.07
Nodes (10): AdminOversightAccessIT, BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture (+2 more)

### Community 50 - "RiderReassignApiIT"
Cohesion: 0.14
Nodes (3): Request, Request, RiderReassignApiIT

### Community 54 - ".convert"
Cohesion: 0.13
Nodes (3): AuthUserAuthentication, AuthUserConverter, AuthUserConverterTest

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "RepositoryIsolationIT.java"
Cohesion: 0.10
Nodes (8): Case, EndpointIsolationIT, Ids, Who, ADMIN, RIDER, TRAINER, RepositoryIsolationIT

### Community 63 - "Kind"
Cohesion: 0.40
Nodes (5): Kind, LIST, OTHER, OWN, UNKNOWN

### Community 64 - "TenantContext"
Cohesion: 0.08
Nodes (6): TenantContext, TenantIdentifierResolver, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, DefaultClubExtension

### Community 65 - "RiderAppController"
Cohesion: 0.12
Nodes (6): RiderAppController, ExpiringSoon, RiderAssessment, RiderBanners, RiderMe, RiderPayments

### Community 66 - "AuthTokenService"
Cohesion: 0.26
Nodes (8): AuthController, SuperAdminRepository, AdminLockoutService, AuthService, AuthTokenService, PasswordService, ClubAccessService, PlatformAuthService

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 69 - "TokenGenerator"
Cohesion: 0.18
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 70 - "SubscriptionService.java"
Cohesion: 0.11
Nodes (14): PlatformSubscriptionController, AddSubscriptionRequest, ClubAccessView, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionView (+6 more)

### Community 71 - "ApiException"
Cohesion: 0.07
Nodes (8): ApiException, ApiFieldError, InviteTrainerRequest, PaymentResult, RecordPaymentRequest, AccountErrors, TrainerErrors, ClubErrors

### Community 74 - "Booking Actions"
Cohesion: 0.12
Nodes (10): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+2 more)

### Community 76 - "PaymentState"
Cohesion: 0.22
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 78 - "ClubAdminService"
Cohesion: 0.06
Nodes (30): PlatformClubController, ChangeClubStatusRequest, Admin, ClubDetail, ClubPage, ClubRow, AdminStatus, ACTIVE (+22 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 83 - "SessionUsageServiceIT"
Cohesion: 0.16
Nodes (4): VoidPaymentRequest, FixedClock, SessionUsageServiceIT, ClubThreads

### Community 84 - "LevelChangeRulesTest.java"
Cohesion: 0.13
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 88 - "AssessmentBookingRepository"
Cohesion: 0.05
Nodes (23): AdminBookingController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest, ChangeBookingRequest, MemberRef, SlotRef (+15 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.13
Nodes (3): PhoneNormalizer, ClubAdminInput, CreateClubRequest

### Community 92 - "EvaluationFollowUpQuery"
Cohesion: 0.12
Nodes (16): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, DbConverter (+8 more)

### Community 101 - "Two-Club Fixture"
Cohesion: 0.14
Nodes (4): RowLevelSecurityIT, Builder, ClubData, TwoClubFixture

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 106 - "TenantTransactions"
Cohesion: 0.22
Nodes (3): Outcome, TenantTransactions, PlatformLoginRequest

### Community 110 - "SessionRepository"
Cohesion: 0.05
Nodes (24): SchoolWeek, MyWeek, WeekDay, WeekSession, LevelRepository, AssignmentRepository, LevelSeatCount, LevelSessionCount (+16 more)

### Community 113 - ".balanceOn"
Cohesion: 0.14
Nodes (3): RiderPackage, PackageBalance, RiderBalance

### Community 115 - "SessionTemplateController.java"
Cohesion: 0.22
Nodes (9): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+1 more)

### Community 116 - ".admin"
Cohesion: 0.13
Nodes (3): OverlapRef, SaveResult, Warning

### Community 119 - "ClubAccessServiceTest.java"
Cohesion: 0.20
Nodes (4): PlanView, OnExpiry, BLOCK, READ_ONLY

### Community 123 - ".book"
Cohesion: 0.20
Nodes (4): AdminNewMemberRequest, FixedClock, MemberBookingServiceIT, TestMembers

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.14
Nodes (6): ClockConfig, MailConfig, SchedulingConfig, SecurityBeans, AuthPropertiesTest, TestConfig

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 129 - "PaymentMethod"
Cohesion: 0.25
Nodes (7): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 133 - "uuid"
Cohesion: 0.04
Nodes (6): ErrorCodes, RiderEvaluation, LevelRef, ReportNotFoundException, ReportRangeTooLongException, SessionSeatAssembler

### Community 135 - "RiderPaymentStatus"
Cohesion: 0.18
Nodes (4): RiderPaymentStatus, ACTIVE, AWAITING_PAYMENT, PaymentEnumsTest

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

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

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

### Community 162 - "AuthTokens"
Cohesion: 0.08
Nodes (6): Access, AuthTokens, ClubRef, BlockedSignIn, Issued, RoleTest

### Community 206 - "ClubOnboardingService.java"
Cohesion: 0.04
Nodes (41): AdminRiderAccountController, RiderAccount, DbConverter, InvitePurpose, INVITE, RESET, DbConverter, SubjectType (+33 more)

### Community 213 - "ReportDefinition"
Cohesion: 0.12
Nodes (8): ReportController, ReportDefinition, ReportExportService, ReportParams, ReportRegistry, ReportWriter, XlsxReportWriter, ReportParamsTest

### Community 214 - "SmtpNotificationSender.java"
Cohesion: 0.16
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 215 - "NoticeRepositoryIT"
Cohesion: 0.14
Nodes (6): DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED, RefTime, NoticeRepositoryIT

### Community 217 - "RiderRepository"
Cohesion: 0.04
Nodes (40): Constraints, AdminTrainerController, AdminRiderController, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRef (+32 more)

### Community 250 - "org.springframework.context.annotation.Bean"
Cohesion: 0.09
Nodes (7): JwtConfig, FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, MailTestConfig

### Community 254 - "TrainersReport"
Cohesion: 0.24
Nodes (3): TrainerReportRow, TrainerReportRepository, TrainersReport

### Community 255 - "org.springframework.stereotype.Component"
Cohesion: 0.10
Nodes (8): BookingCancellationListener, AccountLinkEmails, Texts, AccountLinkMailListener, LogNotificationSender, NotificationSender, AssignmentCancellationListener, SessionGenerationJob

### Community 258 - "AuthProperties"
Cohesion: 0.26
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, SessionProperties

### Community 261 - "SecurityConfig.java"
Cohesion: 0.06
Nodes (12): SecurityConfig, Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, ClubAccessFilter, TenantFilter (+4 more)

### Community 263 - "Override"
Cohesion: 0.15
Nodes (4): ReportTotal, Named, ReportRegistryTest, Totalled

### Community 268 - "XlsxReportWriterTest"
Cohesion: 0.12
Nodes (4): AllTypesReport, Line, XlsxReportWriterTest, AllTypesReport

### Community 274 - "SubscriptionStatus"
Cohesion: 0.29
Nodes (6): SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED

## Knowledge Gaps
- **122 isolated node(s):** `com.equestrian:equis-api`, `TEXT`, `DATE`, `DATETIME`, `MONEY_EGP` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 513 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **181 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `ApiException` to `assertthat`, `uuid`, `Coach`, `org.junit.jupiter.api.BeforeEach`, `AssessmentSlotQueryServiceIT`, `org.springframework.stereotype.Service`, `Session.java`, `Repository Layer`, `.sessions`, `SessionTemplateServiceIT`, `AuthUser`, `SessionTemplateRulesIT`, `AssessmentRulesIT`, `AssignmentServiceIT`, `RiderAppController`, `AuthTokenService`, `SubscriptionService.java`, `SessionTest`, `ClubAdminService`, `ClubOnboardingService.java`, `.get`, `SessionUsageServiceIT`, `LevelChangeRulesTest.java`, `AssessmentBookingRepository`, `RiderRepository`, `EvaluationFollowUpQuery`, `.hash`, `TenantTransactions`, `SessionRepository`, `SessionTemplateController.java`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.013) - this node is a cross-community bridge._
- **What connects `com.equestrian:equis-api`, `TEXT`, `DATE` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SheetWriter` be split into smaller, more focused modules?**
  _Cohesion score 0.11931818181818182 - nodes in this community are weakly interconnected._
- **Why does `Rider` connect `Session.java` to `RiderTest`, `AdminTrainerApiIT`, `uuid`, `.of`, `PaymentRepositoryIT`, `org.springframework.stereotype.Service`, `Repository Layer`, `AuthTokens`, `RiderRepositoryIT`, `MyRidersQueryServiceIT`, `AssessmentRulesIT`, `RiderAppController`, `.get`, `ClubOnboardingService.java`, `SessionUsageServiceIT`, `LevelChangeRulesTest.java`, `AdminTrainerApiIT.java`, `RiderRepository`, `Invite`, `PaymentTest`, `SessionRepository`, `AssignmentRepositoryIT`, `.of`?**
  _High betweenness centrality (0.011) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.0845997973657548 - nodes in this community are weakly interconnected._
- **Why does `AuthUser` connect `AuthUser` to `SecurityConfig.java`, `RecordResultServiceIT`, `FinishSessionApiIT`, `.set`, `JwtServiceTest`, `AssessmentSlotQueryServiceIT`, `MySessionsControllerTest.java`, `org.springframework.stereotype.Service`, `RiderEvaluationHistoryApiIT`, `SessionEvaluationApiIT`, `AuthTokens`, `LevelChangeFlowIT`, `TrainerAccessServiceIT`, `MyRidersQueryServiceIT`, `AssessmentRulesIT`, `.convert`, `.issue`, `TenantContext`, `RiderAppController`, `.status`, `AssignOnTheDayApiIT`, `SubscriptionService.java`, `ApiException`, `ClubAdminService`, `AdminTrainerApiIT.java`, `RiderRepository`, `EvaluationFollowUpApiIT`, `AuthProperties`, `.admin`, `RiderLevelQueryServiceIT`, `org.springframework.context.annotation.Configuration`?**
  _High betweenness centrality (0.011) - this node is a cross-community bridge._
- **Should `AdminTrainerApiIT` be split into smaller, more focused modules?**
  _Cohesion score 0.14717741935483872 - nodes in this community are weakly interconnected._