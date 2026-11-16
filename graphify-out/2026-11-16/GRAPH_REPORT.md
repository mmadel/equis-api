# Graph Report - equis-api  (2026-11-16)

## Corpus Check
- 632 files · ~209,865 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5733 nodes · 24617 edges · 270 communities (81 shown, 189 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2635 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1c2f08ad`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SheetWriter
- assertthat
- .status
- AdminTrainerApiIT
- BookingReminderApiIT
- org.springframework.test.web.servlet.ResultActions
- AssignmentRepository
- RiderAccountJourneyIT
- .accounts
- Coach
- org.springframework.jdbc.core.JdbcTemplate
- FinishSessionApiIT
- PlanLimitService
- .recorder
- org.junit.jupiter.api.Test
- InviteRepositoryIT
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- AdminPaymentController.java
- Session
- Session.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- SlotBookedCount
- .booking
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- .login
- AuthUser
- RiderBalancesReportTest.java
- RiderBalancesReportIT
- PlanAndSubscriptionIT
- TestPostgres
- AdminBookingChangeApiIT
- LevelChangeServiceIT
- TrainerAccessServiceIT
- LevelRiderCount
- IntakeScenarioIT
- AdminBookingApiIT
- RiderReassignApiIT
- AdminOverviewApiIT
- ExpiredSessionsReportIT
- AuthUserConverter.java
- Release Workflow
- .issue
- EndpointIsolationIT
- FreshDatabase.java
- AssignmentServiceIT
- API Exception Handler
- LevelChangeRulesTest.java
- AdminMemberCloseApiIT
- .runAs
- uuid
- .hash
- AssessmentRulesIT
- AssignOnTheDayApiIT
- MySessionsControllerTest
- SubscriptionService.java
- RecordResultServiceIT
- ClubRepository
- ClubRepositoryIT
- Booking Actions
- TenantIsolationIT
- Payment
- AdminMemberApiIT
- ClubAdminService
- ClubAccessStateIT
- Prod Environment Check
- Plan
- org.springframework.context.annotation.Bean
- AuthService
- .register
- AdminTrainerApiIT.java
- AdminBookingRow
- CI Workflow
- notblank
- PaymentsReportIT
- EvaluationFollowUpQuery
- MigrationFromRelease2IT
- TrainerRepositoryIT
- TrainerAccountsFixture
- AccountLinkServiceIT
- .of
- ExpiredSessionsReport
- XlsxReportWriter.java
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthService.java
- AuthProperties
- ApiExceptionHandlerTest.java
- MyWeekApiIT
- PlanLimitApiIT
- org.junit.jupiter.params.ParameterizedTest
- SessionRepository
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- TokenGenerator
- .reset
- .sessions
- E18DemoIT
- AdminPaymentApiIT
- SubjectType
- SeedDataIT
- AssessmentSlotControllerTest
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- Assignment
- Docker Compose
- .convert
- AdminAccountRepositoryIT
- RiderTest
- AdminMemberEditApiIT
- org.springframework.transaction.annotation.Transactional
- SessionTemplateRepositoryIT
- AuthTokenService
- TenantAutoConfiguration.java
- .doFilterInternal
- Changelog Epics A
- .of
- CancelReason
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- PaymentRepositoryIT
- .set
- Test Mail Rules Docs
- Test Club Rules Docs
- AdminAssessmentSlotApiIT
- Changelog Epics B
- .status
- Image Release Docs
- Notification Sender Docs
- NoticeRepositoryIT
- .stateOf
- LevelChangeApiIT
- Graphify Rules Docs
- TenantFilterTest.java
- Release 2.0.1
- Maven POM
- .status
- Trainer
- DefaultClubExtension.java
- E17DemoIT
- AuthProperties
- EndpointCoverageGuardIT
- ReportDefinition
- TenantTransactions
- .instant
- SessionEvaluationRepositoryIT
- RiderRepository
- ProdDemoDataCleanupIT
- SessionTest
- TrainerDisableJourneyIT
- ReportColumn
- RecordingNotificationSender
- MyLevelsControllerTest
- Invite
- ReportExportIT
- TrainersReport
- Snapshot
- AdminStatus
- ReportParams
- SmtpNotificationSender.java
- ClubAccessFilter.java
- Override
- XlsxReportWriterTest
- AccessReason

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

## Communities (270 total, 189 thin omitted)

### Community 0 - "SheetWriter"
Cohesion: 0.12
Nodes (8): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, SheetWriter

### Community 2 - "assertthat"
Cohesion: 0.07
Nodes (19): ReportNotFoundException, ReportRangeTooLongException, AssessmentBookingRepositoryIT, AssignmentRepositoryIT, FixedClock, MemberBookingServiceIT, FixedClock, RecordResultServiceIT (+11 more)

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.17
Nodes (3): BookingReminderApiIT, NoticeRow, Request

### Community 6 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.10
Nodes (5): AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture, Coach

### Community 7 - "AssignmentRepository"
Cohesion: 0.05
Nodes (24): MyRidersController, EvaluationRequest, RiderEvaluations, LevelChanged, MyRiderDto, RiderLevelDto, UpcomingSeat, EvaluationHistoryRow (+16 more)

### Community 9 - ".accounts"
Cohesion: 0.18
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.12
Nodes (5): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Check, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.15
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.15
Nodes (9): AdminClubUsageController, ClubStatusView, ClubUsageView, Count, LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException (+1 more)

### Community 15 - ".recorder"
Cohesion: 0.12
Nodes (4): Request, TrainerAdminAccessIT, Request, TrainerInviteJourneyIT

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (14): AccessToken, AuthPropertiesTest, TestConfig, JwtConfigTest, AdminRiderBalanceApiIT, CancelReasonTest, MemberStatusTest, PaymentEnumsTest (+6 more)

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.13
Nodes (3): MyRidersControllerTest, ActiveClubAccess, Config

### Community 24 - "AdminPaymentController.java"
Cohesion: 0.08
Nodes (15): AdminPaymentController, PaymentEntry, PaymentResult, PaymentUsage, RecordPaymentRequest, RiderPayments, VoidPaymentRequest, RiderAssessment (+7 more)

### Community 25 - "Session"
Cohesion: 0.08
Nodes (9): AdminOverviewController, OverviewResponse, TodaySession, TrackFill, WeekFill, Session, SeatHolder, AdminOverviewService (+1 more)

### Community 26 - "Session.java"
Cohesion: 0.18
Nodes (9): TenantGuard, AdminAccount, RefreshToken, Club, Notice, SessionUsage, SuperAdmin, TrainerReportRow (+1 more)

### Community 27 - "Repository Layer"
Cohesion: 0.10
Nodes (3): TrainerRiderCount, RefreshTokenRepository, SuperAdminRepository

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.05
Nodes (17): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateResponse, SessionTemplateUpdateRequest, DeleteImpact (+9 more)

### Community 35 - ".login"
Cohesion: 0.14
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "AuthUser"
Cohesion: 0.04
Nodes (36): AuthUser, AdminTrainerController, AdminAssessmentSlotController, AssessmentSlotController, BookingController, MyEvaluationsController, MyLevelsController, PlatformAuthController (+28 more)

### Community 37 - "RiderBalancesReportTest.java"
Cohesion: 0.16
Nodes (5): RiderReportRow, RiderReportRepository, RiderBalanceReportRow, RiderBalancesReport, RiderBalancesReportTest

### Community 43 - "TestPostgres"
Cohesion: 0.18
Nodes (4): ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 45 - "LevelChangeServiceIT"
Cohesion: 0.07
Nodes (4): LevelChangeRepositoryIT, LevelChangeFlowIT, FixedClock, LevelChangeServiceIT

### Community 48 - "IntakeScenarioIT"
Cohesion: 0.14
Nodes (4): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person

### Community 50 - "RiderReassignApiIT"
Cohesion: 0.08
Nodes (4): AdminRiderApiIT, Request, Request, RiderReassignApiIT

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "EndpointIsolationIT"
Cohesion: 0.17
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 62 - "LevelChangeRulesTest.java"
Cohesion: 0.14
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 64 - ".runAs"
Cohesion: 0.06
Nodes (6): TenantContext, TenantIdentifierResolver, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, RepositoryIsolationIT

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 70 - "SubscriptionService.java"
Cohesion: 0.11
Nodes (11): SubscriptionResult, SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED, SubscriptionView (+3 more)

### Community 72 - "ClubRepository"
Cohesion: 0.13
Nodes (3): ClubRepository, ClubAccessService, SessionGenerationJob

### Community 74 - "Booking Actions"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 76 - "Payment"
Cohesion: 0.07
Nodes (16): Payment, DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER (+8 more)

### Community 78 - "ClubAdminService"
Cohesion: 0.12
Nodes (9): ClubAdminService, Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME, ClubUsageService (+1 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plan"
Cohesion: 0.14
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 83 - "org.springframework.context.annotation.Bean"
Cohesion: 0.07
Nodes (6): SecurityBeans, FixedClock, Config, MailTestConfig, Config, Config

### Community 88 - "AdminBookingRow"
Cohesion: 0.05
Nodes (15): AdminBookingController, AdminBookingList, AdminBookingRow, AdminNewMemberRequest, BookingTotals, CancelBookingRequest, ChangeBookingRequest, MemberRef (+7 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 92 - "EvaluationFollowUpQuery"
Cohesion: 0.12
Nodes (16): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, DbConverter (+8 more)

### Community 98 - "ExpiredSessionsReport"
Cohesion: 0.10
Nodes (8): ExpiredPackageReportRow, PaymentReportRow, ExpiredPackageReportRepository, PaymentReportRepository, ExpiredSessionsReport, PaymentsReport, ExpiredSessionsReportTest, PaymentsReportTest

### Community 99 - "XlsxReportWriter.java"
Cohesion: 0.12
Nodes (4): ReportController, ReportExportService, ReportWriter, XlsxReportWriter

### Community 101 - "Two-Club Fixture"
Cohesion: 0.09
Nodes (4): PerClubSignInIT, Builder, ClubData, TwoClubFixture

### Community 102 - "Member Status"
Cohesion: 0.17
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "AuthService.java"
Cohesion: 0.09
Nodes (7): Access, AuthTokens, ClubRef, RefreshRequest, PlatformLoginRequest, Issued, AuthTokenServiceIT

### Community 109 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.11
Nodes (3): PhoneNormalizerTest, LevelRepositoryIT, BookingActionsTest

### Community 110 - "SessionRepository"
Cohesion: 0.05
Nodes (17): SchoolWeek, SessionTemplateController, MyWeek, WeekDay, WeekSession, LevelRepository, LevelSeatCount, LevelSessionCount (+9 more)

### Community 113 - "TokenGenerator"
Cohesion: 0.17
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 115 - ".sessions"
Cohesion: 0.24
Nodes (7): LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, SessionSeatAssembler, SessionSeats

### Community 118 - "SubjectType"
Cohesion: 0.07
Nodes (17): DbConverter, InvitePurpose, INVITE, RESET, DbConverter, SubjectType, ADMIN, RIDER (+9 more)

### Community 121 - "AssessmentSlotControllerTest"
Cohesion: 0.22
Nodes (5): AssessmentSlotDayDto, AssessmentSlotDto, BookingRowDto, SlotDetailDto, AssessmentSlotControllerTest

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.22
Nodes (3): ClockConfig, MailConfig, SchedulingConfig

### Community 127 - "Assignment"
Cohesion: 0.13
Nodes (9): SessionDetailMapper, SeatEvaluation, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse, SessionEvaluation, Assignment (+1 more)

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "RiderTest"
Cohesion: 0.09
Nodes (6): Level, LevelChange, Rider, NoSessionsLeftException, LevelChangeTest, RiderTest

### Community 133 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.05
Nodes (29): ApiException, ApiFieldError, ErrorCodes, DateRanges, RiderAppController, ClubPage, ExpiringSoon, RiderBanners (+21 more)

### Community 135 - "AuthTokenService"
Cohesion: 0.25
Nodes (5): AuthController, AdminLockoutService, AuthTokenService, PasswordService, PlatformAuthService

### Community 138 - ".doFilterInternal"
Cohesion: 0.16
Nodes (3): ClubBlockedException, SubscriptionExpiredException, ClubAccessFilterTest

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 140 - ".of"
Cohesion: 0.23
Nodes (3): Chunks, Iterator<>, ChunksTest

### Community 141 - "CancelReason"
Cohesion: 0.21
Nodes (9): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+1 more)

### Community 142 - "Base URLs & Accounts Docs"
Cohesion: 0.29
Nodes (8): APP_RIDER_BASE_URL, E15 Rider accounts and data, E1 Auth & accounts, E6 Trainer accounts, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (smtp|log), Production environment variables

### Community 143 - "Ops Scripts & Plans Docs"
Cohesion: 0.25
Nodes (8): V5__seed_admin.sql, create-super-admin.sql, on_expiry policy (BLOCK | READ_ONLY), Ops scripts (run by hand), Plans TRIAL / BASIC / PRO / MAX, POST /api/platform/auth/login, upsert-plan.sql, APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH

### Community 144 - "PaymentRepositoryIT"
Cohesion: 0.06
Nodes (4): PaymentRepositoryIT, FixedClock, RiderBalanceServiceIT, ClubThreads

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

### Community 162 - "TenantFilterTest.java"
Cohesion: 0.11
Nodes (6): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, TenantFilter

### Community 206 - "Trainer"
Cohesion: 0.03
Nodes (49): AdminRiderController, TrainerRef, ActivityItem, PendingInvite, BalanceDto, AdminRiderList, AdminRiderRow, ReassignResult (+41 more)

### Community 209 - "AuthProperties"
Cohesion: 0.26
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, SessionProperties

### Community 213 - "ReportDefinition"
Cohesion: 0.16
Nodes (4): ReportDefinition, ReportParams, ReportRegistry, ReportTotal

### Community 217 - "RiderRepository"
Cohesion: 0.04
Nodes (34): Constraints, AdminMemberController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow (+26 more)

### Community 256 - "AdminStatus"
Cohesion: 0.40
Nodes (4): Admin, AdminStatus, ACTIVE, INVITED

### Community 257 - "ReportParams"
Cohesion: 0.15
Nodes (3): ReportParamsFactory, ReportParamsFactoryTest, ReportParamsTest

### Community 258 - "SmtpNotificationSender.java"
Cohesion: 0.16
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 261 - "ClubAccessFilter.java"
Cohesion: 0.14
Nodes (3): SecurityConfig, ClubAccessFilter, ClubStatusController

### Community 263 - "Override"
Cohesion: 0.17
Nodes (3): Named, ReportRegistryTest, Totalled

### Community 268 - "XlsxReportWriterTest"
Cohesion: 0.13
Nodes (4): AllTypesReport, Line, XlsxReportWriterTest, AllTypesReport

### Community 274 - "AccessReason"
Cohesion: 0.11
Nodes (18): ClubAccessView, ClubRow, ClubStatus, ACTIVE, SUSPENDED, BillingPeriod, ANNUAL, MONTHLY (+10 more)

## Knowledge Gaps
- **122 isolated node(s):** `ADMIN`, `TRAINER`, `RIDER`, `LIST`, `OTHER` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 512 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **189 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Session` connect `Session` to `org.springframework.transaction.annotation.Transactional`, `AssignmentRepository`, `PaymentRepositoryIT`, `Session.java`, `Repository Layer`, `.booking`, `SessionTemplateServiceIT`, `AssignmentServiceIT`, `uuid`, `Trainer`, `AssessmentBookingRepositoryIT`, `AdminBookingRow`, `SessionEvaluationRepositoryIT`, `RiderRepository`, `EvaluationFollowUpQuery`, `SessionTest`, `.of`, `SessionRepository`, `.sessions`, `AssignmentRepositoryIT`, `Assignment`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **What connects `ADMIN`, `TRAINER`, `RIDER` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SheetWriter` be split into smaller, more focused modules?**
  _Cohesion score 0.11596638655462185 - nodes in this community are weakly interconnected._
- **Why does `ApiException` connect `org.springframework.transaction.annotation.Transactional` to `assertthat`, `AssignmentRepository`, `.status`, `AdminPaymentController.java`, `Session`, `SessionTemplateServiceIT`, `AuthUser`, `AssignmentServiceIT`, `API Exception Handler`, `LevelChangeRulesTest.java`, `.hash`, `AssessmentRulesIT`, `SubscriptionService.java`, `ClubRepository`, `Trainer`, `TenantTransactions`, `AdminBookingRow`, `RiderRepository`, `EvaluationFollowUpQuery`, `AuthService.java`, `SessionRepository`, `.sessions`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.06816961889425657 - nodes in this community are weakly interconnected._
- **Why does `Rider` connect `RiderTest` to `AdminTrainerApiIT`, `org.springframework.transaction.annotation.Transactional`, `AssignmentRepository`, `org.junit.jupiter.api.Test`, `PaymentRepositoryIT`, `AdminPaymentController.java`, `Session`, `Session.java`, `Repository Layer`, `MyRidersQueryServiceIT`, `LevelChangeRulesTest.java`, `uuid`, `AssessmentRulesIT`, `RecordResultServiceIT`, `Payment`, `Trainer`, `AuthService`, `AdminTrainerApiIT.java`, `SessionEvaluationRepositoryIT`, `RiderRepository`, `SessionRepository`, `AssignmentRepositoryIT`, `TrainerDisableJourneyIT`, `Invite`, `Assignment`?**
  _High betweenness centrality (0.014) - this node is a cross-community bridge._
- **Should `.status` be split into smaller, more focused modules?**
  _Cohesion score 0.14772727272727273 - nodes in this community are weakly interconnected._