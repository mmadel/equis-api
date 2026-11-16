# Graph Report - equis-api  (2026-11-16)

## Corpus Check
- 626 files · ~207,727 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5682 nodes · 24387 edges · 258 communities (90 shown, 168 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2622 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b08696f4`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- SheetWriter
- IntegrationTestBase
- assertthat
- .status
- AdminTrainerApiIT
- BookingReminderApiIT
- .w3
- RiderRepository
- RiderAccountJourneyIT
- .accounts
- Coach
- FinishSessionApiIT
- PlanLimitService
- Session
- org.junit.jupiter.api.Test
- TrainerRef
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- RiderAppQueryService.java
- AdminOverviewService.java
- Session.java
- Repository Layer
- RiderEvaluationHistoryApiIT
- org.springframework.transaction.annotation.Transactional
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
- LevelChangeFlowIT
- TrainerAccessServiceIT
- RiderRepositoryIT
- IntakeScenarioIT
- AdminBookingApiIT
- RiderReassignApiIT
- MyRidersQueryServiceIT
- AdminOverviewApiIT
- AdminRiderApiIT
- .convert
- Release Workflow
- .issue
- RepositoryIsolationIT.java
- FreshDatabase.java
- AssignmentServiceIT
- API Exception Handler
- LevelChangeRulesTest.java
- AdminMemberCloseApiIT
- .runAs
- uuid
- TenantTransactions
- AssessmentRulesIT
- AssignOnTheDayApiIT
- AdminRiderService
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
- SubscriptionService.java
- SessionUsageServiceIT
- AssessmentBookingRepositoryIT
- PerClubSignInIT
- AssessmentRules
- AdminTrainerApiIT.java
- AdminBookingRow
- CI Workflow
- notblank
- PaymentsReportIT
- EvaluationFollowUpQuery
- com.equestrian.equis.service.platform.ClubAccessService
- TrainerRepositoryIT
- LevelChangeServiceIT
- AccountLinkServiceIT
- .of
- PaymentsReport
- XlsxReportWriter.java
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthService.java
- AuthProperties
- org.flywaydb.core.Flyway
- MyWeekApiIT
- PlanLimitApiIT
- org.junit.jupiter.params.ParameterizedTest
- SessionRepository
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- RiderBalanceServiceIT
- .reset
- SessionTemplateController.java
- E18DemoIT
- AdminPaymentApiIT
- InvitePurpose
- SeedDataIT
- RiderLevelQueryServiceIT
- PaymentTest
- SmtpNotificationSenderTest.java
- .book
- org.springframework.context.annotation.Configuration
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- .toResponse
- Docker Compose
- PaymentMethod
- AdminAccountRepositoryIT
- RiderTest
- AdminMemberEditApiIT
- ApiException
- SessionTemplateRepositoryIT
- InvitePurpose
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
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
- .record
- .countUsedByPayment
- LevelChangeApiIT
- LevelChangeRepositoryIT
- Graphify Rules Docs
- SubjectType
- Release 2.0.1
- Maven POM
- .status
- jakarta.persistence.AttributeConverter
- TenantIdentifierResolver
- E17DemoIT
- SessionTemplateRepository
- EndpointCoverageGuardIT
- ReportDefinition
- AssessmentSlotMapper
- Request
- SessionEvaluationRepositoryIT
- Trainer
- AssignmentRepositoryIT
- AccountLinkMailListenerIT.java
- ReportColumn
- ReportExportIT
- TrainersReport
- AccountLinkEmails
- ReportParams
- SmtpNotificationSender.java
- RiderBalancesReportIT.java
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

## Communities (258 total, 168 thin omitted)

### Community 0 - "SheetWriter"
Cohesion: 0.11
Nodes (9): ColumnType, DATE, DATETIME, INTEGER, MONEY_EGP, TEXT, ExcelStyles, SheetWriter (+1 more)

### Community 1 - "IntegrationTestBase"
Cohesion: 0.07
Nodes (4): TrainerAdminAccessIT, IntegrationTestBase, MutableClock, TrainerAccountsFixture

### Community 2 - "assertthat"
Cohesion: 0.06
Nodes (17): NoticeRepositoryIT, AuthServiceIT, FixedClock, FixedClock, MyLevelsServiceIT, FixedClock, FixedClock, FixedClock (+9 more)

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.18
Nodes (3): BookingReminderApiIT, NoticeRow, Request

### Community 7 - "RiderRepository"
Cohesion: 0.09
Nodes (20): AdminPaymentController, MyRidersController, RiderEvaluations, EvaluationHistoryRow, PendingEvaluationRow, SessionEvaluationRepository, PaymentRepository, SessionUsageRepository (+12 more)

### Community 9 - ".accounts"
Cohesion: 0.07
Nodes (10): OutgoingEmail, Request, Request, TrainerInviteJourneyIT, TrainerResetJourneyIT, OmarAndSara, Person, RiderAccountsScenarioIT (+2 more)

### Community 10 - "Coach"
Cohesion: 0.11
Nodes (5): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Coach, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.14
Nodes (5): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED

### Community 13 - "PlanLimitService"
Cohesion: 0.12
Nodes (11): AdminClubUsageController, Access, ClubStatusView, ClubUsageView, Count, BlockedSignIn, LimitedResource, RIDERS (+3 more)

### Community 15 - "Session"
Cohesion: 0.07
Nodes (10): MySessionsController, Session, SessionRepository, EvaluationService, AssignmentRules, AssignmentService, FinishSessionService, SessionAccess (+2 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (10): AccessToken, AuthPropertiesTest, TestConfig, JwtConfigTest, CancelReasonTest, MemberStatusTest, EquisApiApplicationIT, InviteRepositoryIT (+2 more)

### Community 17 - "TrainerRef"
Cohesion: 0.18
Nodes (8): TrainerRef, AdminRiderView, AdminRiderRowAssembler, HowAssigned, CHANGED_MANUALLY, FROM_ASSESSMENT, SET_MANUALLY, UNASSIGNED

### Community 19 - "AssessmentSlotQueryServiceIT"
Cohesion: 0.10
Nodes (8): AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, AssessmentSlotControllerTest, AssessmentSlotQueryServiceIT, FixedClock

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.11
Nodes (6): BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, SessionTemplateControllerTest, ActiveClubAccess, Config

### Community 24 - "RiderAppQueryService.java"
Cohesion: 0.09
Nodes (12): ExpiringSoon, RiderBanners, RiderMe, RiderPackage, RiderPayment, RiderPayments, RiderProgress, RiderSession (+4 more)

### Community 25 - "AdminOverviewService.java"
Cohesion: 0.09
Nodes (12): AdminOverviewController, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill, TrainerRiderCount (+4 more)

### Community 26 - "Session.java"
Cohesion: 0.10
Nodes (15): TenantGuard, SeatEvaluation, AdminAccount, RefreshToken, Trainer, Club, SessionEvaluation, Level (+7 more)

### Community 27 - "Repository Layer"
Cohesion: 0.10
Nodes (4): InviteRepository, SubscriptionRepository, LevelChangeRepository, MemberRider

### Community 29 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.06
Nodes (15): Constraints, LevelHistoryEntry, AssessmentBooking, BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING (+7 more)

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.06
Nodes (11): BookedSeats, DeleteImpact, OverlapRef, SaveResult, SessionsCancelledEvent, SessionTemplateInput, SessionTemplateRules, SessionTemplateService (+3 more)

### Community 35 - ".login"
Cohesion: 0.08
Nodes (3): InviteAccepted, InviteAcceptRequest, LoginRequest

### Community 36 - "AuthUser"
Cohesion: 0.07
Nodes (22): AuthUser, AdminAssessmentSlotController, AssessmentSlotController, BookingController, MyEvaluationsController, MyLevelsController, PlatformAuthController, PlatformClubController (+14 more)

### Community 37 - "RiderBalancesReportTest.java"
Cohesion: 0.11
Nodes (5): RiderReportRow, RiderReportRepository, RiderBalanceReportRow, RiderBalancesReport, RiderBalancesReportTest

### Community 43 - "TestPostgres"
Cohesion: 0.17
Nodes (4): ProdDemoDataCleanupIT, ApplicantIntakeMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 48 - "IntakeScenarioIT"
Cohesion: 0.09
Nodes (7): AdminOversightAccessIT, BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person, ReminderJourneyIT, AdminOversightFixture

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.21
Nodes (3): BalanceDto, MyRiderDto, MyRidersQueryServiceIT

### Community 54 - ".convert"
Cohesion: 0.13
Nodes (3): AuthUserAuthentication, AuthUserConverter, AuthUserConverterTest

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "RepositoryIsolationIT.java"
Cohesion: 0.08
Nodes (13): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+5 more)

### Community 62 - "LevelChangeRulesTest.java"
Cohesion: 0.13
Nodes (5): Direction, DOWN, UP, LevelChangeRequest, LevelChangeRulesTest

### Community 64 - ".runAs"
Cohesion: 0.09
Nodes (5): TenantContext, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, DefaultClubExtension

### Community 66 - "TenantTransactions"
Cohesion: 0.10
Nodes (9): Outcome, TenantTransactions, PlatformLoginRequest, SuperAdminRepository, AdminLockoutService, PasswordService, PlatformAuthService, CountingEncoder (+1 more)

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 69 - "AdminRiderService"
Cohesion: 0.16
Nodes (9): AdminRiderController, AdminRiderList, AdminRiderRow, ReassignResult, ReassignRidersRequest, RiderTotals, TrainerCard, AdminRiderService (+1 more)

### Community 70 - "SubscriptionService.java"
Cohesion: 0.09
Nodes (16): PlatformSubscriptionController, AddSubscriptionRequest, PlanView, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionStatus (+8 more)

### Community 71 - "RecordResultServiceIT"
Cohesion: 0.28
Nodes (3): RecordResultRequest, ResultRecorded, RecordResultServiceIT

### Community 72 - "ClubRepository"
Cohesion: 0.11
Nodes (7): OnExpiry, BLOCK, READ_ONLY, ClubRepository, ClubAccess, ClubAccessService, ClubAccessServiceTest

### Community 74 - "Booking Actions"
Cohesion: 0.12
Nodes (10): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+2 more)

### Community 76 - "Payment"
Cohesion: 0.10
Nodes (13): PaymentEntry, PaymentUsage, RiderPayments, Payment, UsageLine, PaymentEntryAssembler, PaymentState, ACTIVE (+5 more)

### Community 78 - "ClubAdminService"
Cohesion: 0.08
Nodes (15): Admin, ClubDetail, AdminStatus, ACTIVE, INVITED, ClubAdminService, Entry, ClubFilter (+7 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "SubscriptionService.java"
Cohesion: 0.15
Nodes (7): BillingPeriod, ANNUAL, MONTHLY, TRIAL, Plan, Subscription, PlanRepository

### Community 83 - "SessionUsageServiceIT"
Cohesion: 0.14
Nodes (4): VoidPaymentRequest, FixedClock, SessionUsageServiceIT, ClubThreads

### Community 86 - "AssessmentRules"
Cohesion: 0.06
Nodes (10): InviteTrainerRequest, AdminNewMemberRequest, UpdateMemberRequest, Member, MemberRepository, AssessmentRules, MemberBookingService, MemberDetailsService (+2 more)

### Community 88 - "AdminBookingRow"
Cohesion: 0.06
Nodes (18): AdminBookingController, AdminMemberController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest, ChangeBookingRequest, MemberRef (+10 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.11
Nodes (4): PhoneNormalizer, RefreshRequest, ClubAdminInput, CreateClubRequest

### Community 92 - "EvaluationFollowUpQuery"
Cohesion: 0.25
Nodes (9): EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, EvaluationFollowUpQuery, SeatKey (+1 more)

### Community 93 - "com.equestrian.equis.service.platform.ClubAccessService"
Cohesion: 0.21
Nodes (3): ClubStatusController, EmptyDatabaseStartIT, MigrationFromRelease2IT

### Community 96 - "AccountLinkServiceIT"
Cohesion: 0.13
Nodes (5): AccountLinkService, GeneratedToken, TokenGenerator, AccountLinkServiceIT, TokenGeneratorTest

### Community 98 - "PaymentsReport"
Cohesion: 0.22
Nodes (3): PaymentReportRepository, PaymentsReport, PaymentsReportTest

### Community 101 - "Two-Club Fixture"
Cohesion: 0.14
Nodes (4): RowLevelSecurityIT, Builder, ClubData, TwoClubFixture

### Community 102 - "Member Status"
Cohesion: 0.15
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "AuthService.java"
Cohesion: 0.09
Nodes (7): AuthController, AuthTokens, ClubRef, AuthService, AuthTokenService, Issued, AuthTokenServiceIT

### Community 104 - "AuthProperties"
Cohesion: 0.10
Nodes (9): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, JwtConfig (+1 more)

### Community 109 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.09
Nodes (5): BookingActions, PhoneNormalizerTest, ClubAccessFilterTest, LevelRepositoryIT, BookingActionsTest

### Community 110 - "SessionRepository"
Cohesion: 0.06
Nodes (21): SchoolWeek, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyLevelSummary, MyWeek (+13 more)

### Community 113 - "RiderBalanceServiceIT"
Cohesion: 0.17
Nodes (4): PackageBalance, RiderBalance, FixedClock, RiderBalanceServiceIT

### Community 115 - "SessionTemplateController.java"
Cohesion: 0.19
Nodes (9): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+1 more)

### Community 120 - "RiderLevelQueryServiceIT"
Cohesion: 0.22
Nodes (3): RiderLevelDto, UpcomingSeat, RiderLevelQueryServiceIT

### Community 123 - ".book"
Cohesion: 0.22
Nodes (3): FixedClock, MemberBookingServiceIT, TestMembers

### Community 124 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.22
Nodes (3): ClockConfig, MailConfig, SchedulingConfig

### Community 127 - ".toResponse"
Cohesion: 0.26
Nodes (5): SessionDetailMapper, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 129 - "PaymentMethod"
Cohesion: 0.10
Nodes (11): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER, RiderPaymentStatus (+3 more)

### Community 131 - "RiderTest"
Cohesion: 0.11
Nodes (4): Rider, NoSessionsLeftException, LevelChangeTest, RiderTest

### Community 133 - "ApiException"
Cohesion: 0.07
Nodes (12): ApiException, ApiFieldError, ErrorCodes, DateRanges, AdminEvaluationController, RiderSessionPage, Levels, AccountErrors (+4 more)

### Community 135 - "InvitePurpose"
Cohesion: 0.27
Nodes (4): DbConverter, InvitePurpose, INVITE, RESET

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 141 - "CancelReason"
Cohesion: 0.21
Nodes (9): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+1 more)

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
Cohesion: 0.08
Nodes (14): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, DbConverter, SubjectType, ADMIN (+6 more)

### Community 205 - ".status"
Cohesion: 0.08
Nodes (3): EvaluationRequest, SecurityConfigTest, MySessionsControllerTest

### Community 206 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.05
Nodes (22): DbConverter, TrainerStatus, ACTIVE, DISABLED, INVITED, DbConverter, DbConverter, NoticeType (+14 more)

### Community 209 - "SessionTemplateRepository"
Cohesion: 0.12
Nodes (6): SessionProperties, SessionTemplateRepository, AssessmentSeatCounts, SessionGenerationJob, SessionGenerator, TemplateBookedSeats

### Community 213 - "ReportDefinition"
Cohesion: 0.10
Nodes (7): ReportController, ReportDefinition, ReportExportService, ReportRegistry, ReportTotal, ReportWriter, AllTypesReport

### Community 217 - "Trainer"
Cohesion: 0.05
Nodes (16): AdminTrainerController, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals, RiderAccount (+8 more)

### Community 255 - "AccountLinkEmails"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 257 - "ReportParams"
Cohesion: 0.11
Nodes (5): ReportParams, ReportParamsFactory, ReportParamsFactoryTest, Snapshot, ReportParamsTest

### Community 258 - "SmtpNotificationSender.java"
Cohesion: 0.15
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 261 - "ClubAccessFilter.java"
Cohesion: 0.09
Nodes (4): SecurityConfig, ClubAccessFilter, TenantFilter, SubscriptionExpiredException

### Community 263 - "Override"
Cohesion: 0.18
Nodes (3): Named, ReportRegistryTest, Totalled

### Community 268 - "XlsxReportWriterTest"
Cohesion: 0.16
Nodes (3): AllTypesReport, Line, XlsxReportWriterTest

### Community 274 - "AccessReason"
Cohesion: 0.12
Nodes (17): ChangeClubStatusRequest, ClubAccessView, ClubPage, ClubRow, ClubStatus, ACTIVE, SUSPENDED, AccessReason (+9 more)

## Knowledge Gaps
- **122 isolated node(s):** `ADMIN`, `TRAINER`, `RIDER`, `LIST`, `OTHER` (+117 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 511 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **168 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `ApiException` to `assertthat`, `RiderTest`, `RiderRepository`, `Session`, `.status`, `RiderAppQueryService.java`, `.record`, `org.springframework.transaction.annotation.Transactional`, `SessionTemplateServiceIT`, `AuthUser`, `AssignmentServiceIT`, `LevelChangeRulesTest.java`, `TenantTransactions`, `AssessmentRulesIT`, `AdminRiderService`, `SubscriptionService.java`, `ClubRepository`, `Payment`, `.status`, `SubscriptionService.java`, `SessionUsageServiceIT`, `AssessmentRules`, `AdminBookingRow`, `Trainer`, `AccountLinkServiceIT`, `SessionRepository`, `SessionTemplateController.java`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **What connects `ADMIN`, `TRAINER`, `RIDER` to the rest of the system?**
  _122 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SheetWriter` be split into smaller, more focused modules?**
  _Cohesion score 0.10510510510510511 - nodes in this community are weakly interconnected._
- **Why does `CancelReason` connect `CancelReason` to `uuid`, `ApiException`, `jakarta.persistence.AttributeConverter`, `org.junit.jupiter.api.Test`, `AdminBookingRow`, `org.springframework.transaction.annotation.Transactional`, `.booking`?**
  _High betweenness centrality (0.022) - this node is a cross-community bridge._
- **Should `IntegrationTestBase` be split into smaller, more focused modules?**
  _Cohesion score 0.06622185154295246 - nodes in this community are weakly interconnected._
- **Why does `AuthUser` connect `AuthUser` to `RiderBalancesReportIT.java`, `ClubAccessFilter.java`, `ApiException`, `RiderRepository`, `IntegrationTestBase`, `FinishSessionApiIT`, `org.junit.jupiter.api.Test`, `.set`, `AssessmentSlotQueryServiceIT`, `.record`, `RiderEvaluationHistoryApiIT`, `org.springframework.transaction.annotation.Transactional`, `SessionEvaluationApiIT`, `SubjectType`, `LevelChangeFlowIT`, `TrainerAccessServiceIT`, `MyRidersQueryServiceIT`, `.convert`, `.issue`, `.runAs`, `uuid`, `AssessmentRulesIT`, `AssignOnTheDayApiIT`, `SubscriptionService.java`, `RecordResultServiceIT`, `.status`, `SessionUsageServiceIT`, `AdminTrainerApiIT.java`, `EvaluationFollowUpApiIT`, `AuthService.java`, `AuthProperties`, `.toResponse`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **Should `assertthat` be split into smaller, more focused modules?**
  _Cohesion score 0.06418098031001257 - nodes in this community are weakly interconnected._