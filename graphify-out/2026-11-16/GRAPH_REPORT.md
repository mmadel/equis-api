# Graph Report - equis-api  (2026-11-16)

## Corpus Check
- 608 files · ~198,875 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5435 nodes · 23414 edges · 260 communities (78 shown, 182 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2572 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `60599256`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- com.equestrian.equis.service.platform.ClubAccessService
- org.springframework.web.bind.annotation.PostMapping
- assertthat
- .status
- AdminMemberCloseApiIT
- AdminTrainerApiIT
- AdminRiderApiIT
- AdminMemberApiIT
- TrainerAccountsFixture
- .accounts
- Coach
- org.junit.jupiter.api.BeforeEach
- FinishSessionApiIT
- PlanLimitService
- Session
- org.junit.jupiter.api.Test
- AuthUser
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- RiderAppQueryService.java
- AdminOverviewService.java
- Session.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- .sessions
- AssessmentRulesIT
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- AuthServiceIT
- org.springframework.web.bind.annotation.GetMapping
- SessionTemplateRulesIT
- org.junit.jupiter.params.ParameterizedTest
- PlanAndSubscriptionIT
- TestPostgres
- AdminBookingChangeApiIT
- LevelChangeFlowIT
- RiderRepositoryIT
- AdminOversightFixture
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- MyRidersControllerTest
- .convert
- Release Workflow
- .issue
- RepositoryIsolationIT.java
- FreshDatabase.java
- AssignmentServiceIT
- API Exception Handler
- SeedDataIT
- EndpointIsolationIT
- Tenant Context
- com.equestrian.equis.service.platform.AccessReason
- .status
- AssignOnTheDayApiIT
- TokenGenerator
- AuthTokenService
- ApiException
- ClubAccessService
- ClubRepositoryIT
- Booking Actions
- TenantIsolationIT
- PaymentMethod
- SessionTest
- BillingPeriod
- ClubAccessStateIT
- Prod Environment Check
- Plan
- SessionUsageServiceIT
- LevelChangeRulesTest.java
- PerClubSignInIT
- LevelRepositoryIT
- AdminTrainerApiIT.java
- AssessmentBookingRepository
- CI Workflow
- notblank
- .list
- EvaluationFollowUpQuery
- .hash
- TrainerRepositoryIT
- LevelChangeServiceIT
- AccountLinkServiceIT
- .of
- SecurityConfig.java
- LimitedResource
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthTokenService
- AuthProperties
- .voidPayment
- MyWeekApiIT
- PlanLimitApiIT
- AdminStatus
- RiderRepository
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- .balanceOn
- .reset
- SessionTemplateController.java
- .admin
- AdminPaymentApiIT
- AccountLinkEmailsTest
- RiderLevelQueryServiceIT
- Request
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- Docker Compose
- Default Club Extension
- AdminAccountRepositoryIT
- Trainer
- AdminMemberEditApiIT
- uuid
- SessionTemplateRepositoryIT
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
- Changelog Epics A
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
- ProdDemoDataCleanupIT.java
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
- pathvariable
- SmtpNotificationSender.java
- NoticeRepositoryIT
- SessionEvaluationRepositoryIT
- TrainerStatus
- Invite
- MigrationFromRelease2IT
- AssignmentRepositoryIT
- AccountLinkMailListenerIT.java
- ReportColumn
- MailTestConfig.java
- SessionTemplateService
- ReportExportIT
- TrainersReport
- AccountLinkEmails
- TenantIdentifierResolver
- ReportParamsFactoryTest
- AuthProperties
- SeedDataIT.java
- TenantFilterTest.java
- .total
- Named
- XlsxReportWriterTest.java
- ReportParams
- ClubUsageService
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

## Communities (260 total, 182 thin omitted)

### Community 1 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.07
Nodes (12): AdminTrainerController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals (+4 more)

### Community 2 - "assertthat"
Cohesion: 0.08
Nodes (10): FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, SessionDetailServiceIT, FixedClock (+2 more)

### Community 5 - "AdminTrainerApiIT"
Cohesion: 0.08
Nodes (4): AdminTrainerApiIT, BookingReminderApiIT, NoticeRow, Request

### Community 8 - "TrainerAccountsFixture"
Cohesion: 0.06
Nodes (10): OutgoingEmail, Request, TrainerAdminAccessIT, TrainerDisableJourneyIT, Request, TrainerInviteJourneyIT, TrainerResetJourneyIT, RiderAccountJourneyIT (+2 more)

### Community 9 - ".accounts"
Cohesion: 0.12
Nodes (5): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin, RiderReassignJourneyIT

### Community 10 - "Coach"
Cohesion: 0.11
Nodes (5): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Coach, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.20
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.31
Nodes (4): AdminClubUsageController, ClubUsageView, Count, PlanLimitService

### Community 15 - "Session"
Cohesion: 0.08
Nodes (9): SeatEvaluation, SessionEvaluation, Rider, Assignment, Session, SessionTemplate, NoSessionsLeftException, SessionDetail (+1 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (10): AuthPropertiesTest, JwtConfigTest, RoleTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, EquisApiApplicationIT, InviteRepositoryIT (+2 more)

### Community 17 - "AuthUser"
Cohesion: 0.09
Nodes (3): AuthUser, AccessToken, JwtServiceTest

### Community 19 - "AssessmentSlotQueryServiceIT"
Cohesion: 0.09
Nodes (8): AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, SlotBookedCount, AssessmentSlotQueryServiceIT, FixedClock

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.11
Nodes (6): AssessmentSlotControllerTest, BookingControllerTest, MyLevelsControllerTest, SessionTemplateControllerTest, ActiveClubAccess, Config

### Community 24 - "RiderAppQueryService.java"
Cohesion: 0.05
Nodes (23): RiderAppController, ExpiringSoon, RiderAssessment, RiderBanners, RiderMe, RiderPackage, RiderPayment, RiderPayments (+15 more)

### Community 25 - "AdminOverviewService.java"
Cohesion: 0.05
Nodes (17): SchoolWeek, AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill (+9 more)

### Community 26 - "Session.java"
Cohesion: 0.14
Nodes (9): TenantGuard, AdminAccount, RefreshToken, Club, Notice, SessionUsage, Subscription, SuperAdmin (+1 more)

### Community 27 - "Repository Layer"
Cohesion: 0.09
Nodes (5): InviteRepository, RefreshTokenRepository, SuperAdminRepository, LevelChangeRepository, TrainerAccessService

### Community 29 - ".sessions"
Cohesion: 0.15
Nodes (8): LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, SessionSeatAssembler, SessionSeats, MyLevelsServiceIT

### Community 30 - "AssessmentRulesIT"
Cohesion: 0.05
Nodes (15): RecordResultRequest, ResultRecorded, CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL (+7 more)

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.17
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceIT

### Community 35 - "AuthServiceIT"
Cohesion: 0.14
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.03
Nodes (31): AdminAssessmentSlotController, AdminBookingController, AdminMemberController, AssessmentSlotController, MyEvaluationsController, MyLevelsController, PlatformAuthController, AdminRiderAccountController (+23 more)

### Community 43 - "TestPostgres"
Cohesion: 0.21
Nodes (4): ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 48 - "AdminOversightFixture"
Cohesion: 0.09
Nodes (7): AdminOversightAccessIT, BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person, ReminderJourneyIT, AdminOversightFixture

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.18
Nodes (4): BalanceDto, MyRiderDto, FixedClock, MyRidersQueryServiceIT

### Community 54 - ".convert"
Cohesion: 0.10
Nodes (8): AuthUserAuthentication, AuthUserConverter, Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, AuthUserConverterTest

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "RepositoryIsolationIT.java"
Cohesion: 0.10
Nodes (3): TenantContext, RepositoryIsolationIT, RowLevelSecurityIT

### Community 63 - "EndpointIsolationIT"
Cohesion: 0.17
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 64 - "Tenant Context"
Cohesion: 0.13
Nodes (4): TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, PlatformPingController

### Community 67 - ".status"
Cohesion: 0.09
Nodes (3): EvaluationRequest, SecurityConfigTest, MySessionsControllerTest

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.16
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 70 - "AuthTokenService"
Cohesion: 0.04
Nodes (32): Outcome, TenantTransactions, AuthController, PlatformClubController, PlatformSubscriptionController, AddSubscriptionRequest, ClubDetail, CreateClubRequest (+24 more)

### Community 71 - "ApiException"
Cohesion: 0.06
Nodes (17): ApiException, ApiFieldError, ErrorCodes, DateRanges, ClubPage, RiderSessionPage, Trainer, Levels (+9 more)

### Community 74 - "Booking Actions"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 76 - "PaymentMethod"
Cohesion: 0.08
Nodes (18): PaymentEntry, PaymentUsage, DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET (+10 more)

### Community 78 - "BillingPeriod"
Cohesion: 0.10
Nodes (20): ChangeClubStatusRequest, ClubAccessView, ClubRow, ClubStatus, ACTIVE, SUSPENDED, BillingPeriod, ANNUAL (+12 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plan"
Cohesion: 0.17
Nodes (5): OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 83 - "SessionUsageServiceIT"
Cohesion: 0.17
Nodes (3): VoidPaymentRequest, FixedClock, SessionUsageServiceIT

### Community 84 - "LevelChangeRulesTest.java"
Cohesion: 0.14
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 88 - "AssessmentBookingRepository"
Cohesion: 0.04
Nodes (29): Constraints, BookingController, BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING, DbConverter (+21 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 91 - ".list"
Cohesion: 0.14
Nodes (6): Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME

### Community 92 - "EvaluationFollowUpQuery"
Cohesion: 0.13
Nodes (16): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, DbConverter (+8 more)

### Community 99 - "LimitedResource"
Cohesion: 0.38
Nodes (4): LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 106 - ".voidPayment"
Cohesion: 0.29
Nodes (5): AdminPaymentController, PaymentResult, RecordPaymentRequest, RiderPayments, AdminPaymentService

### Community 109 - "AdminStatus"
Cohesion: 0.40
Nodes (4): Admin, AdminStatus, ACTIVE, INVITED

### Community 110 - "RiderRepository"
Cohesion: 0.06
Nodes (28): MyRidersController, MySessionsController, PendingEvaluations, PendingSession, RiderEvaluations, TrainerRepository, EvaluationHistoryRow, PendingEvaluationRow (+20 more)

### Community 111 - "SessionGeneratorIT"
Cohesion: 0.23
Nodes (3): GenerationResult, FixedClock, SessionGeneratorIT

### Community 114 - ".reset"
Cohesion: 0.10
Nodes (3): ApiExceptionHandlerTest, Schedule, Check

### Community 115 - "SessionTemplateController.java"
Cohesion: 0.19
Nodes (8): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse, SessionTemplateUpdateRequest

### Community 116 - ".admin"
Cohesion: 0.15
Nodes (3): OverlapRef, SaveResult, Warning

### Community 123 - ".book"
Cohesion: 0.24
Nodes (3): AdminNewMemberRequest, MemberBookingServiceIT, TestMembers

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.19
Nodes (5): ClockConfig, MailConfig, SchedulingConfig, SecurityBeans, TestConfig

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "Trainer"
Cohesion: 0.07
Nodes (8): LevelChanged, LevelHistoryEntry, RiderLevelDto, UpcomingSeat, Level, LevelChange, LevelChangeTest, RiderTest

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
Cohesion: 0.15
Nodes (4): Access, AuthTokens, ClubRef, Issued

### Community 205 - ".get"
Cohesion: 0.16
Nodes (5): SessionDetailMapper, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse

### Community 206 - "ClubOnboardingService.java"
Cohesion: 0.05
Nodes (27): RiderAccount, DbConverter, InvitePurpose, INVITE, RESET, DbConverter, SubjectType, ADMIN (+19 more)

### Community 213 - "pathvariable"
Cohesion: 0.19
Nodes (6): ReportController, ReportDefinition, ReportExportService, ReportParamsFactory, ReportRegistry, ReportWriter

### Community 214 - "SmtpNotificationSender.java"
Cohesion: 0.19
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 217 - "TrainerStatus"
Cohesion: 0.07
Nodes (21): TrainerRef, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard, DbConverter (+13 more)

### Community 249 - "ReportColumn"
Cohesion: 0.12
Nodes (8): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ReportColumn, RangeSampleReport

### Community 252 - "SessionTemplateService"
Cohesion: 0.19
Nodes (5): SessionTemplateController, BookedSeats, SessionTemplateInput, SessionTemplateRules, SessionTemplateService

### Community 255 - "AccountLinkEmails"
Cohesion: 0.23
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 258 - "AuthProperties"
Cohesion: 0.23
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, SessionProperties

### Community 268 - "XlsxReportWriterTest.java"
Cohesion: 0.13
Nodes (3): XlsxReportWriter, Item, XlsxReportWriterTest

### Community 274 - "SubscriptionStatus"
Cohesion: 0.20
Nodes (6): SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED

## Knowledge Gaps
- **122 isolated node(s):** `CLOSED`, `NO_SHOW`, `PENDING_ASSESSMENT`, `RIDER`, `ACTIVE` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 510 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **182 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Rider` connect `Session` to `assertthat`, `Trainer`, `AdminTrainerApiIT`, `PaymentRepositoryIT`, `RiderAppQueryService.java`, `Session.java`, `Repository Layer`, `AssessmentRulesIT`, `AuthTokens`, `RiderRepositoryIT`, `MyRidersQueryServiceIT`, `ApiException`, `ClubOnboardingService.java`, `SessionUsageServiceIT`, `LevelChangeRulesTest.java`, `AdminTrainerApiIT.java`, `AssessmentBookingRepository`, `Invite`, `RiderRepository`, `AssignmentRepositoryIT`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **What connects `CLOSED`, `NO_SHOW`, `PENDING_ASSESSMENT` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.springframework.web.bind.annotation.PostMapping` be split into smaller, more focused modules?**
  _Cohesion score 0.06862745098039216 - nodes in this community are weakly interconnected._
- **Why does `ApiException` connect `ApiException` to `assertthat`, `Session`, `RiderAppQueryService.java`, `Repository Layer`, `.sessions`, `AssessmentRulesIT`, `org.springframework.web.bind.annotation.GetMapping`, `SessionTemplateRulesIT`, `AssignmentServiceIT`, `API Exception Handler`, `AuthTokenService`, `SessionTest`, `ClubOnboardingService.java`, `.get`, `SessionUsageServiceIT`, `LevelChangeRulesTest.java`, `AssessmentBookingRepository`, `TrainerStatus`, `EvaluationFollowUpQuery`, `.hash`, `.voidPayment`, `RiderRepository`, `.reset`, `SessionTemplateService`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.018) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.08393650793650793 - nodes in this community are weakly interconnected._
- **Why does `AdminOversightFixture` connect `AdminOversightFixture` to `assertthat`, `SeedDataIT.java`, `org.springframework.jdbc.core.JdbcTemplate`, `TrainerAccountsFixture`, `.accounts`, `Coach`, `org.junit.jupiter.api.BeforeEach`, `.reset`, `PaymentsScenarioIT`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **Should `AdminTrainerApiIT` be split into smaller, more focused modules?**
  _Cohesion score 0.08143839238498149 - nodes in this community are weakly interconnected._