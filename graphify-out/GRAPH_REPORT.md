# Graph Report - equis-api  (2026-11-08)

## Corpus Check
- 574 files · ~185,288 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: .properties 6, (none) 4, .imports 2)

## Summary
- 5180 nodes · 22936 edges · 247 communities (84 shown, 163 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 2981 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `34af5fcd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AuthUser
- SeedDataIT
- com.equestrian.equis.entity.accounts.Trainer
- .status
- AdminOversightFixture
- BookingReminderApiIT
- AdminRiderService.java
- TrainerAccountsFixture
- .record
- .accounts
- Coach
- org.junit.jupiter.api.BeforeEach
- FinishSessionApiIT
- TwoClubFixture
- uuid
- org.junit.jupiter.api.Test
- RiderRepository
- AssessmentSlotQueryServiceIT
- map
- ApiException
- RiderAppQueryService.java
- SessionRepository
- Rider
- Repository Layer
- AdminOverviewService.java
- TrainerRepository
- Auth User Conversion
- PaymentsScenarioIT
- SessionEvaluationApiIT
- SessionTemplateServiceIT
- AuthServiceIT
- RiderRepository
- SessionTemplateRulesIT
- org.junit.jupiter.params.ParameterizedTest
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
- .book
- Release Workflow
- .issue
- RepositoryIsolationIT.java
- FreshDatabase.java
- AssignmentServiceIT
- API Exception Handler
- AssessmentBooking
- EndpointIsolationIT
- Tenant Context
- AdminRiderApiIT
- jakarta.persistence.AttributeConverter
- My Sessions & Evaluation
- AssignOnTheDayApiIT
- TokenGenerator
- SubscriptionService
- InvitePurpose
- ClubAccessService
- ClubRepositoryIT
- Booking Actions
- TenantIsolationIT
- PaymentState
- SessionTest
- BillingPeriod
- ClubAccessStateIT
- Prod Environment Check
- Plans & Expiry Policy
- SessionUsageServiceIT
- SessionTemplateService
- PerClubSignInIT
- TenantTransactions
- AdminTrainerApiIT
- AssignmentRepository
- CI Workflow
- notblank
- ClubAdminService
- Trainer
- PlatformAuthService.java
- TrainerRepositoryIT
- LevelChangeServiceIT
- AccountLinkServiceIT
- .of
- ClubAccessFilter.java
- Session
- EvaluationFollowUpApiIT
- Two-Club Fixture
- Member Status
- AuthTokenService
- JWT Config
- RefreshTokenRepositoryIT
- AuthProperties
- .toResponse
- .registerAndBook
- RiderEvaluationHistoryApiIT
- SessionGeneratorIT
- AdminPaymentVoidApiIT
- AssessmentRulesIT
- SmtpNotificationSenderTest.java
- Template Mapper
- .admin
- AdminPaymentApiIT
- SmtpNotificationSender.java
- RiderAccountStatus
- AssignmentRepositoryIT
- PaymentMethod
- .book
- JwtServiceTest.java
- PlatformSignInJourneyIT
- AdminLockoutServiceIT
- Cancel Reasons
- Docker Compose
- Default Club Extension
- AdminAccountRepositoryIT
- TemplateBookedSeats
- AdminMemberEditApiIT
- NoticeRepositoryIT
- SessionTemplateRepositoryIT
- SessionTemplateRulesOverlapsTest
- TenantAutoConfiguration.java
- AdminRiderBalanceApiIT
- Changelog Epics A
- SecurityConfig.java
- Role
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- RiderBalanceServiceIT
- .set
- Test Mail Rules Docs
- Test Club Rules Docs
- AdminAssessmentSlotApiIT
- Changelog Epics B
- AuthService.java
- Image Release Docs
- Notification Sender Docs
- SessionEvaluationRepositoryIT
- AccountLinkEmails
- LevelChangeApiIT
- AccountLinkEmailsTest
- LevelChangeRepositoryIT
- Graphify Rules Docs
- .evaluate
- Release 2.0.1
- Maven POM
- PaymentRepositoryIT
- MailTestConfig.java
- .allowed
- JwtConfig.java
- .status
- TenantIdentifierResolver
- MailConfig
- EndpointCoverageGuardIT
- CountingEncoder
- TestSubscriptions.java
- FixedClock

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

## Communities (247 total, 163 thin omitted)

### Community 0 - "AuthUser"
Cohesion: 0.06
Nodes (27): AuthUser, AuthController, AdminAssessmentSlotController, AssessmentSlotController, MyEvaluationsController, MyLevelsController, AdminPaymentController, PlatformAuthController (+19 more)

### Community 2 - "com.equestrian.equis.entity.accounts.Trainer"
Cohesion: 0.07
Nodes (13): Issued, AssessmentBookingRepositoryIT, FixedClock, MemberBookingServiceIT, FixedClock, RecordResultServiceIT, FixedClock, MyLevelsServiceIT (+5 more)

### Community 4 - "AdminOversightFixture"
Cohesion: 0.07
Nodes (8): AdminOversightAccessIT, BookingOversightJourneyIT, NoShowRebooked, IntakeScenarioIT, Person, ReminderJourneyIT, AdminOversightFixture, Schedule

### Community 5 - "BookingReminderApiIT"
Cohesion: 0.06
Nodes (5): AdminMemberApiIT, AdminMemberCloseApiIT, BookingReminderApiIT, NoticeRow, Request

### Community 6 - "AdminRiderService.java"
Cohesion: 0.15
Nodes (5): RiderProgress, LevelHistoryEntry, RiderTotals, TrainerCard, Levels

### Community 7 - "TrainerAccountsFixture"
Cohesion: 0.07
Nodes (8): OutgoingEmail, Request, TrainerAdminAccessIT, TrainerInviteJourneyIT, TrainerResetJourneyIT, RiderAccountJourneyIT, RecordingNotificationSender, TrainerAccountsFixture

### Community 9 - ".accounts"
Cohesion: 0.18
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioIT, RiderLogin

### Community 10 - "Coach"
Cohesion: 0.12
Nodes (6): SessionEvaluationsScenarioIT, SharedSession, SameDaySessionsScenarioIT, Coach, Check, SameDayJourneys

### Community 11 - "org.junit.jupiter.api.BeforeEach"
Cohesion: 0.09
Nodes (3): AuthTokens, ClubRef, IntegrationTestBase

### Community 12 - "FinishSessionApiIT"
Cohesion: 0.08
Nodes (7): FinishSessionApiIT, Request, State, CANCELLED, SCHEDULED, MyWeekApiIT, Request

### Community 13 - "TwoClubFixture"
Cohesion: 0.17
Nodes (3): RowLevelSecurityIT, Config, TwoClubFixture

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.04
Nodes (9): AuthPropertiesTest, TestConfig, SecurityConfigTest, RoleTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, EquisApiApplicationIT (+1 more)

### Community 17 - "RiderRepository"
Cohesion: 0.06
Nodes (20): BookingController, RiderAccount, TrainerStatus, ACTIVE, DISABLED, INVITED, AdminAccountRepository, InviteRepository (+12 more)

### Community 19 - "AssessmentSlotQueryServiceIT"
Cohesion: 0.07
Nodes (9): DateRanges, AssessmentSlotDayDto, AssessmentSlotDto, BookingRowDto, SlotDetailDto, SlotBookedCount, AssessmentSlotMapper, AssessmentSlotQueryServiceIT (+1 more)

### Community 20 - "map"
Cohesion: 0.06
Nodes (7): ErrorCodes, AssessmentSlotControllerTest, BookingControllerTest, MyLevelsControllerTest, MyRidersControllerTest, ActiveClubAccess, Config

### Community 21 - "ApiException"
Cohesion: 0.08
Nodes (11): ApiException, ApiFieldError, AdminRiderController, ChangeBookingRequest, RiderSessionPage, AdminRiderList, ReassignResult, ReassignRidersRequest (+3 more)

### Community 24 - "RiderAppQueryService.java"
Cohesion: 0.05
Nodes (35): BalanceDto, PaymentEntry, PaymentResult, PaymentUsage, RiderPayments, ExpiringSoon, RiderBanners, RiderMe (+27 more)

### Community 25 - "SessionRepository"
Cohesion: 0.07
Nodes (19): SchoolWeek, LevelRef, Day, LevelSessionsResponse, Seat, SessionSlot, MyWeek, WeekDay (+11 more)

### Community 26 - "Rider"
Cohesion: 0.11
Nodes (12): TenantGuard, AdminAccount, RefreshToken, Member, Club, Level, Notice, SessionUsage (+4 more)

### Community 27 - "Repository Layer"
Cohesion: 0.10
Nodes (4): RefreshTokenRepository, ClubRepository, PendingEvaluationRow, SubscriptionRepository

### Community 28 - "AdminOverviewService.java"
Cohesion: 0.07
Nodes (21): AdminOverviewController, TrainerRef, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession, TrackFill (+13 more)

### Community 29 - "TrainerRepository"
Cohesion: 0.10
Nodes (9): AdminTrainerController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals (+1 more)

### Community 30 - "Auth User Conversion"
Cohesion: 0.13
Nodes (3): AuthUserAuthentication, AuthUserConverter, AuthUserConverterTest

### Community 32 - "SessionEvaluationApiIT"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiIT, State, CANCELLED, SCHEDULED

### Community 33 - "SessionTemplateServiceIT"
Cohesion: 0.15
Nodes (3): DeleteImpact, SessionsCancelledEvent, SessionTemplateServiceIT

### Community 35 - "AuthServiceIT"
Cohesion: 0.14
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceIT

### Community 36 - "RiderRepository"
Cohesion: 0.07
Nodes (19): AdminBookingController, AdminMemberController, AdminBookingList, AdminBookingRow, BookingTotals, CancelBookingRequest, MemberRef, SlotRef (+11 more)

### Community 37 - "SessionTemplateRulesIT"
Cohesion: 0.20
Nodes (4): OverlapRef, SessionTemplateInput, Warning, SessionTemplateRulesIT

### Community 38 - "org.junit.jupiter.params.ParameterizedTest"
Cohesion: 0.14
Nodes (3): PhoneNormalizerTest, ClubAccessFilterTest, LevelRepositoryIT

### Community 43 - "TestPostgres"
Cohesion: 0.16
Nodes (5): ProdDemoDataCleanupIT, ApplicantIntakeMigrationIT, RiderAccountMigrationIT, SessionFinishedMigrationIT, TestPostgres

### Community 47 - "RiderRepositoryIT"
Cohesion: 0.08
Nodes (3): LevelRiderCount, RiderTest, RiderRepositoryIT

### Community 48 - "org.springframework.test.web.servlet.ResultActions"
Cohesion: 0.11
Nodes (3): TrainerDisableJourneyIT, Request, RiderReassignJourneyIT

### Community 51 - "MyRidersQueryServiceIT"
Cohesion: 0.10
Nodes (5): RiderLevelDto, UpcomingSeat, MyRidersQueryServiceIT, FixedClock, RiderLevelQueryServiceIT

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 62 - "AssessmentBooking"
Cohesion: 0.07
Nodes (13): Constraints, AssessmentBooking, BookingStatus, CANCELLED, NOSHOW, PLACED, UPCOMING, DbConverter (+5 more)

### Community 63 - "EndpointIsolationIT"
Cohesion: 0.15
Nodes (13): Case, EndpointIsolationIT, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+5 more)

### Community 64 - "Tenant Context"
Cohesion: 0.15
Nodes (4): TenantContext, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 66 - "jakarta.persistence.AttributeConverter"
Cohesion: 0.14
Nodes (6): DbConverter, DbConverter, DbConverter, DbConverter, DayOfWeekConverter, DbConverter

### Community 68 - "AssignOnTheDayApiIT"
Cohesion: 0.17
Nodes (5): AssignOnTheDayApiIT, State, CANCELLED, FINISHED, SCHEDULED

### Community 69 - "TokenGenerator"
Cohesion: 0.18
Nodes (3): GeneratedToken, TokenGenerator, TokenGeneratorTest

### Community 70 - "SubscriptionService"
Cohesion: 0.09
Nodes (16): PlatformSubscriptionController, AddSubscriptionRequest, PlanView, StartMode, AFTER_CURRENT, NOW, SubscriptionResult, SubscriptionStatus (+8 more)

### Community 71 - "InvitePurpose"
Cohesion: 0.11
Nodes (12): DbConverter, InvitePurpose, INVITE, RESET, DbConverter, SubjectType, ADMIN, RIDER (+4 more)

### Community 72 - "ClubAccessService"
Cohesion: 0.21
Nodes (3): ClubAccess, ClubAccessService, ClubAccessServiceTest

### Community 74 - "Booking Actions"
Cohesion: 0.12
Nodes (10): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+2 more)

### Community 76 - "PaymentState"
Cohesion: 0.16
Nodes (7): PaymentState, ACTIVE, EXPIRED, UPCOMING, USED_UP, VOIDED, PaymentStateTest

### Community 78 - "BillingPeriod"
Cohesion: 0.08
Nodes (24): ClubAccessView, Admin, ClubDetail, ClubPage, ClubRow, AdminStatus, ACTIVE, INVITED (+16 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plans & Expiry Policy"
Cohesion: 0.17
Nodes (5): OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 83 - "SessionUsageServiceIT"
Cohesion: 0.08
Nodes (5): VoidPaymentRequest, PaymentTest, FixedClock, SessionUsageServiceIT, ClubThreads

### Community 84 - "SessionTemplateService"
Cohesion: 0.14
Nodes (6): SessionProperties, SessionTemplateRepository, BookedSeats, SessionGenerator, SessionTemplateRules, SessionTemplateService

### Community 86 - "TenantTransactions"
Cohesion: 0.12
Nodes (6): Outcome, TenantTransactions, PlatformClubController, ChangeClubStatusRequest, ClubErrors, ClubOnboardingService

### Community 87 - "AdminTrainerApiIT"
Cohesion: 0.06
Nodes (4): AdminTrainerActionsApiIT, AdminTrainerApiIT, AccountLinkMailListenerIT, MutableClock

### Community 88 - "AssignmentRepository"
Cohesion: 0.06
Nodes (15): RiderEvaluations, Direction, DOWN, UP, LevelChangeRequest, EvaluationHistoryRow, LevelChangeRepository, AssignmentRepository (+7 more)

### Community 89 - "CI Workflow"
Cohesion: 0.07
Nodes (34): needs, runs-on, steps, env, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET (+26 more)

### Community 90 - "notblank"
Cohesion: 0.10
Nodes (3): PhoneNormalizer, ClubAdminInput, CreateClubRequest

### Community 91 - "ClubAdminService"
Cohesion: 0.13
Nodes (9): ClubAdminService, Entry, ClubFilter, ClubSort, CODE, ENDS_AT, NAME, ClubUsageService (+1 more)

### Community 92 - "Trainer"
Cohesion: 0.09
Nodes (13): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, Invite (+5 more)

### Community 93 - "PlatformAuthService.java"
Cohesion: 0.15
Nodes (6): PlatformLoginRequest, SuperAdminRepository, AdminLockoutService, PasswordService, PlatformAuthService, PasswordServiceTest

### Community 98 - "ClubAccessFilter.java"
Cohesion: 0.12
Nodes (5): ClubAccessFilter, TenantFilter, ClubStatusController, ClubBlockedException, SubscriptionExpiredException

### Community 99 - "Session"
Cohesion: 0.06
Nodes (16): SeatEvaluation, SessionEvaluation, Rider, Assignment, Session, SessionStatus, CANCELLED, DONE (+8 more)

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 107 - "AuthProperties"
Cohesion: 0.33
Nodes (7): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset

### Community 108 - ".toResponse"
Cohesion: 0.27
Nodes (6): MySessionsController, SessionDetailMapper, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse

### Community 109 - ".registerAndBook"
Cohesion: 0.08
Nodes (4): AdminNewMemberRequest, UpdateMemberRequest, MemberTest, MemberRepositoryIT

### Community 111 - "SessionGeneratorIT"
Cohesion: 0.23
Nodes (3): GenerationResult, FixedClock, SessionGeneratorIT

### Community 114 - "SmtpNotificationSenderTest.java"
Cohesion: 0.07
Nodes (5): AccessToken, JwtConfigTest, MailConfigTest, JwtServiceTest, SmtpNotificationSenderTest

### Community 115 - "Template Mapper"
Cohesion: 0.20
Nodes (9): SessionTemplateController, SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateCreateRequest, SessionTemplateResponse (+1 more)

### Community 118 - "SmtpNotificationSender.java"
Cohesion: 0.15
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 119 - "RiderAccountStatus"
Cohesion: 0.36
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 121 - "PaymentMethod"
Cohesion: 0.24
Nodes (6): PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 124 - "JwtServiceTest.java"
Cohesion: 0.27
Nodes (3): ClockConfig, SchedulingConfig, SecurityBeans

### Community 127 - "Cancel Reasons"
Cohesion: 0.20
Nodes (8): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 133 - "NoticeRepositoryIT"
Cohesion: 0.14
Nodes (5): NoticeType, RESULT_OVERDUE, SESSION_CANCELLED, RefTime, NoticeRepositoryIT

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 141 - "Role"
Cohesion: 0.16
Nodes (5): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER

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

### Community 150 - "AuthService.java"
Cohesion: 0.14
Nodes (4): Access, ClubStatusView, AuthService, BlockedSignIn

### Community 152 - "Image Release Docs"
Cohesion: 0.40
Nodes (5): GHCR image release pipeline (GitHub Actions), Release 2.0.2 (GHCR release pipeline), Docker image (Java 21 JRE + app.jar), equis-api (Spring Boot backend), GET /api/actuator/health

### Community 153 - "Notification Sender Docs"
Cohesion: 0.50
Nodes (3): LogNotificationSender, RecordingNotificationSender, TestPostgres (shared container, per-class DB)

### Community 155 - "AccountLinkEmails"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

## Knowledge Gaps
- **115 isolated node(s):** `com.equestrian:equis-api`, `SCHEDULED`, `CANCELLED`, `SCHEDULED`, `CANCELLED` (+110 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 492 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **163 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ApiException` connect `ApiException` to `AuthUser`, `com.equestrian.equis.entity.accounts.Trainer`, `AdminRiderService.java`, `org.junit.jupiter.api.BeforeEach`, `RiderRepository`, `AssessmentSlotQueryServiceIT`, `map`, `RiderAppQueryService.java`, `SessionRepository`, `AdminOverviewService.java`, `.evaluate`, `RiderRepository`, `SessionTemplateRulesIT`, `PlanAndSubscriptionIT`, `TrainerAccessServiceIT`, `AssignmentServiceIT`, `AssessmentBooking`, `My Sessions & Evaluation`, `SubscriptionService`, `InvitePurpose`, `ClubAccessService`, `.status`, `SessionTemplateService`, `TenantTransactions`, `AssignmentRepository`, `ClubAdminService`, `Trainer`, `PlatformAuthService.java`, `AccountLinkServiceIT`, `Session`, `.registerAndBook`, `AssessmentRulesIT`, `Template Mapper`, `AdminLockoutServiceIT`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **What connects `com.equestrian:equis-api`, `SCHEDULED`, `CANCELLED` to the rest of the system?**
  _115 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AuthUser` be split into smaller, more focused modules?**
  _Cohesion score 0.056945481702763256 - nodes in this community are weakly interconnected._
- **Why does `Rider` connect `Session` to `AdminRiderService.java`, `uuid`, `RiderRepository`, `map`, `AuthService.java`, `RiderAppQueryService.java`, `SessionRepository`, `Rider`, `Repository Layer`, `RiderRepositoryIT`, `org.springframework.test.web.servlet.ResultActions`, `MyRidersQueryServiceIT`, `AssessmentBooking`, `My Sessions & Evaluation`, `InvitePurpose`, `SessionUsageServiceIT`, `AdminTrainerApiIT`, `AssignmentRepository`, `Trainer`, `.registerAndBook`, `AssessmentRulesIT`, `RiderAccountStatus`, `AssignmentRepositoryIT`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `com.equestrian.equis.entity.accounts.Trainer` be split into smaller, more focused modules?**
  _Cohesion score 0.07293064876957495 - nodes in this community are weakly interconnected._
- **Why does `TwoClubFixture` connect `TwoClubFixture` to `Two-Club Fixture`, `TrainerAccountsFixture`, `org.junit.jupiter.api.BeforeEach`, `ClubAccessStateIT`, `uuid`, `map`, `PerClubSignInIT`, `AdminTrainerApiIT`, `RepositoryIsolationIT.java`, `EndpointIsolationIT`?**
  _High betweenness centrality (0.013) - this node is a cross-community bridge._
- **Should `.status` be split into smaller, more focused modules?**
  _Cohesion score 0.1112618724559023 - nodes in this community are weakly interconnected._