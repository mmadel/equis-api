# Graph Report - equis-api  (2026-11-16)

## Corpus Check
- 608 files · ~198,676 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5430 nodes · 23395 edges · 280 communities (96 shown, 184 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2569 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b08b5f4a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.springframework.web.bind.annotation.GetMapping
- org.springframework.web.bind.annotation.PostMapping
- assertthat
- .status
- AdminOversightFixture
- BookingReminderApiIT
- BookingStatus
- TrainerAccountsFixture
- .admin
- .accounts
- Coach
- org.junit.jupiter.api.BeforeEach
- FinishSessionApiIT
- PlanLimitService
- Session
- org.junit.jupiter.api.Test
- AssessmentSlotControllerTest
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- RiderAppQueryService
- .summaries
- Session.java
- Repository Layer
- AdminOverviewService.java
- .sessions
- AssessmentBooking
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
- LevelRiderCount
- AdminOversightFixture
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- AdminTrainerApiIT
- AuthUser
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
- jakarta.persistence.AttributeConverter
- .status
- AssignOnTheDayApiIT
- TokenGenerator
- Subscription
- list
- ClubAccessService
- ClubRepositoryIT
- Booking Actions
- TenantIsolationIT
- PaymentState
- SessionTest
- BillingPeriod
- ClubAccessStateIT
- Prod Environment Check
- OnExpiry
- SessionUsageServiceIT
- LevelChangeRulesTest.java
- PerClubSignInIT
- Member
- AdminTrainerApiIT.java
- RiderRepository
- CI Workflow
- notblank
- PlatformClubController.java
- EvaluationFollowUpQuery
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
- TemplateBookedSeats
- AssignmentRepository
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- .balanceOn
- .reset
- SessionTemplateController.java
- .admin
- AdminPaymentApiIT
- AccountLinkEmailsTest
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
- uuid
- SessionTemplateRepositoryIT
- TrainerDisableJourneyIT
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
- Changelog Epics A
- InviteRepositoryIT
- RoleTest.java
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- PaymentRepositoryIT
- .set
- Test Mail Rules Docs
- Test Club Rules Docs
- AdminAssessmentSlotApiIT
- Changelog Epics B
- TenantTransactions
- Image Release Docs
- Notification Sender Docs
- ProdDemoDataCleanupIT
- LevelChangeApiIT
- LevelChangeRepositoryIT
- Graphify Rules Docs
- AuthService
- Release 2.0.1
- Maven POM
- SessionRepository
- ClubOnboardingService.java
- .allowed
- E17DemoIT
- .status
- EndpointCoverageGuardIT
- pathvariable
- SmtpNotificationSender.java
- NoticeRepositoryIT
- SessionEvaluationRepositoryIT
- AdminRiderService
- Invite
- MigrationFromRelease2IT
- AssignmentRepositoryIT
- AccountLinkMailListenerIT.java
- ReportColumn
- MailTestConfig.java
- PaymentTest
- SessionTemplateService
- ReportExportIT
- TrainersReport
- AccountLinkEmails
- TenantIdentifierResolver
- ReportParamsFactoryTest
- AuthProperties
- com.fasterxml.jackson.annotation.JsonValue
- TwoClubFixture
- ReportDefinition
- Named
- SlotBookedCount
- XlsxReportWriter.java
- SessionStatus
- ReportParams
- ClubUsageService
- org.springframework.security.crypto.password.PasswordEncoder
- SubscriptionStatus
- Snapshot
- XlsxReportWriterTest
- Notice
- SlotLabels.java
- Plan

## God Nodes (most connected - your core abstractions)
1. `ApiException` - 126 edges
2. `IntegrationTestBase` - 114 edges
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

## Communities (280 total, 184 thin omitted)

### Community 0 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.06
Nodes (21): BookingController, ClubStatusController, MyEvaluationsController, MyLevelsController, AdminOverviewController, AdminRiderAccountController, MyWeekController, PendingEvaluations (+13 more)

### Community 1 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.06
Nodes (10): AdminTrainerController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals (+2 more)

### Community 2 - "assertthat"
Cohesion: 0.09
Nodes (8): FixedClock, RecordResultServiceIT, FixedClock, MyLevelsServiceIT, FixedClock, SessionDetailServiceIT, Config, PostgresContainer

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.06
Nodes (6): AdminMemberApiIT, AdminMemberCloseApiIT, BookingReminderApiIT, NoticeRow, Request, ClubThreads

### Community 6 - "BookingStatus"
Cohesion: 0.25
Nodes (6): BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING, DbConverter

### Community 7 - "TrainerAccountsFixture"
Cohesion: 0.11
Nodes (4): Request, TrainerAdminAccessIT, Request, TrainerInviteJourneyIT

### Community 9 - ".accounts"
Cohesion: 0.10
Nodes (5): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin, RiderReassignJourneyIT

### Community 10 - "Coach"
Cohesion: 0.12
Nodes (5): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Coach, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.14
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.10
Nodes (11): AdminClubUsageController, Access, ClubStatusView, ClubUsageView, Count, BlockedSignIn, LimitedResource, RIDERS (+3 more)

### Community 15 - "Session"
Cohesion: 0.08
Nodes (9): SessionDetailMapper, SeatEvaluation, Trainer, SessionEvaluation, Assignment, Session, SessionTemplate, SessionDetail (+1 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (11): AccessToken, AuthPropertiesTest, JwtConfigTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, EquisApiApplicationIT, RefreshTokenRepositoryIT (+3 more)

### Community 17 - "AssessmentSlotControllerTest"
Cohesion: 0.10
Nodes (10): AdminAssessmentSlotController, AssessmentSlotController, AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, AssessmentSlotMapper (+2 more)

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.10
Nodes (4): MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess, Config

### Community 24 - "RiderAppQueryService"
Cohesion: 0.08
Nodes (10): RiderAppController, ExpiringSoon, RiderAssessment, RiderBanners, RiderPayments, RiderProgress, LevelHistoryEntry, RiderLevelDto (+2 more)

### Community 25 - ".summaries"
Cohesion: 0.14
Nodes (5): MyWeek, WeekDay, WeekSession, LevelSeatCount, LevelSessionCount

### Community 26 - "Session.java"
Cohesion: 0.22
Nodes (8): TenantGuard, AdminAccount, RefreshToken, Club, Payment, SessionUsage, SuperAdmin, TrainerReportRow

### Community 27 - "Repository Layer"
Cohesion: 0.11
Nodes (3): TrainerRiderCount, RefreshTokenRepository, PaymentRepository

### Community 28 - "AdminOverviewService.java"
Cohesion: 0.16
Nodes (11): TrainerRef, ActivityItem, PendingInvite, AdminRiderRow, AdminRiderView, AdminRiderRowAssembler, HowAssigned, CHANGED_MANUALLY (+3 more)

### Community 29 - ".sessions"
Cohesion: 0.16
Nodes (7): DateRanges, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, SessionSeats

### Community 30 - "AssessmentBooking"
Cohesion: 0.12
Nodes (3): AssessmentBooking, AssessmentBookingTest, AssessmentBookingRepositoryIT

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.15
Nodes (4): DeleteImpact, SessionsCancelledEvent, FixedClock, SessionTemplateServiceIT

### Community 35 - "AuthServiceIT"
Cohesion: 0.16
Nodes (3): InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "ApiException"
Cohesion: 0.05
Nodes (17): ApiException, ApiFieldError, AdminBookingController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest, ChangeBookingRequest (+9 more)

### Community 37 - "SessionTemplateRulesIT"
Cohesion: 0.20
Nodes (4): OverlapRef, SessionTemplateInput, Warning, SessionTemplateRulesIT

### Community 38 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.10
Nodes (3): PhoneNormalizerTest, ClubAccessFilterTest, LevelRepositoryIT

### Community 43 - "TestPostgres"
Cohesion: 0.15
Nodes (5): ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT, FreshDatabase, TestPostgres

### Community 48 - "AdminOversightFixture"
Cohesion: 0.12
Nodes (5): AdminOversightAccessIT, BookingOversightJourneyIT, NoShowRebooked, ReminderJourneyIT, AdminOversightFixture

### Community 50 - "RiderReassignApiIT"
Cohesion: 0.07
Nodes (4): AdminRiderApiIT, Request, Request, RiderReassignApiIT

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.17
Nodes (5): BalanceDto, MyRiderDto, UpcomingSeat, FixedClock, MyRidersQueryServiceIT

### Community 54 - "AuthUser"
Cohesion: 0.05
Nodes (13): AuthUser, AuthUserAuthentication, AuthUserConverter, Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER (+5 more)

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 62 - "org.slf4j.Logger"
Cohesion: 0.14
Nodes (4): BookingCancellationListener, LogNotificationSender, AssignmentCancellationListener, SessionGenerationJob

### Community 63 - "EndpointIsolationIT"
Cohesion: 0.15
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 64 - "Tenant Context"
Cohesion: 0.18
Nodes (3): TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 65 - "AdminOverviewService"
Cohesion: 0.20
Nodes (4): OverviewResponse, TodaySession, SeatHolder, AdminOverviewService

### Community 66 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.14
Nodes (6): DbConverter, DbConverter, DbConverter, DbConverter, DbConverter, DayOfWeekConverter

### Community 67 - ".status"
Cohesion: 0.08
Nodes (3): SecurityConfigTest, BookingControllerTest, MySessionsControllerTest

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 69 - "TokenGenerator"
Cohesion: 0.18
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 70 - "Subscription"
Cohesion: 0.10
Nodes (11): PlatformSubscriptionController, AddSubscriptionRequest, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionView, VoidSubscriptionRequest (+3 more)

### Community 71 - "list"
Cohesion: 0.05
Nodes (12): ErrorCodes, SchoolWeek, TrackFill, WeekFill, TrainerStatus, ACTIVE, DISABLED, INVITED (+4 more)

### Community 72 - "ClubAccessService"
Cohesion: 0.31
Nodes (3): ClubAccess, ClubAccessService, ClubAccessServiceTest

### Community 74 - "Booking Actions"
Cohesion: 0.13
Nodes (10): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+2 more)

### Community 76 - "PaymentState"
Cohesion: 0.24
Nodes (4): PaymentEntry, PaymentUsage, UsageLine, PaymentStateTest

### Community 78 - "BillingPeriod"
Cohesion: 0.08
Nodes (23): ClubAccessView, Admin, ClubRow, AdminStatus, ACTIVE, INVITED, ClubStatus, ACTIVE (+15 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "OnExpiry"
Cohesion: 0.28
Nodes (4): PlanView, OnExpiry, BLOCK, READ_ONLY

### Community 83 - "SessionUsageServiceIT"
Cohesion: 0.17
Nodes (3): VoidPaymentRequest, FixedClock, SessionUsageServiceIT

### Community 84 - "LevelChangeRulesTest.java"
Cohesion: 0.13
Nodes (6): Direction, DOWN, UP, LevelChanged, LevelChangeRequest, LevelChangeRulesTest

### Community 86 - "Member"
Cohesion: 0.12
Nodes (4): UpdateMemberRequest, Member, MemberTest, MemberRepositoryIT

### Community 88 - "RiderRepository"
Cohesion: 0.09
Nodes (19): AdminMemberController, AdminAccountRepository, InviteRepository, TrainerRepository, AssessmentBookingRepository, MemberRepository, NoticeRepository, RiderRepository (+11 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 91 - "PlatformClubController.java"
Cohesion: 0.10
Nodes (13): PlatformClubController, ChangeClubStatusRequest, ClubAdminInput, ClubDetail, ClubPage, CreateClubRequest, ClubAdminService, Entry (+5 more)

### Community 92 - "EvaluationFollowUpQuery"
Cohesion: 0.20
Nodes (10): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, EvaluationFollowUpQuery (+2 more)

### Community 98 - "SecurityConfig.java"
Cohesion: 0.08
Nodes (5): SecurityConfig, ClubAccessFilter, TenantContext, TenantFilter, SubscriptionExpiredException

### Community 99 - "ClubRepository"
Cohesion: 0.23
Nodes (3): ClubRepository, PlanRepository, ClubOnboardingService

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "AuthTokenService"
Cohesion: 0.14
Nodes (5): AuthTokens, ClubRef, AuthTokenService, Issued, AuthTokenServiceIT

### Community 106 - ".voidPayment"
Cohesion: 0.23
Nodes (5): AdminPaymentController, PaymentResult, RecordPaymentRequest, RiderPayments, AdminPaymentService

### Community 110 - "AssignmentRepository"
Cohesion: 0.12
Nodes (12): MyRidersController, RiderEvaluations, EvaluationHistoryRow, LevelChangeRepository, AssignmentRepository, EvaluationHistoryQuery, LevelChangeRules, LevelChangeService (+4 more)

### Community 111 - "SessionGeneratorIT"
Cohesion: 0.23
Nodes (3): GenerationResult, FixedClock, SessionGeneratorIT

### Community 115 - "SessionTemplateController.java"
Cohesion: 0.17
Nodes (8): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse, SessionTemplateUpdateRequest

### Community 119 - "RiderAccountStatus"
Cohesion: 0.36
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 121 - "PaymentMethod"
Cohesion: 0.24
Nodes (6): PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 123 - ".book"
Cohesion: 0.20
Nodes (4): AdminNewMemberRequest, FixedClock, MemberBookingServiceIT, TestMembers

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.20
Nodes (5): ClockConfig, MailConfig, SchedulingConfig, SecurityBeans, TestConfig

### Community 127 - "Cancel Reasons"
Cohesion: 0.20
Nodes (8): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "Trainer"
Cohesion: 0.08
Nodes (6): Level, LevelChange, Rider, NoSessionsLeftException, LevelChangeTest, RiderTest

### Community 133 - "uuid"
Cohesion: 0.07
Nodes (14): RiderEvaluation, RiderMe, RiderPackage, RiderPayment, RiderSession, RiderSessionEvaluation, RiderSessionPage, StarTrendPoint (+6 more)

### Community 135 - "TrainerDisableJourneyIT"
Cohesion: 0.12
Nodes (5): OutgoingEmail, TrainerDisableJourneyIT, RecordingNotificationSender, Config, TrainerAccountsFixture

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

### Community 150 - "TenantTransactions"
Cohesion: 0.16
Nodes (8): Outcome, TenantTransactions, PlatformAuthController, PlatformLoginRequest, SuperAdminRepository, AdminLockoutService, PasswordService, PlatformAuthService

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

### Community 162 - "AuthService"
Cohesion: 0.14
Nodes (4): AuthController, InviteAccepted, RefreshRequest, AuthService

### Community 205 - "SessionRepository"
Cohesion: 0.06
Nodes (19): MySessionsController, EvaluationRequest, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse, PendingEvaluationRow, SessionEvaluationRepository (+11 more)

### Community 206 - "ClubOnboardingService.java"
Cohesion: 0.14
Nodes (12): Constraints, InvitePurpose, INVITE, RESET, SubjectType, ADMIN, RIDER, SUPER_ADMIN (+4 more)

### Community 213 - "pathvariable"
Cohesion: 0.23
Nodes (4): ReportController, ReportExportService, ReportRegistry, ReportWriter

### Community 214 - "SmtpNotificationSender.java"
Cohesion: 0.13
Nodes (6): MailSettings, Mode, LOG, SMTP, SessionProperties, SmtpNotificationSender

### Community 215 - "NoticeRepositoryIT"
Cohesion: 0.14
Nodes (6): DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED, RefTime, NoticeRepositoryIT

### Community 217 - "AdminRiderService"
Cohesion: 0.12
Nodes (8): AdminRiderController, AdminRiderList, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard, AdminRiderService, RiderReassignService

### Community 249 - "ReportColumn"
Cohesion: 0.12
Nodes (8): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ReportColumn, RangeSampleReport

### Community 252 - "SessionTemplateService"
Cohesion: 0.21
Nodes (7): SessionTemplateController, LevelRepository, SessionTemplateRepository, BookedSeats, SessionGenerator, SessionTemplateRules, SessionTemplateService

### Community 255 - "AccountLinkEmails"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 258 - "AuthProperties"
Cohesion: 0.23
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, JwtConfig

### Community 259 - "com.fasterxml.jackson.annotation.JsonValue"
Cohesion: 0.20
Nodes (6): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED

### Community 260 - "TwoClubFixture"
Cohesion: 0.12
Nodes (5): RowLevelSecurityIT, MutableClock, TestSubscriptions, Config, TwoClubFixture

### Community 269 - "SessionStatus"
Cohesion: 0.27
Nodes (6): DbConverter, SessionStatus, CANCELLED, DONE, FINISHED, SCHEDULED

### Community 274 - "SubscriptionStatus"
Cohesion: 0.29
Nodes (6): SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED

## Knowledge Gaps
- **122 isolated node(s):** `com.equestrian:equis-api`, `TEXT`, `DATE`, `DATETIME`, `MONEY_EGP` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 509 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **184 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AdminOversightFixture` connect `AdminOversightFixture` to `assertthat`, `AdminOversightFixture`, `TwoClubFixture`, `TrainerDisableJourneyIT`, `.accounts`, `Coach`, `org.junit.jupiter.api.BeforeEach`, `.reset`, `PlatformSignInJourneyIT`, `PaymentsScenarioIT`?**
  _High betweenness centrality (0.021) - this node is a cross-community bridge._
- **What connects `com.equestrian:equis-api`, `TEXT`, `DATE` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.springframework.web.bind.annotation.GetMapping` be split into smaller, more focused modules?**
  _Cohesion score 0.0563165905631659 - nodes in this community are weakly interconnected._
- **Why does `Rider` connect `Trainer` to `org.springframework.web.bind.annotation.GetMapping`, `uuid`, `Session`, `org.junit.jupiter.api.Test`, `RiderAppQueryService`, `Session.java`, `Repository Layer`, `AuthService`, `RiderReassignApiIT`, `MyRidersQueryServiceIT`, `AdminTrainerApiIT`, `AuthUser`, `list`, `SessionRepository`, `ClubOnboardingService.java`, `SessionUsageServiceIT`, `LevelChangeRulesTest.java`, `AdminTrainerApiIT.java`, `RiderRepository`, `Invite`, `AssignmentRepositoryIT`, `AssignmentRepository`, `RiderAccountStatus`, `PaymentTest`?**
  _High betweenness centrality (0.018) - this node is a cross-community bridge._
- **Should `org.springframework.web.bind.annotation.PostMapping` be split into smaller, more focused modules?**
  _Cohesion score 0.06038961038961039 - nodes in this community are weakly interconnected._
- **Why does `Session` connect `Session` to `Trainer`, `uuid`, `org.junit.jupiter.api.BeforeEach`, `SessionStatus`, `PaymentRepositoryIT`, `AssessmentSlotControllerTest`, `SlotLabels.java`, `.summaries`, `Session.java`, `Repository Layer`, `.sessions`, `AssessmentBooking`, `SessionTemplateServiceIT`, `ApiException`, `MyRidersQueryServiceIT`, `AssignmentServiceIT`, `AdminOverviewService`, `list`, `SessionTest`, `SessionRepository`, `SessionUsageServiceIT`, `RiderRepository`, `EvaluationFollowUpQuery`, `.of`, `AssignmentRepository`, `AssignmentRepositoryIT`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.08856749311294766 - nodes in this community are weakly interconnected._