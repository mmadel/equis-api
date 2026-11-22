# Graph Report - equis-api  (2026-11-22)

## Corpus Check
- 663 files · ~225,939 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5935 nodes · 25606 edges · 273 communities (90 shown, 183 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2765 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ebdb0f4f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SheetWriter
- org.junit.jupiter.api.BeforeEach
- com.equestrian.equis.repository.accounts.TrainerRepository
- .status
- AdminTrainerApiIT
- BookingReminderApiIT
- org.springframework.test.web.servlet.ResultActions
- org.springframework.stereotype.Component
- .admin
- .accounts
- Coach
- org.springframework.jdbc.core.JdbcTemplate
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
- Rider.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- TrainerAccountsFixture
- AssessmentBookingRepositoryIT
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- .login
- AuthUser
- RiderBalancesReport
- RiderBalancesReportIT
- PlanAndSubscriptionIT
- TestPostgres
- AdminBookingChangeApiIT
- ApiExceptionHandler.java
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
- BookingActions.java
- FreshDatabase.java
- LevelChangeApiIT
- .of
- PlatformAccountActivityIT
- AdminMemberApiIT
- RepositoryIsolationIT.java
- .resultOverdue
- AuthService
- RiderAppQueryService.java
- AssignOnTheDayApiIT
- MySessionsControllerTest
- SubscriptionService.java
- jakarta.persistence.AttributeConverter
- SessionTemplateRepository
- ClubRepositoryIT
- com.equestrian.equis.entity.riders.Rider
- TenantIsolationIT
- LevelChangeServiceIT
- SessionTemplate
- ClubAdminService
- ClubAccessStateIT
- Prod Environment Check
- Plan
- AdminRiderApiIT
- SessionTemplateControllerTest
- AuthTokens
- .runAsRoot
- AdminTrainerApiIT.java
- AdminBookingRow
- CI Workflow
- notblank
- PaymentsReportIT
- Payment
- LevelChangeRulesTest
- TrainerRepositoryIT
- SessionTemplateController.java
- AccountLinkServiceIT
- .register
- org.springframework.stereotype.Component
- SessionEvaluationsScenarioIT
- EvaluationFollowUpApiIT
- Builder
- CancelReason
- AuthTokenService
- AuthProperties
- AssignmentServiceIT
- MyWeekApiIT
- PlanLimitApiIT
- PostgresContainer
- PlatformAccountResetDemoModeIT
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- .status
- .sessions
- SessionTemplateRulesIT
- AdminPaymentApiIT
- InvitePurpose
- EndpointIsolationIT
- com.equestrian.equis.entity.riders.Rider
- AssessmentSlotControllerTest
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- SessionUsageServiceIT.java
- Docker Compose
- .convert
- AdminAccountRepositoryIT
- LevelChangeRepositoryIT
- AdminMemberEditApiIT
- list
- SessionTemplateRepositoryIT
- E19DemoIT
- TenantAutoConfiguration.java
- .asRoot
- Changelog Epics A
- .of
- TrainerDisableJourneyIT
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
- AccountLinkEmailsTest
- CancelReason
- .clear
- Graphify Rules Docs
- SubjectType
- Release 2.0.1
- Maven POM
- Invite
- TrainerRef
- AccountLinkMailListenerIT.java
- org.springframework.test.web.servlet.ResultActions
- AuthProperties
- org.slf4j.Logger
- ReportDefinition
- ClubDetail
- tools.jackson.databind.json.JsonMapper
- PaymentsReport
- Trainer
- ProdDemoDataCleanupIT.java
- MailConfig.java
- TenantTransactions
- LevelChangeRepositoryIT
- AssignmentRepositoryIT
- ReportColumn
- MyLevelsServiceIT
- RiderAccountService
- EmptyDatabaseStartIT.java
- ReportExportIT
- TrainersReport
- MigrationFromRelease2IT
- .allowed
- ReportParams
- SmtpNotificationSender.java
- SessionGenerationJob.java
- org.springframework.test.web.servlet.ResultActions
- SecurityConfig.java
- .slot
- Override
- NoticeType
- TrainerInviteJourneyIT
- SessionEvaluation
- XlsxReportWriterTest
- DbConverter
- BillingPeriod

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

## Communities (273 total, 183 thin omitted)

### Community 0 - "SheetWriter"
Cohesion: 0.09
Nodes (9): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, SheetWriter (+1 more)

### Community 2 - "com.equestrian.equis.repository.accounts.TrainerRepository"
Cohesion: 0.06
Nodes (20): TrainerTotals, TrainerAdminService, ReportNotFoundException, ReportRangeTooLongException, AssessmentRulesIT, FixedClock, RecordResultServiceIT, FixedClock (+12 more)

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.16
Nodes (3): BookingReminderApiIT, NoticeRow, Request

### Community 6 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.11
Nodes (5): AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture, Coach

### Community 7 - "org.springframework.stereotype.Component"
Cohesion: 0.06
Nodes (17): RiderEvaluations, LevelChanged, LevelHistoryEntry, RiderLevelDto, UpcomingSeat, Assignment, EvaluationHistoryRow, LevelChangeRepository (+9 more)

### Community 9 - ".accounts"
Cohesion: 0.16
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.20
Nodes (3): SameDaySessionsScenarioIT, Check, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.14
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlatformAccountActionServiceTest"
Cohesion: 0.15
Nodes (3): PlatformAccountReset, IssuedLink, PlatformAccountActionServiceTest

### Community 15 - "PlatformAccountActionServiceTest.java"
Cohesion: 0.16
Nodes (3): DemoModeAccounts, ClubOnboardingService, PlatformAccountActionService

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (11): JwtConfigTest, MailConfigTest, MailDemoModeConfigTest, AdminRiderBalanceApiIT, CancelReasonTest, MemberStatusTest, SessionEvaluationTest, PaymentEnumsTest (+3 more)

### Community 20 - "ErrorCodes.java"
Cohesion: 0.15
Nodes (4): BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess

### Community 25 - "Session"
Cohesion: 0.05
Nodes (18): AdminOverviewController, AssessmentSlotDto, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill (+10 more)

### Community 27 - "Repository Layer"
Cohesion: 0.09
Nodes (7): TrainerRiderCount, AdminAccountRepository, RefreshTokenRepository, SlotBookedCount, PlatformAccountActionRepository, SuperAdminRepository, MemberRider

### Community 29 - "TrainerAccountsFixture"
Cohesion: 0.12
Nodes (3): TrainerResetJourneyIT, RiderAccountJourneyIT, TrainerAccountsFixture

### Community 30 - "AssessmentBookingRepositoryIT"
Cohesion: 0.06
Nodes (4): RecordResultRequest, ResultRecorded, AssessmentBookingTest, AssessmentBookingRepositoryIT

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.16
Nodes (4): DeleteImpact, SessionsCancelledEvent, SessionTemplateService, SessionTemplateServiceIT

### Community 35 - ".login"
Cohesion: 0.13
Nodes (5): AuthController, InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "AuthUser"
Cohesion: 0.04
Nodes (45): AuthUser, AdminTrainerController, AdminAssessmentSlotController, BookingController, AdminClubUsageController, MyEvaluationsController, MyLevelsController, PlatformAuthController (+37 more)

### Community 37 - "RiderBalancesReport"
Cohesion: 0.15
Nodes (3): RiderBalanceReportRow, RiderBalancesReport, RiderBalancesReportTest

### Community 43 - "TestPostgres"
Cohesion: 0.16
Nodes (4): ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 48 - "IntakeScenarioIT"
Cohesion: 0.13
Nodes (5): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person, Schedule

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.22
Nodes (3): BalanceDto, MyRiderDto, MyRidersQueryServiceIT

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "BookingActions.java"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 63 - "AdminMemberApiIT"
Cohesion: 0.09
Nodes (3): AdminMemberApiIT, AdminMemberCloseApiIT, ClubThreads

### Community 64 - "RepositoryIsolationIT.java"
Cohesion: 0.10
Nodes (3): PerClubSignInIT, RepositoryIsolationIT, ClubData

### Community 66 - "AuthService"
Cohesion: 0.12
Nodes (5): AccountPasswords, AuthService, PasswordService, CountingEncoder, PasswordServiceTest

### Community 67 - "RiderAppQueryService.java"
Cohesion: 0.04
Nodes (37): AdminPaymentController, RiderAppController, PaymentEntry, PaymentResult, PaymentUsage, RecordPaymentRequest, RiderPayments, VoidPaymentRequest (+29 more)

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.11
Nodes (6): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED, SessionTest

### Community 70 - "SubscriptionService.java"
Cohesion: 0.09
Nodes (11): SubscriptionResult, SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED, SubscriptionView (+3 more)

### Community 71 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.13
Nodes (5): DbConverter, DbConverter, DbConverter, DayOfWeekConverter, DbConverter

### Community 72 - "SessionTemplateRepository"
Cohesion: 0.14
Nodes (7): SessionTemplateRepository, AssessmentSeatCounts, BookedSeats, SessionGenerator, SessionTemplateInput, SessionTemplateRules, TemplateBookedSeats

### Community 77 - "SessionTemplate"
Cohesion: 0.17
Nodes (9): RefreshToken, Club, Level, Notice, SuperAdmin, PaymentReportRow, RiderReportRow, TrainerReportRow (+1 more)

### Community 78 - "ClubAdminService"
Cohesion: 0.12
Nodes (9): ClubAdminService, Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME, ClubUsageService (+1 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plan"
Cohesion: 0.13
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 85 - "AuthTokens"
Cohesion: 0.12
Nodes (10): ClubStatusController, ClubStatusView, ClubUsageView, Count, BlockedSignIn, LimitedResource, RIDERS, TRAINERS (+2 more)

### Community 86 - ".runAsRoot"
Cohesion: 0.09
Nodes (5): TenantContext, TenantIdentifierResolver, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 88 - "AdminBookingRow"
Cohesion: 0.04
Nodes (32): AdminBookingController, AdminMemberController, AdminBookingList, AdminBookingRow, AdminNewMemberRequest, BookingTotals, CancelBookingRequest, ChangeBookingRequest (+24 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.14
Nodes (3): PhoneNormalizer, ClubAdminInput, CreateClubRequest

### Community 92 - "Payment"
Cohesion: 0.07
Nodes (16): RiderPayment, Payment, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER (+8 more)

### Community 93 - "LevelChangeRulesTest"
Cohesion: 0.13
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 95 - "SessionTemplateController.java"
Cohesion: 0.14
Nodes (11): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+3 more)

### Community 96 - "AccountLinkServiceIT"
Cohesion: 0.13
Nodes (5): AccountLinkService, GeneratedToken, TokenGenerator, AccountLinkServiceIT, TokenGeneratorTest

### Community 98 - "org.springframework.stereotype.Component"
Cohesion: 0.11
Nodes (4): ExpiredPackageReportRow, ExpiredPackageReportRepository, ExpiredSessionsReport, ExpiredSessionsReportTest

### Community 102 - "CancelReason"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "AuthTokenService"
Cohesion: 0.09
Nodes (8): Access, AuthTokens, ClubRef, RefreshRequest, PlatformLoginRequest, AuthTokenService, Issued, AuthTokenServiceIT

### Community 109 - "PostgresContainer"
Cohesion: 0.10
Nodes (6): PhoneNormalizerTest, ClubAccessFilterTest, MemberRepositoryIT, LevelRepositoryIT, NoticeRepositoryIT, PostgresContainer

### Community 115 - ".sessions"
Cohesion: 0.07
Nodes (14): SchoolWeek, SeatEvaluation, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyLevelSummary (+6 more)

### Community 118 - "InvitePurpose"
Cohesion: 0.18
Nodes (5): DbConverter, InvitePurpose, INVITE, RESET, AccountLinkCreatedEvent

### Community 119 - "EndpointIsolationIT"
Cohesion: 0.18
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 121 - "AssessmentSlotControllerTest"
Cohesion: 0.19
Nodes (4): AssessmentSlotController, AssessmentSlotDayDto, AssessmentSlotsDto, AssessmentSlotControllerTest

### Community 122 - "SmtpNotificationSenderTest.java"
Cohesion: 0.10
Nodes (3): AccessToken, JwtServiceTest, SmtpNotificationSenderTest

### Community 123 - ".book"
Cohesion: 0.23
Nodes (3): FixedClock, MemberBookingServiceIT, TestMembers

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.17
Nodes (5): ClockConfig, SchedulingConfig, SecurityBeans, AuthPropertiesTest, TestConfig

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 131 - "LevelChangeRepositoryIT"
Cohesion: 0.10
Nodes (5): LevelChange, Rider, NoSessionsLeftException, LevelChangeTest, RiderTest

### Community 133 - "list"
Cohesion: 0.06
Nodes (15): ApiException, ApiFieldError, Constraints, ErrorCodes, DateRanges, SessionStatus, CANCELLED, DONE (+7 more)

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 140 - ".of"
Cohesion: 0.11
Nodes (3): Chunks, Iterator<>, ChunksTest

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

### Community 158 - "AccountLinkEmailsTest"
Cohesion: 0.22
Nodes (3): AccountLinkEmails, Texts, AccountLinkEmailsTest

### Community 159 - "CancelReason"
Cohesion: 0.21
Nodes (9): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+1 more)

### Community 162 - "SubjectType"
Cohesion: 0.17
Nodes (7): DbConverter, SubjectType, ADMIN, RIDER, SUPER_ADMIN, TRAINER, RoleTest

### Community 205 - "Invite"
Cohesion: 0.15
Nodes (6): Admin, AdminAccount, AdminStatus, ACTIVE, INVITED, Invite

### Community 206 - "TrainerRef"
Cohesion: 0.06
Nodes (26): AdminRiderController, TrainerRef, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard (+18 more)

### Community 209 - "AuthProperties"
Cohesion: 0.21
Nodes (8): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, SessionProperties

### Community 212 - "org.slf4j.Logger"
Cohesion: 0.19
Nodes (3): AccountLinkMailListener, LogNotificationSender, NotificationSender

### Community 213 - "ReportDefinition"
Cohesion: 0.11
Nodes (6): ReportController, ReportDefinition, ReportExportService, ReportRegistry, ReportTotal, ReportWriter

### Community 214 - "ClubDetail"
Cohesion: 0.15
Nodes (12): PlatformClubController, ChangeClubStatusRequest, ClubDetail, ClubPage, ClubRow, ClubStatus, ACTIVE, SUSPENDED (+4 more)

### Community 216 - "PaymentsReport"
Cohesion: 0.20
Nodes (3): PaymentReportRepository, PaymentsReport, PaymentsReportTest

### Community 217 - "Trainer"
Cohesion: 0.05
Nodes (21): AdminEvaluationController, LevelCount, PendingLink, RiderCounts, TrainerRow, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer (+13 more)

### Community 221 - "TenantTransactions"
Cohesion: 0.15
Nodes (4): Outcome, TenantTransactions, ClubRepository, ClubAccessService

### Community 254 - "TrainersReport"
Cohesion: 0.21
Nodes (3): RiderReportRepository, TrainerReportRepository, TrainersReport

### Community 257 - "ReportParams"
Cohesion: 0.11
Nodes (5): ReportParams, ReportParamsFactory, ReportParamsFactoryTest, Snapshot, ReportParamsTest

### Community 258 - "SmtpNotificationSender.java"
Cohesion: 0.14
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 261 - "SecurityConfig.java"
Cohesion: 0.05
Nodes (9): SecurityConfig, Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, ClubAccessFilter, TenantFilter (+1 more)

### Community 263 - "Override"
Cohesion: 0.16
Nodes (3): Named, ReportRegistryTest, Totalled

### Community 264 - "NoticeType"
Cohesion: 0.33
Nodes (4): DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED

### Community 265 - "TrainerInviteJourneyIT"
Cohesion: 0.28
Nodes (3): Request, Request, TrainerInviteJourneyIT

### Community 268 - "XlsxReportWriterTest"
Cohesion: 0.13
Nodes (4): AllTypesReport, Line, XlsxReportWriterTest, AllTypesReport

### Community 274 - "BillingPeriod"
Cohesion: 0.16
Nodes (11): ClubAccessView, BillingPeriod, ANNUAL, MONTHLY, TRIAL, AccessReason, NO_SUBSCRIPTION, SUBSCRIPTION_ENDED (+3 more)

## Knowledge Gaps
- **122 isolated node(s):** `DATE`, `DATETIME`, `INTEGER`, `MONEY_EGP`, `TEXT` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 516 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **183 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Session` connect `Session` to `list`, `org.springframework.stereotype.Component`, `SessionEvaluation`, `org.springframework.jdbc.core.JdbcTemplate`, `org.junit.jupiter.api.Test`, `PaymentRepositoryIT`, `SessionUsageServiceIT`, `Rider.java`, `Repository Layer`, `AssessmentBookingRepositoryIT`, `SessionTemplateServiceIT`, `AuthUser`, `.of`, `RiderAppQueryService.java`, `AssignOnTheDayApiIT`, `SessionTemplate`, `AdminBookingRow`, `Trainer`, `AssignmentServiceIT`, `.sessions`, `AssignmentRepositoryIT`?**
  _High betweenness centrality (0.014) - this node is a cross-community bridge._
- **What connects `DATE`, `DATETIME`, `INTEGER` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SheetWriter` be split into smaller, more focused modules?**
  _Cohesion score 0.08748615725359911 - nodes in this community are weakly interconnected._
- **Why does `PaymentRepositoryIT` connect `PaymentRepositoryIT` to `com.equestrian.equis.repository.accounts.TrainerRepository`, `RiderAppQueryService.java`, `org.springframework.test.web.servlet.ResultActions`, `com.equestrian.equis.entity.riders.Rider`, `PostgresContainer`, `PlatformAccountActionServiceTest.java`, `Payment`, `PaymentsScenarioIT`, `SessionUsageServiceIT.java`?**
  _High betweenness centrality (0.013) - this node is a cross-community bridge._
- **Should `org.junit.jupiter.api.BeforeEach` be split into smaller, more focused modules?**
  _Cohesion score 0.08181393174380551 - nodes in this community are weakly interconnected._
- **Why does `ApiException` connect `list` to `com.equestrian.equis.repository.accounts.TrainerRepository`, `org.springframework.stereotype.Component`, `.status`, `Session`, `AssessmentBookingRepositoryIT`, `SessionTemplateServiceIT`, `AuthUser`, `ApiExceptionHandler.java`, `AuthService`, `RiderAppQueryService.java`, `SubscriptionService.java`, `SessionTemplateRepository`, `TrainerRef`, `Plan`, `ClubDetail`, `AdminBookingRow`, `Trainer`, `TenantTransactions`, `LevelChangeRulesTest`, `AccountLinkServiceIT`, `AuthTokenService`, `AssignmentServiceIT`, `.sessions`, `SessionTemplateRulesIT`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.012) - this node is a cross-community bridge._
- **Should `com.equestrian.equis.repository.accounts.TrainerRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.05634725634725635 - nodes in this community are weakly interconnected._