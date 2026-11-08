# Graph Report - equis-api  (2026-11-08)

## Corpus Check
- 581 files · ~187,508 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 17 file(s) not represented in the graph (top: .properties 7, (none) 4, .imports 2)

## Summary
- 5235 nodes · 22691 edges · 241 communities (75 shown, 166 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 2520 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9b860b16`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AuthUser
- SeedDataIT
- com.equestrian.equis.repository.sessions.SessionRepository
- .status
- AdminOversightFixture
- AdminTrainerApiIT
- IntegrationTestBase
- TrainerAccountsFixture
- .admin
- .accounts
- Coach
- FinishSessionApiIT
- PlanLimitService.java
- uuid
- org.junit.jupiter.api.Test
- .slots
- AssessmentSlotQueryServiceIT
- MySessionsControllerTest.java
- PlatformSubscriptionApiIT
- RiderAppQueryService.java
- SessionRepository
- Rider
- Repository Layer
- AdminOverviewService.java
- TrainerLevelCount
- .convert
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- AuthServiceIT
- AssessmentBookingRepository
- SessionTemplateRulesIT
- org.springframework.jdbc.core.JdbcTemplate
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
- InviteRepositoryIT
- .book
- Release Workflow
- .issue
- RepositoryIsolationIT.java
- AssignmentServiceIT
- API Exception Handler
- org.springframework.stereotype.Component
- EndpointIsolationIT
- Tenant Context
- AdminRiderApiIT
- jakarta.persistence.AttributeConverter
- .status
- AssignOnTheDayApiIT
- SubscriptionService
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
- SessionTemplateService
- PerClubSignInIT
- TenantTransactions
- AdminTrainerApiIT.java
- Rider
- CI Workflow
- notblank
- ClubAdminService
- Session
- .hash
- TrainerRepositoryIT
- LevelChangeServiceIT
- AccountLinkServiceIT
- .of
- ClubAccessFilter.java
- AdminMemberCloseApiIT
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthTokenService
- AuthProperties
- AdminMemberApiIT
- SessionEvaluationsScenarioIT
- PlanLimitApiIT
- .register
- RiderEvaluationHistoryApiIT
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- AssessmentRulesIT
- .reset
- Template Mapper
- .admin
- AdminPaymentApiIT
- SmtpNotificationSenderTest.java
- RiderAccountStatus
- com.equestrian.equis.entity.accounts.Trainer
- PaymentMethod
- AuthUserConverter.java
- .book
- JwtServiceTest.java
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- Cancel Reasons
- Docker Compose
- Default Club Extension
- AdminAccountRepositoryIT
- RiderTest
- AdminMemberEditApiIT
- .snapshot
- SessionTemplateRepositoryIT
- TrainerDisableJourneyIT
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
- Changelog Epics A
- RecordingNotificationSender
- SubjectType
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- java.time.YearMonth
- .set
- Test Mail Rules Docs
- Test Club Rules Docs
- AdminAssessmentSlotApiIT
- Changelog Epics B
- AuthTokenService
- Image Release Docs
- Notification Sender Docs
- ProdDemoDataCleanupIT
- ProdDemoDataCleanupIT.java
- LevelChangeApiIT
- LevelChangeRepositoryIT
- Graphify Rules Docs
- Release 2.0.1
- Maven POM
- TenantTransactions
- LimitedResource
- .allowed
- StartMode
- .status
- EndpointCoverageGuardIT

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

## Communities (241 total, 166 thin omitted)

### Community 0 - "AuthUser"
Cohesion: 0.04
Nodes (40): AuthUser, AdminTrainerController, AdminAssessmentSlotController, MyEvaluationsController, MyLevelsController, AdminPaymentController, PlatformClubController, PlatformSubscriptionController (+32 more)

### Community 2 - "com.equestrian.equis.repository.sessions.SessionRepository"
Cohesion: 0.06
Nodes (18): SubscriptionExpiredException, FixedClock, FixedClock, FixedClock, RecordResultServiceIT, FixedClock, FixedClock, FixedClock (+10 more)

### Community 4 - "AdminOversightFixture"
Cohesion: 0.15
Nodes (4): BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person

### Community 5 - "AdminTrainerApiIT"
Cohesion: 0.07
Nodes (5): AdminTrainerApiIT, BookingReminderApiIT, NoticeRow, Request, MutableClock

### Community 7 - "TrainerAccountsFixture"
Cohesion: 0.10
Nodes (6): Request, TrainerAdminAccessIT, Request, TrainerInviteJourneyIT, TrainerResetJourneyIT, TrainerAccountsFixture

### Community 9 - ".accounts"
Cohesion: 0.16
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.20
Nodes (3): SameDaySessionsScenarioIT, Check, SameDayJourneys

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.07
Nodes (7): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED, MyWeekApiIT, Request

### Community 13 - "PlanLimitService.java"
Cohesion: 0.11
Nodes (7): AdminClubUsageController, ClubStatusController, ClubStatusView, ClubUsageView, Count, PlanLimitService, Config

### Community 15 - "uuid"
Cohesion: 0.07
Nodes (3): RiderEvaluation, EvaluationHistoryRow, PendingEvaluationRow

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (12): AccessToken, AuthPropertiesTest, JwtConfigTest, MailConfigTest, CancelReasonTest, MemberStatusTest, SessionEvaluationTest, PaymentEnumsTest (+4 more)

### Community 17 - ".slots"
Cohesion: 0.11
Nodes (9): AssessmentSlotController, AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, SlotBookedCount, AssessmentSlotMapper (+1 more)

### Community 20 - "MySessionsControllerTest.java"
Cohesion: 0.10
Nodes (5): AssessmentSlotControllerTest, BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess

### Community 24 - "RiderAppQueryService.java"
Cohesion: 0.05
Nodes (27): RiderAppController, RiderPayments, ExpiringSoon, RiderAssessment, RiderBanners, RiderMe, RiderPackage, RiderPayments (+19 more)

### Community 25 - "SessionRepository"
Cohesion: 0.07
Nodes (17): SchoolWeek, SessionDetailMapper, SeatEvaluation, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot (+9 more)

### Community 26 - "Rider"
Cohesion: 0.06
Nodes (20): TenantGuard, Direction, DOWN, UP, LevelChangeRequest, AdminAccount, Invite, RefreshToken (+12 more)

### Community 27 - "Repository Layer"
Cohesion: 0.08
Nodes (9): AdminAccountRepository, InviteRepository, RefreshTokenRepository, ClubRepository, PlanRepository, AccountLinkService, IssuedLink, TrainerRowAssembler (+1 more)

### Community 28 - "AdminOverviewService.java"
Cohesion: 0.07
Nodes (21): AdminRiderController, TrainerRef, ActivityItem, Attention, PendingInvite, TrainerRiderCount, AdminRiderList, AdminRiderRow (+13 more)

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.16
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceIT

### Community 35 - "AuthServiceIT"
Cohesion: 0.14
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "AssessmentBookingRepository"
Cohesion: 0.04
Nodes (36): AdminBookingController, AdminMemberController, AdminBookingList, AdminBookingRow, AdminNewMemberRequest, BookingTotals, CancelBookingRequest, ChangeBookingRequest (+28 more)

### Community 38 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.08
Nodes (6): PhoneNormalizerTest, ClubAccessFilterTest, MemberRepositoryIT, LevelRepositoryIT, NoticeRepositoryIT, PostgresContainer

### Community 43 - "TestPostgres"
Cohesion: 0.17
Nodes (4): ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 48 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.11
Nodes (5): AdminOversightAccessIT, ReminderJourneyIT, RiderReassignJourneyIT, AdminOversightFixture, Coach

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.22
Nodes (3): BalanceDto, MyRiderDto, MyRidersQueryServiceIT

### Community 54 - ".book"
Cohesion: 0.09
Nodes (4): RecordResultRequest, ResultRecorded, AssessmentBookingTest, AssessmentBookingRepositoryIT

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 62 - "org.springframework.stereotype.Component"
Cohesion: 0.09
Nodes (7): BookingCancellationListener, BookingSeatCounts, LogNotificationSender, AssessmentSeatCounts, AssignmentCancellationListener, SessionGenerationJob, TemplateBookedSeats

### Community 63 - "EndpointIsolationIT"
Cohesion: 0.13
Nodes (12): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+4 more)

### Community 64 - "Tenant Context"
Cohesion: 0.10
Nodes (6): TenantContext, TenantIdentifierResolver, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet, PlatformPingController

### Community 66 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.10
Nodes (6): DbConverter, DbConverter, DbConverter, DbConverter, DayOfWeekConverter, DbConverter

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.16
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 70 - "SubscriptionService"
Cohesion: 0.10
Nodes (10): SubscriptionStatus, CURRENT, ENDED, REPLACED, UPCOMING, VOIDED, SubscriptionView, Subscription (+2 more)

### Community 71 - "list"
Cohesion: 0.08
Nodes (12): ApiException, ApiFieldError, Constraints, ErrorCodes, DateRanges, InvitePurpose, INVITE, RESET (+4 more)

### Community 72 - "ClubAccessService"
Cohesion: 0.27
Nodes (3): ClubAccess, ClubAccessService, ClubAccessServiceTest

### Community 74 - "Booking Actions"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 76 - "PaymentState"
Cohesion: 0.14
Nodes (9): PaymentEntry, PaymentUsage, PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED (+1 more)

### Community 78 - "BillingPeriod"
Cohesion: 0.10
Nodes (24): ClubAccessView, Admin, ClubDetail, ClubPage, ClubRow, AdminStatus, ACTIVE, INVITED (+16 more)

### Community 82 - "OnExpiry"
Cohesion: 0.24
Nodes (4): PlanView, OnExpiry, BLOCK, READ_ONLY

### Community 83 - "SessionUsageServiceIT"
Cohesion: 0.16
Nodes (4): PaymentResult, VoidPaymentRequest, SessionUsageServiceIT, ClubThreads

### Community 84 - "SessionTemplateService"
Cohesion: 0.21
Nodes (3): SessionTemplateController, BookedSeats, SessionTemplateService

### Community 85 - "PerClubSignInIT"
Cohesion: 0.09
Nodes (5): PerClubSignInIT, RowLevelSecurityIT, ClubData, Config, TwoClubFixture

### Community 88 - "Rider"
Cohesion: 0.04
Nodes (31): BookingController, AdminEvaluationController, AdminRiderAccountController, RiderEvaluations, RiderProgress, LevelHistoryEntry, RiderAccount, RiderLevelDto (+23 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.14
Nodes (3): PhoneNormalizer, ClubAdminInput, CreateClubRequest

### Community 91 - "ClubAdminService"
Cohesion: 0.12
Nodes (9): ClubAdminService, Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME, ClubUsageService (+1 more)

### Community 92 - "Session"
Cohesion: 0.04
Nodes (32): AdminOverviewController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, OverviewResponse (+24 more)

### Community 96 - "AccountLinkServiceIT"
Cohesion: 0.15
Nodes (4): GeneratedToken, TokenGenerator, AccountLinkServiceIT, TokenGeneratorTest

### Community 97 - ".of"
Cohesion: 0.09
Nodes (3): SessionEvaluationRepositoryIT, SessionRepositoryIT, SessionDetailServiceIT

### Community 98 - "ClubAccessFilter.java"
Cohesion: 0.08
Nodes (3): SecurityConfig, ClubAccessFilter, TenantFilter

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 103 - "AuthTokenService"
Cohesion: 0.15
Nodes (3): RefreshRequest, ClubBlockedException, AuthTokenServiceIT

### Community 104 - "AuthProperties"
Cohesion: 0.09
Nodes (10): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, JwtConfig (+2 more)

### Community 115 - "Template Mapper"
Cohesion: 0.17
Nodes (10): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse, SessionTemplateUpdateRequest (+2 more)

### Community 118 - "SmtpNotificationSenderTest.java"
Cohesion: 0.05
Nodes (12): MailSettings, Mode, LOG, SMTP, AccountLinkCreatedEvent, AccountLinkEmails, Texts, AccountLinkMailListener (+4 more)

### Community 119 - "RiderAccountStatus"
Cohesion: 0.31
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 120 - "com.equestrian.equis.entity.accounts.Trainer"
Cohesion: 0.11
Nodes (3): AssignmentRepositoryIT, MyLevelsServiceIT, RiderLevelQueryServiceIT

### Community 121 - "PaymentMethod"
Cohesion: 0.25
Nodes (7): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 122 - "AuthUserConverter.java"
Cohesion: 0.17
Nodes (3): AuthUserAuthentication, AuthUserConverter, SuperAdminRepository

### Community 124 - "JwtServiceTest.java"
Cohesion: 0.12
Nodes (6): ClockConfig, MailConfig, SchedulingConfig, SecurityBeans, TestConfig, CountingEncoder

### Community 127 - "Cancel Reasons"
Cohesion: 0.21
Nodes (9): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+1 more)

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 141 - "SubjectType"
Cohesion: 0.11
Nodes (12): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, DbConverter, SubjectType, ADMIN (+4 more)

### Community 142 - "Base URLs & Accounts Docs"
Cohesion: 0.29
Nodes (8): APP_RIDER_BASE_URL, E15 Rider accounts and data, E1 Auth & accounts, E6 Trainer accounts, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (smtp|log), Production environment variables

### Community 143 - "Ops Scripts & Plans Docs"
Cohesion: 0.25
Nodes (8): V5__seed_admin.sql, create-super-admin.sql, on_expiry policy (BLOCK | READ_ONLY), Ops scripts (run by hand), Plans TRIAL / BASIC / PRO / MAX, POST /api/platform/auth/login, upsert-plan.sql, APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH

### Community 144 - "java.time.YearMonth"
Cohesion: 0.08
Nodes (5): RiderPayment, StarTrendPoint, PaymentTest, PaymentRepositoryIT, RiderBalanceServiceIT

### Community 147 - "Test Club Rules Docs"
Cohesion: 0.33
Nodes (6): ClubThreads, DefaultClubExtension, TenantContext.runAs, APP_DEFAULT_CLUB_CODE / APP_DEFAULT_CLUB_NAME, equis_app database role (SET ROLE), Row-level security per club (V23, V24)

### Community 149 - "Changelog Epics B"
Cohesion: 0.47
Nodes (6): E11 Admin intake, E12 Payments and balance, E13 Same-day sessions, E14 Session evaluations, E4 Assessment, Release 2.0.0

### Community 150 - "AuthTokenService"
Cohesion: 0.08
Nodes (13): AuthController, PlatformAuthController, Access, AuthTokens, ClubRef, PlatformLoginRequest, AdminLockoutService, AuthService (+5 more)

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

### Community 206 - "LimitedResource"
Cohesion: 0.38
Nodes (4): LimitedResource, RIDERS, TRAINERS, PlanLimitReachedException

### Community 208 - "StartMode"
Cohesion: 0.50
Nodes (3): StartMode, AFTER_CURRENT, NOW

## Knowledge Gaps
- **117 isolated node(s):** `com.equestrian:equis-api`, `TRAINERS`, `RIDERS`, `SCHEDULED`, `CANCELLED` (+112 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 494 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **166 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `list` to `AuthUser`, `com.equestrian.equis.repository.sessions.SessionRepository`, `org.junit.jupiter.api.BeforeEach`, `.slots`, `AuthTokenService`, `RiderAppQueryService.java`, `SessionRepository`, `Rider`, `Repository Layer`, `AdminOverviewService.java`, `SessionTemplateServiceIT`, `AssessmentBookingRepository`, `SessionTemplateRulesIT`, `AssignmentServiceIT`, `API Exception Handler`, `SubscriptionService`, `TenantTransactions`, `.status`, `SessionUsageServiceIT`, `TenantTransactions`, `Rider`, `Session`, `.hash`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **What connects `com.equestrian:equis-api`, `TRAINERS`, `RIDERS` to the rest of the system?**
  _117 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthUser` be split into smaller, more focused modules?**
  _Cohesion score 0.04008667388949079 - nodes in this community are weakly interconnected._
- **Why does `TwoClubFixture` connect `PerClubSignInIT` to `AdminTrainerApiIT`, `org.springframework.jdbc.core.JdbcTemplate`, `IntegrationTestBase`, `Two-Club Fixture`, `org.junit.jupiter.api.BeforeEach`, `RecordingNotificationSender`, `uuid`, `ClubAccessStateIT`, `RepositoryIsolationIT.java`, `JwtServiceTest.java`, `EndpointIsolationIT`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `com.equestrian.equis.repository.sessions.SessionRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.05730777159348588 - nodes in this community are weakly interconnected._
- **Why does `AuthUser` connect `AuthUser` to `FinishSessionApiIT`, `SubjectType`, `org.junit.jupiter.api.Test`, `.slots`, `.set`, `MySessionsControllerTest.java`, `AuthTokenService`, `RiderAppQueryService.java`, `.convert`, `SessionEvaluationApiIT`, `AssessmentBookingRepository`, `LevelChangeFlowIT`, `TrainerAccessServiceIT`, `.book`, `.issue`, `Tenant Context`, `.status`, `AssignOnTheDayApiIT`, `list`, `SessionUsageServiceIT`, `AdminTrainerApiIT.java`, `ClubAccessFilter.java`, `EvaluationFollowUpApiIT`, `AuthProperties`, `RiderEvaluationHistoryApiIT`, `AssessmentRulesIT`, `.admin`, `AuthUserConverter.java`, `JwtServiceTest.java`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `.status` be split into smaller, more focused modules?**
  _Cohesion score 0.12804878048780488 - nodes in this community are weakly interconnected._