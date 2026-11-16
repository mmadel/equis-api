# Graph Report - equis-api  (2026-11-16)

## Corpus Check
- 617 files · ~204,749 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5595 nodes · 24034 edges · 265 communities (82 shown, 183 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2605 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c6dc8228`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SheetWriter
- assertthat
- .status
- AdminTrainerApiIT
- BookingReminderApiIT
- AdminOversightFixture
- Rider
- RiderAccountJourneyIT
- .admin
- Coach
- IntegrationTestBase
- FinishSessionApiIT
- PlanLimitService
- .of
- org.junit.jupiter.api.Test
- TrainerStatus
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- org.springframework.stereotype.Service
- Session
- Session.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- .recorder
- AssessmentBookingRepositoryIT
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- AuthServiceIT
- AuthUser
- SessionTemplateRulesIT
- LevelRepositoryIT
- PlanAndSubscriptionIT
- org.springframework.jdbc.core.JdbcTemplate
- AdminBookingChangeApiIT
- LevelChangeFlowIT
- TrainerAccessServiceIT
- LevelRiderCount
- IntakeScenarioIT
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- AuthUserConverterTest.java
- Release Workflow
- .issue
- EndpointIsolationIT
- RepositoryIsolationIT.java
- AssignmentServiceIT
- API Exception Handler
- InviteRepositoryIT
- AdminMemberCloseApiIT
- .runAs
- localdate
- PasswordService
- .status
- AssignOnTheDayApiIT
- TokenGenerator
- SubscriptionService.java
- TenantTransactions
- ClubAccessService
- ClubRepositoryIT
- Booking Actions
- TenantIsolationIT
- PaymentState
- AdminMemberApiIT
- ClubAdminService
- ClubAccessStateIT
- Prod Environment Check
- Plan
- SessionUsageServiceIT
- SessionTemplate
- PerClubSignInIT
- .register
- AdminTrainerApiIT.java
- AssessmentBookingRepository
- CI Workflow
- notblank
- PaymentsReportIT
- EvaluationFollowUpQuery
- .hash
- TrainerRepositoryIT
- LevelChangeServiceIT
- AccountLinkServiceIT
- .of
- PaymentsReport
- XlsxReportWriter.java
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthProperties
- AssessmentSlotControllerTest
- MyWeekApiIT
- PlanLimitApiIT
- .allowed
- SessionRepository
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- RiderBalanceServiceIT
- .reset
- SessionTemplateController.java
- .admin
- AdminPaymentApiIT
- InvitePurpose
- TrainerDisableJourneyIT
- RiderLevelQueryServiceIT
- SecurityConfig.java
- SmtpNotificationSenderTest.java
- .book
- JwtServiceTest.java
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- RepositoryIsolationIT
- Docker Compose
- PaymentMethod
- AdminAccountRepositoryIT
- RiderTest
- AdminMemberEditApiIT
- list
- SessionTemplateRepositoryIT
- .convert
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
- Changelog Epics A
- .set
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
- BookingControllerTest
- AdminNewMemberRequest.java
- LevelChangeApiIT
- ReportParams
- LevelChangeRepositoryIT
- Graphify Rules Docs
- SubjectType
- Release 2.0.1
- Maven POM
- MySessionsControllerTest
- jakarta.persistence.AttributeConverter
- TenantIdentifierResolver
- org.springframework.test.web.servlet.ResultActions
- SessionGenerationJob.java
- EndpointCoverageGuardIT
- ReportDefinition
- OutgoingEmail
- MailConfig
- SessionEvaluationRepositoryIT
- Trainer
- TemplateBookedSeats
- CountingEncoder
- AssignmentRepositoryIT
- AccountLinkMailListenerIT.java
- ReportColumn
- org.springframework.context.annotation.Bean
- .aSnapshotSaysAsOf
- ReportNotFoundException
- ReportExportIT
- TrainersReport
- AccountLinkEmails
- ReportRangeTooLongException
- ReportParamsFactoryTest
- AuthProperties
- TrainerAccountsFixture
- ClubAccessFilter.java
- Override
- XlsxReportWriterTest
- SubscriptionService.java
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

## Communities (265 total, 183 thin omitted)

### Community 0 - "SheetWriter"
Cohesion: 0.12
Nodes (8): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, SheetWriter

### Community 2 - "assertthat"
Cohesion: 0.07
Nodes (8): PhoneNormalizerTest, ClubAccessFilterTest, MemberRepositoryIT, AssessmentRulesIT, FixedClock, RecordResultServiceIT, Config, PostgresContainer

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.16
Nodes (3): BookingReminderApiIT, NoticeRow, Request

### Community 6 - "AdminOversightFixture"
Cohesion: 0.11
Nodes (5): AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture, Coach

### Community 7 - "Rider"
Cohesion: 0.06
Nodes (15): BookingController, AdminRiderAccountController, RiderEvaluations, RiderAccount, Rider, RiderRepository, RecordResultService, EvaluationHistoryQuery (+7 more)

### Community 9 - ".admin"
Cohesion: 0.15
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.12
Nodes (5): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Check, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.14
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.11
Nodes (9): AdminClubUsageController, ClubStatusView, ClubUsageView, Count, LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException (+1 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (10): AuthPropertiesTest, TestConfig, JwtConfigTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, EquisApiApplicationIT, RefreshTokenRepositoryIT (+2 more)

### Community 17 - "TrainerStatus"
Cohesion: 0.08
Nodes (21): TrainerRef, ActivityItem, AdminRiderRow, RiderTotals, TrainerCard, TrainerStatus, ACTIVE, DISABLED (+13 more)

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.10
Nodes (4): MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess, Config

### Community 24 - "org.springframework.stereotype.Service"
Cohesion: 0.04
Nodes (37): RiderAppController, PaymentEntry, PaymentResult, PaymentUsage, RiderPayments, VoidPaymentRequest, ExpiringSoon, RiderAssessment (+29 more)

### Community 25 - "Session"
Cohesion: 0.06
Nodes (13): AdminOverviewController, AdminNewMemberRequest, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill, WeekFill (+5 more)

### Community 26 - "Session.java"
Cohesion: 0.05
Nodes (23): TenantGuard, Admin, Direction, DOWN, UP, LevelChangeRequest, AdminAccount, AdminStatus (+15 more)

### Community 27 - "Repository Layer"
Cohesion: 0.09
Nodes (5): TrainerRiderCount, InviteRepository, RefreshTokenRepository, SlotBookedCount, SuperAdminRepository

### Community 29 - ".recorder"
Cohesion: 0.12
Nodes (3): Request, Request, TrainerInviteJourneyIT

### Community 30 - "AssessmentBookingRepositoryIT"
Cohesion: 0.07
Nodes (4): RecordResultRequest, ResultRecorded, AssessmentBookingTest, AssessmentBookingRepositoryIT

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.17
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceIT

### Community 35 - "AuthServiceIT"
Cohesion: 0.06
Nodes (15): ClubStatusController, Access, AuthTokens, ClubRef, InviteAcceptRequest, LoginRequest, AuthService, BlockedSignIn (+7 more)

### Community 36 - "AuthUser"
Cohesion: 0.04
Nodes (43): AuthUser, AuthController, AdminAssessmentSlotController, AdminMemberController, AssessmentSlotController, MyEvaluationsController, MyLevelsController, AdminPaymentController (+35 more)

### Community 43 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.06
Nodes (8): ProdDemoDataCleanupIT, ApplicantIntakeMigrationIT, EmptyDatabaseStartIT, MigrationFromRelease2IT, RiderAccountMigrationIT, SessionFinishedMigrationIT, FreshDatabase, TestPostgres

### Community 45 - "LevelChangeFlowIT"
Cohesion: 0.10
Nodes (4): MyLevelsServiceIT, LevelChangeFlowIT, FixedClock, SessionDetailServiceIT

### Community 48 - "IntakeScenarioIT"
Cohesion: 0.14
Nodes (4): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person

### Community 50 - "RiderReassignApiIT"
Cohesion: 0.08
Nodes (4): AdminRiderApiIT, Request, Request, RiderReassignApiIT

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.21
Nodes (4): BalanceDto, MyRiderDto, UpcomingSeat, MyRidersQueryServiceIT

### Community 54 - "AuthUserConverterTest.java"
Cohesion: 0.13
Nodes (7): AuthUserAuthentication, AuthUserConverter, Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 56 - ".issue"
Cohesion: 0.13
Nodes (3): AccessToken, RiderAppApiIT, JwtServiceTest

### Community 57 - "EndpointIsolationIT"
Cohesion: 0.18
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 64 - ".runAs"
Cohesion: 0.13
Nodes (5): TenantContext, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, PlatformPingController

### Community 65 - "localdate"
Cohesion: 0.09
Nodes (4): RiderEvaluation, SeatEvaluation, EvaluationHistoryRow, PendingEvaluationRow

### Community 66 - "PasswordService"
Cohesion: 0.22
Nodes (5): PlatformAuthController, PlatformLoginRequest, AdminLockoutService, PasswordService, PlatformAuthService

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 70 - "SubscriptionService.java"
Cohesion: 0.12
Nodes (5): PlatformSubscriptionController, SubscriptionResult, Subscription, SubscriptionRepository, SubscriptionService

### Community 71 - "TenantTransactions"
Cohesion: 0.10
Nodes (5): Outcome, TenantTransactions, ClubRepository, ClubErrors, ClubOnboardingService

### Community 72 - "ClubAccessService"
Cohesion: 0.29
Nodes (3): ClubAccess, ClubAccessService, ClubAccessServiceTest

### Community 74 - "Booking Actions"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 76 - "PaymentState"
Cohesion: 0.22
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 78 - "ClubAdminService"
Cohesion: 0.12
Nodes (10): ClubPage, ClubAdminService, Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME (+2 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plan"
Cohesion: 0.12
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 84 - "SessionTemplate"
Cohesion: 0.17
Nodes (7): SessionTemplateController, SessionTemplate, SessionTemplateRepository, BookedSeats, SessionGenerator, SessionTemplateRules, SessionTemplateService

### Community 88 - "AssessmentBookingRepository"
Cohesion: 0.03
Nodes (34): AdminBookingController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest, ChangeBookingRequest, MemberRef, SlotRef (+26 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.15
Nodes (3): ClubAdminInput, CreateClubRequest, VoidSubscriptionRequest

### Community 92 - "EvaluationFollowUpQuery"
Cohesion: 0.16
Nodes (15): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, SessionStatus (+7 more)

### Community 98 - "PaymentsReport"
Cohesion: 0.14
Nodes (4): PaymentReportRepository, PaymentsReport, ReportTotal, PaymentsReportTest

### Community 102 - "Member Status"
Cohesion: 0.13
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 106 - "AssessmentSlotControllerTest"
Cohesion: 0.20
Nodes (5): AssessmentSlotDayDto, AssessmentSlotDto, BookingRowDto, SlotDetailDto, AssessmentSlotControllerTest

### Community 110 - "SessionRepository"
Cohesion: 0.04
Nodes (26): SchoolWeek, SessionDetailMapper, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyWeek (+18 more)

### Community 115 - "SessionTemplateController.java"
Cohesion: 0.17
Nodes (10): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse, SessionTemplateUpdateRequest (+2 more)

### Community 118 - "InvitePurpose"
Cohesion: 0.17
Nodes (6): DbConverter, InvitePurpose, INVITE, RESET, AccountLinkCreatedEvent, AccountLinkEmailsTest

### Community 122 - "SmtpNotificationSenderTest.java"
Cohesion: 0.09
Nodes (4): LogNotificationSender, SmtpNotificationSender, MailConfigTest, SmtpNotificationSenderTest

### Community 123 - ".book"
Cohesion: 0.23
Nodes (3): FixedClock, MemberBookingServiceIT, TestMembers

### Community 124 - "JwtServiceTest.java"
Cohesion: 0.22
Nodes (3): ClockConfig, SchedulingConfig, SecurityBeans

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 129 - "PaymentMethod"
Cohesion: 0.27
Nodes (7): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 133 - "list"
Cohesion: 0.09
Nodes (8): ApiException, ApiFieldError, ErrorCodes, DateRanges, Levels, AccountErrors, TrainerErrors, SessionSeatAssembler

### Community 136 - "TenantAutoConfiguration.java"
Cohesion: 0.13
Nodes (3): TenantAutoConfiguration, BeanPostProcessor, TenantAwareDataSource

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 141 - "CancelReason"
Cohesion: 0.20
Nodes (8): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER

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

### Community 162 - "SubjectType"
Cohesion: 0.17
Nodes (7): DbConverter, SubjectType, ADMIN, RIDER, SUPER_ADMIN, TRAINER, RoleTest

### Community 206 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.09
Nodes (10): DbConverter, DbConverter, DbConverter, DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED, DbConverter (+2 more)

### Community 208 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.13
Nodes (3): E17DemoIT, E18DemoIT, TestSubscriptions

### Community 213 - "ReportDefinition"
Cohesion: 0.15
Nodes (5): ReportController, ReportDefinition, ReportExportService, ReportRegistry, ReportWriter

### Community 217 - "Trainer"
Cohesion: 0.05
Nodes (20): Constraints, AdminTrainerController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow (+12 more)

### Community 250 - "org.springframework.context.annotation.Bean"
Cohesion: 0.07
Nodes (12): FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, FixedClock, FixedClock (+4 more)

### Community 255 - "AccountLinkEmails"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 258 - "AuthProperties"
Cohesion: 0.15
Nodes (12): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, MailSettings (+4 more)

### Community 260 - "TrainerAccountsFixture"
Cohesion: 0.08
Nodes (8): TrainerAdminAccessIT, TrainerResetJourneyIT, MutableClock, RecordingNotificationSender, TrainerAccountsFixture, ClubData, Config, TwoClubFixture

### Community 261 - "ClubAccessFilter.java"
Cohesion: 0.13
Nodes (3): ClubAccessFilter, TenantFilter, SubscriptionExpiredException

### Community 263 - "Override"
Cohesion: 0.18
Nodes (3): Named, ReportRegistryTest, Totalled

### Community 268 - "XlsxReportWriterTest"
Cohesion: 0.16
Nodes (3): AllTypesReport, Line, XlsxReportWriterTest

### Community 274 - "SubscriptionService.java"
Cohesion: 0.08
Nodes (26): AddSubscriptionRequest, ClubAccessView, ClubRow, StartMode, AFTER_CURRENT, NOW, SubscriptionStatus, CURRENT (+18 more)

## Knowledge Gaps
- **122 isolated node(s):** `TEXT`, `DATE`, `DATETIME`, `MONEY_EGP`, `INTEGER` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 509 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **183 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Session` connect `Session` to `assertthat`, `list`, `Rider`, `.of`, `PaymentRepositoryIT`, `org.springframework.stereotype.Service`, `Session.java`, `Repository Layer`, `AssessmentBookingRepositoryIT`, `SessionTemplateServiceIT`, `AuthUser`, `MyRidersQueryServiceIT`, `AssignmentServiceIT`, `localdate`, `SessionUsageServiceIT`, `SessionTemplate`, `AssessmentBookingRepository`, `Trainer`, `EvaluationFollowUpQuery`, `.of`, `AssessmentSlotControllerTest`, `SessionRepository`, `AssignmentRepositoryIT`?**
  _High betweenness centrality (0.024) - this node is a cross-community bridge._
- **What connects `TEXT`, `DATE`, `DATETIME` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SheetWriter` be split into smaller, more focused modules?**
  _Cohesion score 0.11596638655462185 - nodes in this community are weakly interconnected._
- **Why does `ApiException` connect `list` to `assertthat`, `Rider`, `TrainerStatus`, `SubscriptionService.java`, `.status`, `org.springframework.stereotype.Service`, `Session`, `Session.java`, `AssessmentBookingRepositoryIT`, `AuthUser`, `SessionTemplateRulesIT`, `AssignmentServiceIT`, `API Exception Handler`, `PasswordService`, `SubscriptionService.java`, `TenantTransactions`, `ClubAdminService`, `SessionTemplate`, `AssessmentBookingRepository`, `Trainer`, `EvaluationFollowUpQuery`, `.hash`, `SessionRepository`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.08289738430583501 - nodes in this community are weakly interconnected._
- **Why does `RiderRepository` connect `Rider` to `LevelChangeRepositoryIT`, `AuthUser`, `list`, `.convert`, `TrainerAccessServiceIT`, `SessionRepository`, `org.junit.jupiter.api.Test`, `TrainerStatus`, `AuthUserConverterTest.java`, `AssessmentBookingRepository`, `Trainer`, `Repository Layer`, `org.springframework.stereotype.Service`?**
  _High betweenness centrality (0.012) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.07239117471675612 - nodes in this community are weakly interconnected._