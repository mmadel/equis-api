# Graph Report - equis-api  (2026-11-16)

## Corpus Check
- 633 files · ~212,253 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5759 nodes · 24723 edges · 269 communities (96 shown, 173 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2634 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `57e8d569`
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
- EvaluationHistoryQuery
- .admin
- .accounts
- Coach
- FinishSessionApiIT
- PlanLimitService
- RiderRepository
- org.junit.jupiter.api.Test
- InviteRepositoryIT
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- SessionUsageServiceIT
- AdminOverviewService.java
- Session.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- SlotBookedCount
- .record
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
- RiderRepositoryIT
- IntakeScenarioIT
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- ExpiredSessionsReportIT
- AuthUserConverter.java
- Release Workflow
- .issue
- EndpointIsolationIT
- FreshDatabase.java
- .of
- SessionRepository
- AdminMemberApiIT
- .runAs
- uuid
- .hash
- RiderAppQueryService
- AssignOnTheDayApiIT
- MySessionsControllerTest
- SubscriptionService.java
- jakarta.persistence.AttributeConverter
- AssessmentBooking
- ClubRepositoryIT
- LevelChangeFlowIT
- TenantIsolationIT
- Payment
- AdminRiderApiIT
- ClubAdminService
- ClubAccessStateIT
- Prod Environment Check
- Plan
- MailTestConfig.java
- SessionTemplateControllerTest
- AuthService
- .current
- AdminTrainerApiIT.java
- AdminBookingRow
- CI Workflow
- notblank
- PaymentsReportIT
- EvaluationFollowUpQuery
- MigrationFromRelease2IT
- TrainerRepositoryIT
- SessionTemplateController.java
- AccountLinkServiceIT
- SessionDetailServiceIT
- org.springframework.stereotype.Component
- XlsxReportWriter.java
- EvaluationFollowUpApiIT
- Builder
- CancelReason
- AuthService.java
- AuthProperties
- MyWeekApiIT
- PlanLimitApiIT
- org.junit.jupiter.params.ParameterizedTest
- SessionTemplateService
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- TokenGenerator
- .reset
- .sessions
- SessionTemplateRulesIT
- AdminPaymentApiIT
- InvitePurpose
- SeedDataIT
- com.equestrian.equis.entity.riders.Rider
- AssessmentSlotControllerTest
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- Session
- Docker Compose
- .convert
- AdminAccountRepositoryIT
- RiderTest
- AdminMemberEditApiIT
- org.springframework.transaction.annotation.Transactional
- SessionTemplateRepositoryIT
- E19DemoIT
- TenantAutoConfiguration.java
- SubscriptionExpiredException
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
- NoticeRepositoryIT
- .stateOf
- LevelChangeApiIT
- PaymentsReport
- MyRidersControllerTest
- Graphify Rules Docs
- SubjectType
- Release 2.0.1
- Maven POM
- .status
- TrainerRef
- AccountLinkMailListenerIT.java
- E17DemoIT
- AuthProperties
- AccountLinkEmails
- ReportDefinition
- TenantTransactions
- TwoClubFixture
- SessionEvaluationRepositoryIT
- TrainerRow
- ProdDemoDataCleanupIT
- BookingStatus
- TemplateBookedSeats
- org.flywaydb.core.Flyway
- ReportColumn
- StartMode
- .summaries
- Invite
- ReportExportIT
- TrainersReport
- Snapshot
- ReportNotFoundException
- ReportParams
- SmtpNotificationSender.java
- org.springframework.test.web.servlet.ResultActions
- SecurityConfig.java
- Override
- Override
- ReportRangeTooLongException
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

## Communities (269 total, 173 thin omitted)

### Community 0 - "SheetWriter"
Cohesion: 0.12
Nodes (8): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, SheetWriter

### Community 2 - "assertthat"
Cohesion: 0.08
Nodes (14): AssessmentBookingRepositoryIT, AssignmentRepositoryIT, AssessmentRulesIT, FixedClock, RecordResultServiceIT, FixedClock, FixedClock, FixedClock (+6 more)

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.16
Nodes (3): BookingReminderApiIT, NoticeRow, Request

### Community 6 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.10
Nodes (5): AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture, Coach

### Community 7 - "EvaluationHistoryQuery"
Cohesion: 0.15
Nodes (8): MyRidersController, RiderEvaluations, EvaluationHistoryRow, EvaluationHistoryQuery, LevelChangeRules, MyRidersQueryService, RiderLevelQueryService, RiderSeatQuery

### Community 8 - ".admin"
Cohesion: 0.05
Nodes (9): OutgoingEmail, Request, TrainerAdminAccessIT, TrainerDisableJourneyIT, Request, TrainerInviteJourneyIT, TrainerResetJourneyIT, RiderAccountJourneyIT (+1 more)

### Community 9 - ".accounts"
Cohesion: 0.18
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.12
Nodes (5): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Check, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.14
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.13
Nodes (6): Count, LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException, PlanLimitService

### Community 15 - "RiderRepository"
Cohesion: 0.11
Nodes (19): AdminMemberController, BookingController, AdminAccountRepository, InviteRepository, TrainerRepository, AssessmentBookingRepository, MemberRepository, NoticeRepository (+11 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (12): AuthPropertiesTest, TestConfig, JwtConfigTest, MailConfigTest, AdminRiderBalanceApiIT, CancelReasonTest, MemberStatusTest, PaymentEnumsTest (+4 more)

### Community 24 - "SessionUsageServiceIT"
Cohesion: 0.08
Nodes (11): PaymentEntry, PaymentResult, PaymentUsage, RiderPayments, VoidPaymentRequest, SessionUsageRepository, UsageLine, AdminPaymentService (+3 more)

### Community 25 - "AdminOverviewService.java"
Cohesion: 0.08
Nodes (15): SchoolWeek, AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill (+7 more)

### Community 26 - "Session.java"
Cohesion: 0.19
Nodes (6): TenantGuard, AdminAccount, RefreshToken, SuperAdmin, PaymentReportRow, RiderReportRow

### Community 27 - "Repository Layer"
Cohesion: 0.10
Nodes (3): RefreshTokenRepository, RiderSessionRow, MemberRider

### Community 30 - ".record"
Cohesion: 0.08
Nodes (3): RecordResultRequest, ResultRecorded, AssessmentBookingTest

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.18
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceIT

### Community 35 - ".login"
Cohesion: 0.14
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "AuthUser"
Cohesion: 0.07
Nodes (24): AuthUser, AdminAssessmentSlotController, AssessmentSlotController, AdminClubUsageController, MyEvaluationsController, MyLevelsController, AdminPaymentController, PlatformAuthController (+16 more)

### Community 37 - "RiderBalancesReportTest.java"
Cohesion: 0.10
Nodes (4): RiderReportRepository, RiderBalanceReportRow, RiderBalancesReport, RiderBalancesReportTest

### Community 43 - "TestPostgres"
Cohesion: 0.20
Nodes (3): ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT

### Community 44 - "AdminBookingChangeApiIT"
Cohesion: 0.07
Nodes (13): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+5 more)

### Community 45 - "LevelChangeServiceIT"
Cohesion: 0.05
Nodes (7): ApiExceptionHandler, Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest, LevelChangeServiceIT

### Community 48 - "IntakeScenarioIT"
Cohesion: 0.15
Nodes (4): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.21
Nodes (3): BalanceDto, MyRiderDto, MyRidersQueryServiceIT

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "EndpointIsolationIT"
Cohesion: 0.18
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 59 - ".of"
Cohesion: 0.07
Nodes (3): SessionTest, SessionRepositoryIT, AssignmentServiceIT

### Community 61 - "SessionRepository"
Cohesion: 0.09
Nodes (16): MySessionsController, EvaluationRequest, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse, SessionEvaluationRepository, LevelRepository (+8 more)

### Community 63 - "AdminMemberApiIT"
Cohesion: 0.07
Nodes (4): AdminMemberApiIT, AdminMemberCloseApiIT, ClubThreads, DefaultClubExtension

### Community 64 - ".runAs"
Cohesion: 0.08
Nodes (4): TenantContext, PerClubSignInIT, RepositoryIsolationIT, ClubData

### Community 67 - "RiderAppQueryService"
Cohesion: 0.08
Nodes (12): RiderAppController, ExpiringSoon, RiderAssessment, RiderBanners, RiderMe, RiderPayments, RiderProgress, LevelHistoryEntry (+4 more)

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.14
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 70 - "SubscriptionService.java"
Cohesion: 0.10
Nodes (11): SubscriptionResult, SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED, SubscriptionView (+3 more)

### Community 71 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.08
Nodes (16): RiderPayment, DbConverter, DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET (+8 more)

### Community 72 - "AssessmentBooking"
Cohesion: 0.08
Nodes (7): AssessmentBooking, Club, ClubRepository, IssuedLink, BookingCancellationListener, AssignmentCancellationListener, SessionGenerationJob

### Community 76 - "Payment"
Cohesion: 0.10
Nodes (10): Payment, SessionUsage, PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED (+2 more)

### Community 78 - "ClubAdminService"
Cohesion: 0.11
Nodes (10): ClubPage, ClubAdminService, Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME (+2 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plan"
Cohesion: 0.16
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 84 - "SessionTemplateControllerTest"
Cohesion: 0.13
Nodes (4): OverlapRef, SaveResult, Warning, SessionTemplateControllerTest

### Community 85 - "AuthService"
Cohesion: 0.10
Nodes (8): Access, AuthTokens, ClubRef, ClubStatusView, ClubUsageView, PlatformLoginRequest, AuthService, BlockedSignIn

### Community 86 - ".current"
Cohesion: 0.11
Nodes (5): TenantIdentifierResolver, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, PlatformPingController

### Community 88 - "AdminBookingRow"
Cohesion: 0.05
Nodes (17): AdminBookingController, AdminBookingList, AdminBookingRow, AdminNewMemberRequest, BookingTotals, CancelBookingRequest, ChangeBookingRequest, MemberRef (+9 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.19
Nodes (3): PhoneNormalizer, ClubAdminInput, CreateClubRequest

### Community 92 - "EvaluationFollowUpQuery"
Cohesion: 0.19
Nodes (10): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, EvaluationFollowUpQuery (+2 more)

### Community 95 - "SessionTemplateController.java"
Cohesion: 0.17
Nodes (9): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+1 more)

### Community 98 - "org.springframework.stereotype.Component"
Cohesion: 0.17
Nodes (4): ExpiredPackageReportRow, ExpiredPackageReportRepository, ExpiredSessionsReport, ExpiredSessionsReportTest

### Community 102 - "CancelReason"
Cohesion: 0.08
Nodes (16): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+8 more)

### Community 103 - "AuthService.java"
Cohesion: 0.13
Nodes (5): AuthController, RefreshRequest, AuthTokenService, Issued, AuthTokenServiceIT

### Community 109 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.09
Nodes (3): PhoneNormalizerTest, LevelRepositoryIT, LevelChangeRepositoryIT

### Community 110 - "SessionTemplateService"
Cohesion: 0.17
Nodes (6): SessionProperties, SessionTemplateRepository, BookedSeats, SessionGenerator, SessionTemplateRules, SessionTemplateService

### Community 113 - "TokenGenerator"
Cohesion: 0.20
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 115 - ".sessions"
Cohesion: 0.17
Nodes (8): SessionDetailMapper, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, Assignment, SessionSeats

### Community 118 - "InvitePurpose"
Cohesion: 0.15
Nodes (6): DbConverter, InvitePurpose, INVITE, RESET, AccountLinkCreatedEvent, AccountLinkEmailsTest

### Community 120 - "com.equestrian.equis.entity.riders.Rider"
Cohesion: 0.12
Nodes (4): FixedClock, MyLevelsServiceIT, FixedClock, RiderLevelQueryServiceIT

### Community 121 - "AssessmentSlotControllerTest"
Cohesion: 0.14
Nodes (7): AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, AssessmentSlotMapper, AssessmentSlotControllerTest

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.20
Nodes (4): ClockConfig, MailConfig, SchedulingConfig, SecurityBeans

### Community 126 - "AdminLockoutServiceIT"
Cohesion: 0.13
Nodes (6): LockableAccount, SuperAdminRepository, AdminLockoutService, PasswordService, PlatformAuthService, AdminLockoutServiceIT

### Community 127 - "Session"
Cohesion: 0.09
Nodes (8): SeatEvaluation, SessionEvaluation, Session, SessionTemplate, AssignmentRules, SessionAccess, SessionDetail, SessionEvaluationTest

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "RiderTest"
Cohesion: 0.09
Nodes (7): Trainer, Level, LevelChange, Rider, NoSessionsLeftException, LevelChangeTest, RiderTest

### Community 133 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.06
Nodes (25): ApiException, ApiFieldError, Constraints, ErrorCodes, DateRanges, RiderSession, RiderSessionEvaluation, RiderSessionPage (+17 more)

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 141 - "RiderAccountStatus"
Cohesion: 0.17
Nodes (6): RiderAccount, DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 142 - "Base URLs & Accounts Docs"
Cohesion: 0.29
Nodes (8): APP_RIDER_BASE_URL, E15 Rider accounts and data, E1 Auth & accounts, E6 Trainer accounts, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (smtp|log), Production environment variables

### Community 143 - "Ops Scripts & Plans Docs"
Cohesion: 0.25
Nodes (8): V5__seed_admin.sql, create-super-admin.sql, on_expiry policy (BLOCK | READ_ONLY), Ops scripts (run by hand), Plans TRIAL / BASIC / PRO / MAX, POST /api/platform/auth/login, upsert-plan.sql, APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH

### Community 144 - "PaymentRepositoryIT"
Cohesion: 0.06
Nodes (8): RecordPaymentRequest, RiderPackage, StarTrendPoint, UsedCount, PackageBalance, PaymentRepositoryIT, FixedClock, RiderBalanceServiceIT

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

### Community 154 - "NoticeRepositoryIT"
Cohesion: 0.13
Nodes (6): DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED, RefTime, NoticeRepositoryIT

### Community 155 - ".stateOf"
Cohesion: 0.25
Nodes (3): ClubAccess, ClubAccessService, ClubAccessServiceTest

### Community 159 - "PaymentsReport"
Cohesion: 0.20
Nodes (3): PaymentReportRepository, PaymentsReport, PaymentsReportTest

### Community 162 - "SubjectType"
Cohesion: 0.12
Nodes (12): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, DbConverter, SubjectType, ADMIN (+4 more)

### Community 206 - "TrainerRef"
Cohesion: 0.09
Nodes (17): AdminRiderController, TrainerRef, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard (+9 more)

### Community 209 - "AuthProperties"
Cohesion: 0.19
Nodes (7): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset

### Community 212 - "AccountLinkEmails"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 213 - "ReportDefinition"
Cohesion: 0.12
Nodes (8): ReportController, ReportDefinition, ReportExportService, ReportParams, ReportRegistry, ReportWriter, XlsxReportWriter, ReportParamsTest

### Community 214 - "TenantTransactions"
Cohesion: 0.25
Nodes (3): Outcome, TenantTransactions, ClubOnboardingService

### Community 215 - "TwoClubFixture"
Cohesion: 0.20
Nodes (3): RowLevelSecurityIT, MutableClock, TwoClubFixture

### Community 217 - "TrainerRow"
Cohesion: 0.06
Nodes (10): AdminTrainerController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals (+2 more)

### Community 220 - "BookingStatus"
Cohesion: 0.25
Nodes (6): BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING, DbConverter

### Community 247 - "TemplateBookedSeats"
Cohesion: 0.27
Nodes (3): BookingSeatCounts, AssessmentSeatCounts, TemplateBookedSeats

### Community 250 - "StartMode"
Cohesion: 0.50
Nodes (3): StartMode, AFTER_CURRENT, NOW

### Community 251 - ".summaries"
Cohesion: 0.16
Nodes (3): MyLevelSummary, LevelSeatCount, LevelSessionCount

### Community 254 - "TrainersReport"
Cohesion: 0.26
Nodes (3): TrainerReportRow, TrainerReportRepository, TrainersReport

### Community 258 - "SmtpNotificationSender.java"
Cohesion: 0.11
Nodes (6): MailSettings, Mode, LOG, SMTP, LogNotificationSender, SmtpNotificationSender

### Community 261 - "SecurityConfig.java"
Cohesion: 0.05
Nodes (7): SecurityConfig, ClubAccessFilter, TenantFilter, ClubStatusController, ClubAccessFilterTest, EndpointCoverageGuardIT, Config

### Community 263 - "Override"
Cohesion: 0.13
Nodes (4): ReportTotal, Named, ReportRegistryTest, Totalled

### Community 268 - "XlsxReportWriterTest"
Cohesion: 0.13
Nodes (4): AllTypesReport, Line, XlsxReportWriterTest, AllTypesReport

### Community 274 - "AccessReason"
Cohesion: 0.08
Nodes (25): AddSubscriptionRequest, ChangeClubStatusRequest, ClubAccessView, Admin, ClubDetail, ClubRow, AdminStatus, ACTIVE (+17 more)

## Knowledge Gaps
- **122 isolated node(s):** `DATE`, `DATETIME`, `INTEGER`, `MONEY_EGP`, `TEXT` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 513 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **173 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `org.springframework.transaction.annotation.Transactional` to `assertthat`, `RiderTest`, `EvaluationHistoryQuery`, `RiderAccountStatus`, `RiderRepository`, `.status`, `SessionUsageServiceIT`, `.record`, `AuthUser`, `LevelChangeServiceIT`, `TrainerAccessServiceIT`, `.of`, `SessionRepository`, `LevelChangeRulesTest.java`, `.hash`, `RiderAppQueryService`, `MySessionsControllerTest`, `SubscriptionService.java`, `ClubAdminService`, `TrainerRef`, `AuthService`, `TenantTransactions`, `TwoClubFixture`, `AdminBookingRow`, `TrainerRow`, `EvaluationFollowUpQuery`, `SessionTemplateController.java`, `AccountLinkServiceIT`, `SessionTemplateService`, `.sessions`, `SessionTemplateRulesIT`, `AdminLockoutServiceIT`, `Session`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **What connects `DATE`, `DATETIME`, `INTEGER` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SheetWriter` be split into smaller, more focused modules?**
  _Cohesion score 0.11596638655462185 - nodes in this community are weakly interconnected._
- **Why does `Session` connect `Session` to `assertthat`, `RiderTest`, `org.springframework.transaction.annotation.Transactional`, `RiderRepository`, `PaymentRepositoryIT`, `SessionUsageServiceIT`, `AdminOverviewService.java`, `Session.java`, `Repository Layer`, `.record`, `MyRidersControllerTest`, `LevelChangeServiceIT`, `.of`, `SessionRepository`, `MySessionsControllerTest`, `jakarta.persistence.AttributeConverter`, `AssessmentBooking`, `Payment`, `AdminBookingRow`, `EvaluationFollowUpQuery`, `XlsxReportWriter.java`, `SessionTemplateService`, `.sessions`, `AssessmentSlotControllerTest`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **Should `org.junit.jupiter.api.BeforeEach` be split into smaller, more focused modules?**
  _Cohesion score 0.09657594381035997 - nodes in this community are weakly interconnected._
- **Why does `RiderBalancesReportIT` connect `RiderBalancesReportIT` to `org.springframework.jdbc.core.JdbcTemplate`, `org.springframework.test.web.servlet.ResultActions`, `RiderBalancesReportTest.java`?**
  _High betweenness centrality (0.013) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.07959611854183057 - nodes in this community are weakly interconnected._