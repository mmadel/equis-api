# Graph Report - equis-api  (2026-11-22)

## Corpus Check
- 662 files · ~224,035 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5883 nodes · 25294 edges · 264 communities (89 shown, 175 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2725 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `4ea50917`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SheetWriter
- org.junit.jupiter.api.BeforeEach
- assertthat
- .status
- AdminTrainerApiIT
- BookingReminderApiIT
- org.springframework.test.web.servlet.ResultActions
- org.springframework.transaction.annotation.Transactional
- .admin
- .accounts
- Coach
- FinishSessionApiIT
- PlatformAccountActionServiceTest
- PlatformAccountActionServiceTest.java
- org.junit.jupiter.api.Test
- InviteRepositoryIT
- AssessmentSlotQueryServiceIT
- ErrorCodes.java
- PlatformSubscriptionApiIT
- SessionUsageServiceIT
- Session
- Session.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- SlotBookedCount
- .book
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- .login
- AuthUser
- RiderBalancesReportTest.java
- RiderBalancesReportIT
- PlanAndSubscriptionIT
- org.springframework.jdbc.core.JdbcTemplate
- AdminBookingChangeApiIT
- ApiExceptionHandler.java
- TrainerAccessServiceIT
- LevelRiderCount
- IntakeScenarioIT
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- ExpiredSessionsReportIT
- AuthUserConverter.java
- Release Workflow
- .issue
- BookingActions.java
- .of
- EvaluationFollowUpQuery.java
- PlatformAccountActivityIT
- AdminMemberApiIT
- RepositoryIsolationIT.java
- uuid
- PasswordService
- RiderAppQueryService.java
- AssignOnTheDayApiIT
- MySessionsControllerTest
- SubscriptionService.java
- jakarta.persistence.AttributeConverter
- org.springframework.stereotype.Component
- ClubRepositoryIT
- LevelChangeServiceIT
- TenantIsolationIT
- PaymentState
- RecordResultServiceIT
- ClubAdminService
- ClubAccessStateIT
- Prod Environment Check
- Plan
- MailTestConfig.java
- SessionTemplateControllerTest
- AuthTokens
- .current
- AdminTrainerApiIT.java
- AdminBookingRow
- CI Workflow
- notblank
- PaymentsReportIT
- .record
- LevelChangeRulesTest
- TrainerRepositoryIT
- SessionTemplateController.java
- AccountLinkServiceIT
- .register
- org.springframework.stereotype.Component
- EvaluationFollowUpApiIT
- Builder
- CancelReason
- AuthService.java
- AuthProperties
- AdminRiderBalanceApiIT
- MyWeekApiIT
- PlanLimitApiIT
- org.junit.jupiter.params.ParameterizedTest
- TenantFilter.java
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- TokenGenerator
- .reset
- .sessions
- SessionTemplateRulesIT
- AdminPaymentApiIT
- InvitePurpose
- PerClubSignInIT
- com.equestrian.equis.entity.riders.Rider
- SlotDetailDto
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- Docker Compose
- .convert
- AdminAccountRepositoryIT
- LevelChangeRepositoryIT
- AdminMemberEditApiIT
- ApiException
- SessionTemplateRepositoryIT
- E19DemoIT
- TenantAutoConfiguration.java
- E18DemoIT
- Changelog Epics A
- .of
- RiderAccountStatus
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
- .set
- .stateOf
- .voidPayment
- CancelReason
- java.sql.Connection
- Graphify Rules Docs
- SubjectType
- Release 2.0.1
- Maven POM
- PaymentMethod
- TrainerRef
- AccountLinkMailListenerIT.java
- E17DemoIT
- AuthProperties
- AccountLinkEmails
- ReportDefinition
- ClubDetail
- TrainerAccountsFixture
- EndpointCoverageGuardIT.java
- Invite
- TenantIdentifierResolver
- MailConfig.java
- TenantTransactions
- LevelRepositoryIT
- .login
- ReportColumn
- CountingEncoder
- ReportExportIT
- TrainersReport
- ReportParams
- SmtpNotificationSender.java
- AuthUser
- SecurityConfig.java
- Override
- XlsxReportWriterTest
- AccessReason

## God Nodes (most connected - your core abstractions)
1. `ApiException` - 122 edges
2. `IntegrationTestBase` - 112 edges
3. `Session` - 107 edges
4. `Rider` - 95 edges
5. `RiderRepository` - 85 edges
6. `AuthUser` - 84 edges
7. `AdminOversightFixture` - 83 edges
8. `Trainer` - 82 edges
9. `PostgresContainer` - 77 edges
10. `AssessmentBookingRepository` - 72 edges

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

## Communities (264 total, 175 thin omitted)

### Community 0 - "SheetWriter"
Cohesion: 0.10
Nodes (10): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, ReportTotal (+2 more)

### Community 1 - "org.junit.jupiter.api.BeforeEach"
Cohesion: 0.08
Nodes (3): DemoModeMailOnIT, RowLevelSecurityIT, IntegrationTestBase

### Community 2 - "assertthat"
Cohesion: 0.06
Nodes (21): AssessmentBookingRepositoryIT, SessionEvaluationRepositoryIT, NoticeRepositoryIT, AssignmentRepositoryIT, FixedClock, FixedClock, FixedClock, MyLevelsServiceIT (+13 more)

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.16
Nodes (3): BookingReminderApiIT, NoticeRow, Request

### Community 6 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.11
Nodes (5): AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture, Coach

### Community 7 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.07
Nodes (19): DateRanges, SchoolWeek, Assignment, LevelRepository, RiderRepository, AssignmentRepository, RecordResultService, EvaluationHistoryQuery (+11 more)

### Community 8 - ".admin"
Cohesion: 0.05
Nodes (7): Request, DemoModeIT, TrainerAdminAccessIT, TrainerDisableJourneyIT, Request, TrainerInviteJourneyIT, RiderAccountJourneyIT

### Community 9 - ".accounts"
Cohesion: 0.15
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.12
Nodes (5): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Check, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.14
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlatformAccountActionServiceTest"
Cohesion: 0.12
Nodes (4): AccountActivityPage, AccountActivityRow, IssuedLink, PlatformAccountActionServiceTest

### Community 15 - "PlatformAccountActionServiceTest.java"
Cohesion: 0.04
Nodes (13): ClubStatusController, TrainerTotals, AccountPasswords, AuthService, AuthTokenService, DemoModeAccounts, TrainerAdminService, ClubBlockedException (+5 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.03
Nodes (11): AuthPropertiesTest, JwtConfigTest, MailDemoModeConfigTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, EquisApiApplicationIT, RefreshTokenRepositoryIT (+3 more)

### Community 20 - "ErrorCodes.java"
Cohesion: 0.08
Nodes (6): AssessmentSlotControllerTest, BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess, Config

### Community 25 - "Session"
Cohesion: 0.06
Nodes (17): AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill, WeekFill (+9 more)

### Community 26 - "Session.java"
Cohesion: 0.08
Nodes (17): TenantGuard, AdminAccount, RefreshToken, Trainer, Club, SessionEvaluation, Level, Notice (+9 more)

### Community 27 - "Repository Layer"
Cohesion: 0.11
Nodes (4): TrainerRiderCount, AdminAccountRepository, RefreshTokenRepository, PlatformAccountActionRepository

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.15
Nodes (4): DeleteImpact, SessionsCancelledEvent, SessionTemplateService, SessionTemplateServiceIT

### Community 35 - ".login"
Cohesion: 0.13
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "AuthUser"
Cohesion: 0.04
Nodes (32): AdminTrainerController, AuthController, AdminAssessmentSlotController, AssessmentSlotController, BookingController, MyEvaluationsController, MyLevelsController, AdminPaymentController (+24 more)

### Community 37 - "RiderBalancesReportTest.java"
Cohesion: 0.09
Nodes (6): PaymentReportRepository, PaymentsReport, RiderBalanceReportRow, RiderBalancesReport, PaymentsReportTest, RiderBalancesReportTest

### Community 43 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.06
Nodes (8): ProdDemoDataCleanupIT, ApplicantIntakeMigrationIT, EmptyDatabaseStartIT, MigrationFromRelease2IT, RiderAccountMigrationIT, SessionFinishedMigrationIT, FreshDatabase, TestPostgres

### Community 48 - "IntakeScenarioIT"
Cohesion: 0.14
Nodes (4): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person

### Community 50 - "RiderReassignApiIT"
Cohesion: 0.08
Nodes (4): AdminRiderApiIT, Request, Request, RiderReassignApiIT

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.19
Nodes (5): BalanceDto, MyRiderDto, UpcomingSeat, FixedClock, MyRidersQueryServiceIT

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "BookingActions.java"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 59 - ".of"
Cohesion: 0.06
Nodes (3): LevelChangeApiIT, SessionTest, SessionRepositoryIT

### Community 61 - "EvaluationFollowUpQuery.java"
Cohesion: 0.08
Nodes (19): AdminEvaluationController, MySessionsController, SessionDetailMapper, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession (+11 more)

### Community 64 - "RepositoryIsolationIT.java"
Cohesion: 0.08
Nodes (14): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+6 more)

### Community 65 - "uuid"
Cohesion: 0.05
Nodes (4): RiderEvaluation, SeatEvaluation, RiderPayment, EvaluationHistoryRow

### Community 66 - "PasswordService"
Cohesion: 0.18
Nodes (5): SuperAdminRepository, AdminLockoutService, PasswordService, PlatformAuthService, PasswordServiceTest

### Community 67 - "RiderAppQueryService.java"
Cohesion: 0.04
Nodes (35): RiderAppController, PaymentEntry, PaymentUsage, RiderPayments, ExpiringSoon, RiderAssessment, RiderBanners, RiderMe (+27 more)

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 70 - "SubscriptionService.java"
Cohesion: 0.09
Nodes (6): PlatformSubscriptionController, Subscription, ClubRepository, SubscriptionRepository, ClubAccessService, SubscriptionService

### Community 71 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.11
Nodes (6): DbConverter, DbConverter, DbConverter, DbConverter, DayOfWeekConverter, DbConverter

### Community 72 - "org.springframework.stereotype.Component"
Cohesion: 0.09
Nodes (9): SessionTemplateRepository, BookingCancellationListener, LogNotificationSender, AssessmentSeatCounts, AssignmentCancellationListener, SessionGenerationJob, SessionGenerator, SessionTemplateRules (+1 more)

### Community 74 - "LevelChangeServiceIT"
Cohesion: 0.06
Nodes (3): AdminMemberCloseApiIT, LevelChangeFlowIT, LevelChangeServiceIT

### Community 76 - "PaymentState"
Cohesion: 0.22
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 77 - "RecordResultServiceIT"
Cohesion: 0.28
Nodes (3): RecordResultRequest, ResultRecorded, RecordResultServiceIT

### Community 78 - "ClubAdminService"
Cohesion: 0.11
Nodes (10): ClubPage, ClubAdminService, Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME (+2 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plan"
Cohesion: 0.15
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 85 - "AuthTokens"
Cohesion: 0.09
Nodes (14): AdminClubUsageController, Access, AuthTokens, ClubRef, ClubStatusView, ClubUsageView, Count, BlockedSignIn (+6 more)

### Community 86 - ".current"
Cohesion: 0.13
Nodes (4): TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, PlatformPingController

### Community 88 - "AdminBookingRow"
Cohesion: 0.04
Nodes (31): AdminBookingController, AdminMemberController, AdminBookingList, AdminBookingRow, AdminNewMemberRequest, BookingTotals, CancelBookingRequest, ChangeBookingRequest (+23 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.13
Nodes (4): PhoneNormalizer, ClubAdminInput, CreateClubRequest, VoidSubscriptionRequest

### Community 95 - "SessionTemplateController.java"
Cohesion: 0.17
Nodes (10): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+2 more)

### Community 98 - "org.springframework.stereotype.Component"
Cohesion: 0.21
Nodes (4): ExpiredPackageReportRow, ExpiredPackageReportRepository, ExpiredSessionsReport, ExpiredSessionsReportTest

### Community 102 - "CancelReason"
Cohesion: 0.13
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 109 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.11
Nodes (3): PhoneNormalizerTest, ClubAccessFilterTest, BookingActionsTest

### Community 113 - "TokenGenerator"
Cohesion: 0.17
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 115 - ".sessions"
Cohesion: 0.11
Nodes (11): LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyWeek, WeekDay, WeekSession (+3 more)

### Community 116 - "SessionTemplateRulesIT"
Cohesion: 0.19
Nodes (4): OverlapRef, SessionTemplateInput, Warning, SessionTemplateRulesIT

### Community 118 - "InvitePurpose"
Cohesion: 0.16
Nodes (6): DbConverter, InvitePurpose, INVITE, RESET, AccountLinkCreatedEvent, AccountLinkEmailsTest

### Community 121 - "SlotDetailDto"
Cohesion: 0.29
Nodes (4): AssessmentSlotDayDto, AssessmentSlotDto, BookingRowDto, SlotDetailDto

### Community 123 - ".book"
Cohesion: 0.23
Nodes (3): FixedClock, MemberBookingServiceIT, TestMembers

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.21
Nodes (4): ClockConfig, SchedulingConfig, SecurityBeans, TestConfig

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "LevelChangeRepositoryIT"
Cohesion: 0.10
Nodes (3): LevelChangeTest, RiderTest, LevelChangeRepositoryIT

### Community 133 - "ApiException"
Cohesion: 0.06
Nodes (20): ApiException, ApiFieldError, Constraints, ErrorCodes, Direction, DOWN, UP, BookingStatus (+12 more)

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 140 - ".of"
Cohesion: 0.14
Nodes (3): Chunks, Iterator<>, ChunksTest

### Community 141 - "RiderAccountStatus"
Cohesion: 0.36
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

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

### Community 158 - ".voidPayment"
Cohesion: 0.25
Nodes (3): PaymentResult, VoidPaymentRequest, ClubThreads

### Community 159 - "CancelReason"
Cohesion: 0.20
Nodes (8): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER

### Community 162 - "SubjectType"
Cohesion: 0.12
Nodes (12): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, DbConverter, SubjectType, ADMIN (+4 more)

### Community 205 - "PaymentMethod"
Cohesion: 0.27
Nodes (7): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 206 - "TrainerRef"
Cohesion: 0.07
Nodes (22): AdminRiderController, TrainerRef, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard (+14 more)

### Community 209 - "AuthProperties"
Cohesion: 0.24
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, SessionProperties

### Community 212 - "AccountLinkEmails"
Cohesion: 0.11
Nodes (5): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender, OutgoingEmail

### Community 213 - "ReportDefinition"
Cohesion: 0.12
Nodes (6): ReportController, ReportDefinition, ReportExportService, ReportRegistry, ReportWriter, AllTypesReport

### Community 214 - "ClubDetail"
Cohesion: 0.09
Nodes (11): PlatformClubController, ChangeClubStatusRequest, Admin, ClubDetail, AdminStatus, ACTIVE, INVITED, ClubStatus (+3 more)

### Community 215 - "TrainerAccountsFixture"
Cohesion: 0.10
Nodes (4): TrainerResetJourneyIT, MutableClock, RecordingNotificationSender, TrainerAccountsFixture

### Community 217 - "Invite"
Cohesion: 0.07
Nodes (9): LevelCount, PendingLink, RiderCounts, Invite, InviteRepository, AssessorCount, TrainerLevelCount, TrainerAccessService (+1 more)

### Community 254 - "TrainersReport"
Cohesion: 0.16
Nodes (4): TrainerReportRow, RiderReportRepository, TrainerReportRepository, TrainersReport

### Community 257 - "ReportParams"
Cohesion: 0.11
Nodes (5): ReportParams, ReportParamsFactory, ReportParamsFactoryTest, Snapshot, ReportParamsTest

### Community 258 - "SmtpNotificationSender.java"
Cohesion: 0.17
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 263 - "Override"
Cohesion: 0.17
Nodes (3): Named, ReportRegistryTest, Totalled

### Community 268 - "XlsxReportWriterTest"
Cohesion: 0.16
Nodes (3): AllTypesReport, Line, XlsxReportWriterTest

### Community 274 - "AccessReason"
Cohesion: 0.08
Nodes (27): AddSubscriptionRequest, ClubAccessView, ClubRow, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionStatus (+19 more)

## Knowledge Gaps
- **122 isolated node(s):** `com.equestrian:equis-api`, `SMTP`, `LOG`, `DATE`, `DATETIME` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 514 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **175 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InvitePurpose` connect `InvitePurpose` to `AccountLinkServiceIT`, `uuid`, `jakarta.persistence.AttributeConverter`, `PlatformAccountActionServiceTest.java`, `Invite`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **What connects `com.equestrian:equis-api`, `SMTP`, `LOG` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SheetWriter` be split into smaller, more focused modules?**
  _Cohesion score 0.09851551956815115 - nodes in this community are weakly interconnected._
- **Why does `ApiException` connect `ApiException` to `assertthat`, `AuthUser`, `org.springframework.transaction.annotation.Transactional`, `AccessReason`, `.status`, `Session`, `Session.java`, `.voidPayment`, `SessionTemplateServiceIT`, `AuthUser`, `ApiExceptionHandler.java`, `TrainerAccessServiceIT`, `.of`, `EvaluationFollowUpQuery.java`, `PasswordService`, `RiderAppQueryService.java`, `SubscriptionService.java`, `org.springframework.stereotype.Component`, `ClubAdminService`, `TrainerRef`, `ClubDetail`, `AdminBookingRow`, `Invite`, `TenantTransactions`, `SessionTemplateController.java`, `AccountLinkServiceIT`, `.sessions`, `SessionTemplateRulesIT`, `.login`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `org.junit.jupiter.api.BeforeEach` be split into smaller, more focused modules?**
  _Cohesion score 0.07529411764705882 - nodes in this community are weakly interconnected._
- **Why does `Session` connect `Session` to `uuid`, `SessionTemplateServiceIT`, `RiderAppQueryService.java`, `ApiException`, `org.springframework.transaction.annotation.Transactional`, `Repository Layer`, `PaymentRepositoryIT`, `MyRidersQueryServiceIT`, `.sessions`, `AdminBookingRow`, `SessionUsageServiceIT`, `Session.java`, `.of`, `EvaluationFollowUpQuery.java`, `.book`?**
  _High betweenness centrality (0.012) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.05521472392638037 - nodes in this community are weakly interconnected._