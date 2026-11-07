# Graph Report - equis-api  (2026-11-08)

## Corpus Check
- Large corpus: 559 files · ~183,342 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder.

## Summary
- 5089 nodes · 24046 edges · 205 communities (81 shown, 124 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 2898 edges (avg confidence: 0.85)
- Token cost: 68,558 input · 0 output

## Community Hubs (Navigation)
- Web Controllers & MVC Imports
- Spring Boot Test Wiring
- Session Templates & Test Asserts
- Subscriptions & Club Onboarding
- Admin Oversight Journeys
- Admin Member API Tests
- Error Codes & Validation
- Rider/Trainer Account Journeys
- Assessment Results
- Evaluation History & Security
- Scenario Journey Tests
- Roles, Clubs & Threads
- Admin Trainer & Mail Tests
- Test Fixtures & Clock
- Platform Club & Invite Tests
- Rider Evaluation DTOs
- Exception Handler & Rules Tests
- Assessment Booking Service
- Sessions & Assignments
- Assessment Slot Query
- Security Beans & Config
- Admin Rider Rows & Reassign
- Platform Sign-In & Subs Tests
- Admin Booking Change Tests
- Rider App & Payments Query
- My Levels & School Week
- JPA Entities Core
- Repository Layer
- Admin Overview & Bookings
- Trainer Admin Service
- Auth User Conversion
- Payments Scenario Tests
- Session Evaluation API Tests
- Template Delete Impact
- Admin Booking API Tests
- Auth Service & Invites
- Rider Accounts Scenario
- Finish Session API Tests
- Phone Normalizer & Params
- Rider Reassign API Tests
- Plan & Subscription Model
- Intake Scenario Tests
- Admin Overview API Tests
- Flyway Migration Tests
- Level Change Model
- Level Change Flow
- Trainer Access Service
- Rider Entity
- Level Change Service
- Console Finished Sessions
- Invite Repository Tests
- My Riders & Balance
- Rider App API & JWT
- Template Rules & Warnings
- Admin Rider API Tests
- Release Workflow
- Session Usage Service
- Rider Balance Service
- Rider Level Counts
- Assignment Service
- My Week API Tests
- API Exception Handler
- Jobs & Event Listeners
- Endpoint Isolation Tests
- Tenant Context
- Assign On The Day Tests
- Converters & Slot Labels
- My Sessions & Evaluation
- Level Change API Tests
- Tenant-Aware DataSource
- Row-Level Security Tests
- Account Link Events
- Evaluation Follow-Up Tests
- Club Repository Tests
- Booking Actions
- Tenant Isolation Tests
- Payment State & Usage
- Session Repository
- Club Access Views
- Rider Eval History Tests
- Payment Void API Tests
- Prod Environment Check
- Plans & Expiry Policy
- Payment Entity
- Payment Repository Tests
- Per-Club Sign-In Tests
- Admin Payment API Tests
- Mail & SMTP Tests
- Level Change Rules
- CI Workflow
- Request Validation
- Club Admin & Usage
- Evaluation Follow-Up
- Admin Lockout
- Trainer Repository
- Tenant Filter
- Account Link Service
- Admin Member Edit Tests
- Security Config
- Rider Balance API Tests
- Two-Club Fixture
- Member Status
- Auth Tokens
- Repository Isolation
- Roles & Subject Types
- Platform Club Status
- My Sessions Controller
- Member Entity
- Notices
- Session Generator
- Admin Slot API Tests
- Config Properties
- SMTP Sender
- Template Mapper
- Template Controller Tests
- Evaluation Repository Tests
- Account Link Mail
- Rider Account Service
- Assignment Repository
- Refresh Tokens
- Session Detail Service
- Member Booking Tests
- Mail & Scheduling Config
- Booked Seat Counts
- Session Evaluation Entity
- Cancel Reasons
- Docker Compose
- Default Club Extension
- Admin Account Repo
- Password Service
- Club Access Service
- Pending Evaluations
- Payment Method
- Session Done Rules
- Token Generator
- Invite Entity
- Changelog Epics A
- Notice Type
- Rider Account Status
- Base URLs & Accounts Docs
- Ops Scripts & Plans Docs
- Tenant Identifier Resolver
- Club Sort
- Test Mail Rules Docs
- Test Club Rules Docs
- Changelog Epics B
- Used Session Count
- Image Release Docs
- Notification Sender Docs
- Trainer Status
- Session State Enum
- Counting Encoder
- Graphify Rules Docs
- Fixed Clock Helper
- Release 2.0.1
- Maven POM

## God Nodes (most connected - your core abstractions)
1. `Trainer` - 214 edges
2. `TestPostgres` - 194 edges
3. `Rider` - 190 edges
4. `Session` - 181 edges
5. `AuthUser` - 176 edges
6. `ApiException` - 168 edges
7. `RiderRepository` - 157 edges
8. `SessionTemplate` - 140 edges
9. `SessionRepository` - 138 edges
10. `TrainerRepository` - 121 edges

## Surprising Connections (you probably didn't know these)
- `create-super-admin.sql` --semantically_similar_to--> `APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH`  [INFERRED] [semantically similar]
  ops/README.md → README.md
- `graphify skill (/graphify trigger)` --conceptually_related_to--> `graphify usage rules`  [INFERRED]
  .claude/CLAUDE.md → CLAUDE.md
- `APP_RIDER_BASE_URL` --semantically_similar_to--> `APP_BASE_URL`  [INFERRED] [semantically similar]
  CHANGELOG.md → README.md
- `E10 Release packaging` --conceptually_related_to--> `Docker image (Java 21 JRE + app.jar)`  [INFERRED]
  CHANGELOG.md → README.md
- `Tests never send real email (app.mail.mode=log)` --references--> `APP_MAIL_MODE (smtp|log)`  [INFERRED]
  CLAUDE.md → README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Multi-tenant club isolation (RLS, equis_app role, test club context)** — readme_row_level_security, readme_equis_app_role, claude_default_club_in_tests, claude_defaultclubextension, claude_tenantcontext_runas, claude_clubthreads [INFERRED 0.85]
- **Test-time mail handling** — claude_mailtestconfig, claude_recordingnotificationsender, claude_lognotificationsender, claude_accountlinkmaillistener, claude_email_tag_tests [EXTRACTED 1.00]
- **Platform operator setup via ops scripts** — ops_readme_create_super_admin, ops_readme_platform_login, ops_readme_upsert_plan, ops_readme_plan_tiers [INFERRED 0.85]

## Communities (205 total, 124 thin omitted)

### Community 0 - "Web Controllers & MVC Imports"
Cohesion: 0.04
Nodes (36): AuthUser, AuthController, AdminAssessmentSlotController, AdminBookingController, AdminMemberController, AssessmentSlotController, MyLevelsController, AdminPaymentController (+28 more)

### Community 2 - "Session Templates & Test Asserts"
Cohesion: 0.09
Nodes (15): Direction, DOWN, UP, LevelChanged, SessionTemplate, LevelRepository, SessionTemplateRepository, HibernateClock (+7 more)

### Community 3 - "Subscriptions & Club Onboarding"
Cohesion: 0.04
Nodes (39): Admin, AuthProperties, Invite, Jwt, Lockout, Refresh, Reset, Outcome (+31 more)

### Community 4 - "Admin Oversight Journeys"
Cohesion: 0.07
Nodes (8): AdminOversightAccessTest, BookingOversightJourneyTest, NoShowRebooked, ReminderJourneyTest, RiderReassignJourneyTest, AdminOversightFixture, Coach, Schedule

### Community 5 - "Admin Member API Tests"
Cohesion: 0.06
Nodes (5): AdminMemberApiTest, AdminMemberCloseApiTest, BookingReminderApiTest, NoticeRow, Request

### Community 6 - "Error Codes & Validation"
Cohesion: 0.07
Nodes (12): ApiException, ApiFieldError, Constraints, ErrorCodes, VoidPaymentRequest, RiderSessionPage, SessionTemplateCreateRequest, Levels (+4 more)

### Community 7 - "Rider/Trainer Account Journeys"
Cohesion: 0.07
Nodes (6): AuthTokens, ClubRef, TrainerDisableJourneyTest, TrainerResetJourneyTest, RiderAccountJourneyTest, TrainerAccountsFixture

### Community 8 - "Assessment Results"
Cohesion: 0.06
Nodes (8): RecordResultRequest, ResultRecorded, BookingControllerTest, AssessmentBookingTest, AssessmentBookingRepositoryTest, AssessmentRulesTest, FixedClock, RecordResultServiceTest

### Community 9 - "Evaluation History & Security"
Cohesion: 0.06
Nodes (26): AuthUserConverter, MyRidersController, RiderEvaluations, TrainerRepository, EvaluationHistoryRow, SessionEvaluationRepository, SuperAdminRepository, LevelChangeRepository (+18 more)

### Community 10 - "Scenario Journey Tests"
Cohesion: 0.10
Nodes (5): SessionEvaluationsScenarioTest, SharedSession, SameDaySessionsScenarioTest, Check, SameDayJourneys

### Community 11 - "Roles, Clubs & Threads"
Cohesion: 0.20
Nodes (8): Role, ADMIN, RIDER, SUPER_ADMIN, TRAINER, Club, JwtService, ClubThreads

### Community 12 - "Admin Trainer & Mail Tests"
Cohesion: 0.05
Nodes (4): AdminTrainerActionsApiTest, AdminTrainerApiTest, AccountLinkMailListenerTest, MailTestConfig

### Community 14 - "Platform Club & Invite Tests"
Cohesion: 0.06
Nodes (7): OutgoingEmail, Request, TrainerAdminAccessTest, Request, TrainerInviteJourneyTest, PlatformClubApiTest, RecordingNotificationSender

### Community 15 - "Rider Evaluation DTOs"
Cohesion: 0.06
Nodes (8): RiderEvaluation, SeatEvaluation, LevelHistoryEntry, RiderLevelDto, UpcomingSeat, RiderPaymentStatus, ACTIVE, AWAITING_PAYMENT

### Community 16 - "Exception Handler & Rules Tests"
Cohesion: 0.05
Nodes (11): AccessToken, AuthPropertiesTest, JwtConfigTest, ApiExceptionHandlerTest, CancelReasonTest, MemberStatusTest, PaymentEnumsTest, SeedDataTest (+3 more)

### Community 17 - "Assessment Booking Service"
Cohesion: 0.06
Nodes (14): BookingController, UpdateMemberRequest, AssessmentBooking, Member, AssessmentBookingRepository, MemberRepository, NoticeRepository, AdminBookingRowAssembler (+6 more)

### Community 18 - "Sessions & Assignments"
Cohesion: 0.08
Nodes (10): LevelRef, Assignment, Session, SessionStatus, CANCELLED, DONE, FINISHED, SCHEDULED (+2 more)

### Community 19 - "Assessment Slot Query"
Cohesion: 0.09
Nodes (10): AssessmentSlotDayDto, AssessmentSlotDto, AssessmentSlotsDto, BookingRowDto, SlotDetailDto, SlotBookedCount, AssessmentSlotMapper, AssessmentSlotControllerTest (+2 more)

### Community 20 - "Security Beans & Config"
Cohesion: 0.08
Nodes (8): ClockConfig, JwtConfig, SecurityBeans, MyLevelSummary, TrainerStatus, ACTIVE, DISABLED, INVITED

### Community 21 - "Admin Rider Rows & Reassign"
Cohesion: 0.07
Nodes (20): TrainerRef, AdminRiderList, AdminRiderRow, RiderTotals, TrainerCard, BookingStatus, CANCELLED, NOSHOW (+12 more)

### Community 24 - "Rider App & Payments Query"
Cohesion: 0.07
Nodes (19): ExpiringSoon, RiderAssessment, RiderBanners, RiderMe, RiderPackage, RiderPayment, RiderPayments, RiderProgress (+11 more)

### Community 25 - "My Levels & School Week"
Cohesion: 0.07
Nodes (15): DateRanges, SchoolWeek, Day, LevelSessionsResponse, Seat, SessionSlot, MyWeek, WeekDay (+7 more)

### Community 26 - "JPA Entities Core"
Cohesion: 0.23
Nodes (7): TenantGuard, AdminAccount, RefreshToken, Notice, SessionUsage, Subscription, SuperAdmin

### Community 28 - "Admin Overview & Bookings"
Cohesion: 0.08
Nodes (14): AdminOverviewController, AdminBookingList, BookingTotals, ActivityItem, Attention, OverviewResponse, PendingInvite, TodaySession (+6 more)

### Community 29 - "Trainer Admin Service"
Cohesion: 0.07
Nodes (12): AdminTrainerController, InviteTrainerRequest, LevelCount, PendingLink, RiderCounts, TrainerList, TrainerRow, TrainerTotals (+4 more)

### Community 30 - "Auth User Conversion"
Cohesion: 0.09
Nodes (3): AuthUserAuthentication, SecurityConfigTest, AuthUserConverterTest

### Community 32 - "Session Evaluation API Tests"
Cohesion: 0.12
Nodes (5): Request, SessionEvaluationApiTest, State, CANCELLED, SCHEDULED

### Community 33 - "Template Delete Impact"
Cohesion: 0.17
Nodes (4): DeleteImpact, SessionsCancelledEvent, SessionTemplateService, SessionTemplateServiceTest

### Community 35 - "Auth Service & Invites"
Cohesion: 0.13
Nodes (4): InviteAccepted, InviteAcceptRequest, LoginRequest, AuthServiceTest

### Community 36 - "Rider Accounts Scenario"
Cohesion: 0.17
Nodes (4): OmarAndSara, Person, RiderAccountsScenarioTest, RiderLogin

### Community 37 - "Finish Session API Tests"
Cohesion: 0.14
Nodes (5): FinishSessionApiTest, Request, State, CANCELLED, SCHEDULED

### Community 38 - "Phone Normalizer & Params"
Cohesion: 0.11
Nodes (3): PhoneNormalizerTest, LevelRepositoryTest, BookingActionsTest

### Community 43 - "Flyway Migration Tests"
Cohesion: 0.13
Nodes (4): ProdDemoDataCleanupTest, ApplicantIntakeMigrationTest, RiderAccountMigrationTest, SessionFinishedMigrationTest

### Community 44 - "Level Change Model"
Cohesion: 0.12
Nodes (4): Level, LevelChange, LevelChangeTest, LevelChangeRepositoryTest

### Community 47 - "Rider Entity"
Cohesion: 0.12
Nodes (3): Rider, NoSessionsLeftException, RiderTest

### Community 51 - "My Riders & Balance"
Cohesion: 0.18
Nodes (4): BalanceDto, MyRiderDto, FixedClock, MyRidersQueryServiceTest

### Community 53 - "Template Rules & Warnings"
Cohesion: 0.18
Nodes (5): OverlapRef, SessionTemplateInput, SessionTemplateRules, Warning, SessionTemplateRulesTest

### Community 55 - "Release Workflow"
Cohesion: 0.07
Nodes (29): DIGEST, IMAGE, TAG, jobs, publish-image, name, on, release (+21 more)

### Community 57 - "Rider Balance Service"
Cohesion: 0.15
Nodes (4): PackageBalance, RiderBalance, FixedClock, RiderBalanceServiceTest

### Community 62 - "Jobs & Event Listeners"
Cohesion: 0.13
Nodes (5): BookingCancellationListener, LogNotificationSender, AssignmentCancellationListener, SessionGenerationJob, SessionGenerator

### Community 63 - "Endpoint Isolation Tests"
Cohesion: 0.13
Nodes (13): Case, EndpointIsolationTest, Ids, Kind, LIST, OTHER, OWN, UNKNOWN (+5 more)

### Community 64 - "Tenant Context"
Cohesion: 0.13
Nodes (4): TenantContext, TenantContextTest, TenantFilterTest, jakarta.servlet.http.HttpServlet

### Community 66 - "Converters & Slot Labels"
Cohesion: 0.13
Nodes (3): DayOfWeekConverter, DbConverter, SlotLabels

### Community 67 - "My Sessions & Evaluation"
Cohesion: 0.17
Nodes (3): EvaluationRequest, SessionAccess, MySessionsControllerTest

### Community 69 - "Tenant-Aware DataSource"
Cohesion: 0.11
Nodes (3): TenantAutoConfiguration, BeanPostProcessor, TenantAwareDataSource

### Community 70 - "Row-Level Security Tests"
Cohesion: 0.14
Nodes (3): RowLevelSecurityTest, Config, TwoClubFixture

### Community 71 - "Account Link Events"
Cohesion: 0.16
Nodes (6): DbConverter, InvitePurpose, INVITE, RESET, AccountLinkCreatedEvent, AccountLinkEmailsTest

### Community 74 - "Booking Actions"
Cohesion: 0.11
Nodes (11): BookingAction, CANCEL, CHANGE_ASSESSOR, CLOSE, EDIT, REBOOK, REMIND, REOPEN (+3 more)

### Community 76 - "Payment State & Usage"
Cohesion: 0.14
Nodes (11): PaymentEntry, PaymentUsage, UsageLine, PaymentEntryAssembler, PaymentState, ACTIVE, EXPIRED, UPCOMING (+3 more)

### Community 78 - "Club Access Views"
Cohesion: 0.11
Nodes (16): ClubPage, ClubRow, BillingPeriod, ANNUAL, MONTHLY, TRIAL, AccessReason, NO_SUBSCRIPTION (+8 more)

### Community 81 - "Prod Environment Check"
Cohesion: 0.16
Nodes (3): ProdEnvironmentCheck, EquisApiApplication, ProdEnvironmentCheckTest

### Community 82 - "Plans & Expiry Policy"
Cohesion: 0.15
Nodes (6): PlanView, OnExpiry, BLOCK, READ_ONLY, Plan, PlanRepository

### Community 89 - "CI Workflow"
Cohesion: 0.09
Nodes (21): env, runs-on, steps, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD_HASH, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (+13 more)

### Community 90 - "Request Validation"
Cohesion: 0.19
Nodes (3): PhoneNormalizer, ClubAdminInput, CreateClubRequest

### Community 91 - "Club Admin & Usage"
Cohesion: 0.19
Nodes (5): ClubAdminService, Entry, ClubFilter, ClubUsageService, Usage

### Community 92 - "Evaluation Follow-Up"
Cohesion: 0.20
Nodes (10): AdminEvaluationController, EvaluationFollowUp, FollowUpTotals, FollowUpTrainer, RiderRef, UnevaluatedSession, UnfinishedSession, EvaluationFollowUpQuery (+2 more)

### Community 102 - "Member Status"
Cohesion: 0.16
Nodes (7): MemberStatus, CLOSED, NO_SHOW, PENDING_ASSESSMENT, RIDER, MemberStatusResolver, MemberStatusResolverTest

### Community 106 - "Roles & Subject Types"
Cohesion: 0.17
Nodes (7): DbConverter, SubjectType, ADMIN, RIDER, SUPER_ADMIN, TRAINER, RoleTest

### Community 107 - "Platform Club Status"
Cohesion: 0.18
Nodes (9): ChangeClubStatusRequest, Admin, ClubDetail, AdminStatus, ACTIVE, INVITED, ClubStatus, ACTIVE (+1 more)

### Community 108 - "My Sessions Controller"
Cohesion: 0.21
Nodes (7): MySessionsController, SessionDetailMapper, AssignmentsRequest, MySeat, RiderOption, SessionDetailResponse, SessionDetail

### Community 114 - "SMTP Sender"
Cohesion: 0.16
Nodes (5): MailSettings, Mode, LOG, SMTP, SmtpNotificationSender

### Community 115 - "Template Mapper"
Cohesion: 0.23
Nodes (7): SessionTemplateMapper, DeleteResponse, Overlap, SaveResponse, Warning, SessionTemplateResponse, SessionTemplateUpdateRequest

### Community 118 - "Account Link Mail"
Cohesion: 0.21
Nodes (4): AccountLinkEmails, Texts, AccountLinkMailListener, NotificationSender

### Community 119 - "Rider Account Service"
Cohesion: 0.31
Nodes (3): AdminRiderAccountController, RiderAccount, RiderAccountService

### Community 124 - "Mail & Scheduling Config"
Cohesion: 0.28
Nodes (3): MailConfig, SchedulingConfig, TestConfig

### Community 125 - "Booked Seat Counts"
Cohesion: 0.22
Nodes (3): AssessmentSeatCounts, BookedSeats, TemplateBookedSeats

### Community 127 - "Cancel Reasons"
Cohesion: 0.21
Nodes (9): CancelReason, FACILITY_CLOSED, HORSE_UNAVAILABLE, OTHER, PERSON_CANCELLED, PERSON_UNWELL, TRAINER_UNAVAILABLE, WEATHER (+1 more)

### Community 128 - "Docker Compose"
Cohesion: 0.17
Nodes (11): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, services (+3 more)

### Community 133 - "Pending Evaluations"
Cohesion: 0.29
Nodes (5): MyEvaluationsController, PendingEvaluations, PendingSession, PendingEvaluationRow, PendingEvaluationQuery

### Community 134 - "Payment Method"
Cohesion: 0.25
Nodes (7): DbConverter, PaymentMethod, BANK_TRANSFER, CARD, CASH, MOBILE_WALLET, OTHER

### Community 139 - "Changelog Epics A"
Cohesion: 0.25
Nodes (9): E10 Release packaging, E2 Session setup, E3 Scheduling riders, E5 Rider levels, E7 Admin oversight, E8 Week view & hardening, E9 Admin console redesign, Production clean-up afterMigrate__remove_demo_data.sql (+1 more)

### Community 140 - "Notice Type"
Cohesion: 0.33
Nodes (4): DbConverter, NoticeType, RESULT_OVERDUE, SESSION_CANCELLED

### Community 141 - "Rider Account Status"
Cohesion: 0.31
Nodes (5): DbConverter, RiderAccountStatus, ACTIVE, INVITED, NONE

### Community 142 - "Base URLs & Accounts Docs"
Cohesion: 0.29
Nodes (8): APP_RIDER_BASE_URL, E15 Rider accounts and data, E1 Auth & accounts, E6 Trainer accounts, APP_BASE_URL, APP_JWT_SECRET, APP_MAIL_MODE (smtp|log), Production environment variables

### Community 143 - "Ops Scripts & Plans Docs"
Cohesion: 0.25
Nodes (8): V5__seed_admin.sql, create-super-admin.sql, on_expiry policy (BLOCK | READ_ONLY), Ops scripts (run by hand), Plans TRIAL / BASIC / PRO / MAX, POST /api/platform/auth/login, upsert-plan.sql, APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD_HASH

### Community 145 - "Club Sort"
Cohesion: 0.25
Nodes (4): ClubSort, CODE, ENDS_AT, NAME

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

### Community 155 - "Session State Enum"
Cohesion: 0.50
Nodes (4): State, CANCELLED, FINISHED, SCHEDULED

## Knowledge Gaps
- **115 isolated node(s):** `com.equestrian:equis-api`, `SMTP`, `LOG`, `TRAINER`, `ADMIN` (+110 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 467 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **124 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Session` connect `Sessions & Assignments` to `Web Controllers & MVC Imports`, `Session Templates & Test Asserts`, `Error Codes & Validation`, `Session Done Rules`, `Assessment Results`, `Evaluation History & Security`, `Roles, Clubs & Threads`, `Test Fixtures & Clock`, `Rider Evaluation DTOs`, `Assessment Booking Service`, `Assessment Slot Query`, `Security Beans & Config`, `Rider App & Payments Query`, `My Levels & School Week`, `JPA Entities Core`, `Repository Layer`, `Admin Overview & Bookings`, `Template Delete Impact`, `Level Change Flow`, `Level Change Service`, `My Riders & Balance`, `Session Usage Service`, `Rider Balance Service`, `Assignment Service`, `Converters & Slot Labels`, `My Sessions & Evaluation`, `Level Change API Tests`, `Session Repository`, `Payment Repository Tests`, `Evaluation Follow-Up`, `Rider Level Query`, `My Sessions Controller`, `Evaluation Repository Tests`, `Assignment Repository`, `Session Detail Service`, `Member Booking Tests`, `Session Evaluation Entity`?**
  _High betweenness centrality (0.035) - this node is a cross-community bridge._
- **What connects `com.equestrian:equis-api`, `SMTP`, `LOG` to the rest of the system?**
  _115 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Web Controllers & MVC Imports` be split into smaller, more focused modules?**
  _Cohesion score 0.04068269643623873 - nodes in this community are weakly interconnected._
- **Why does `TestPostgres` connect `Session Templates & Test Asserts` to `Spring Boot Test Wiring`, `Admin Oversight Journeys`, `Admin Member API Tests`, `Rider/Trainer Account Journeys`, `Assessment Results`, `Evaluation History & Security`, `Scenario Journey Tests`, `Roles, Clubs & Threads`, `Admin Trainer & Mail Tests`, `Test Fixtures & Clock`, `Platform Club & Invite Tests`, `Rider Evaluation DTOs`, `Exception Handler & Rules Tests`, `Sessions & Assignments`, `Assessment Slot Query`, `Security Beans & Config`, `Platform Sign-In & Subs Tests`, `Admin Booking Change Tests`, `My Levels & School Week`, `Payments Scenario Tests`, `Session Evaluation API Tests`, `Template Delete Impact`, `Admin Booking API Tests`, `Auth Service & Invites`, `Rider Accounts Scenario`, `Finish Session API Tests`, `Phone Normalizer & Params`, `Rider Reassign API Tests`, `Plan & Subscription Model`, `Intake Scenario Tests`, `Admin Overview API Tests`, `Flyway Migration Tests`, `Level Change Model`, `Level Change Flow`, `Trainer Access Service`, `Level Change Service`, `Console Finished Sessions`, `Invite Repository Tests`, `My Riders & Balance`, `Rider App API & JWT`, `Template Rules & Warnings`, `Admin Rider API Tests`, `Session Usage Service`, `Rider Balance Service`, `Rider Level Counts`, `Assignment Service`, `My Week API Tests`, `Endpoint Isolation Tests`, `Assign On The Day Tests`, `Level Change API Tests`, `Tenant-Aware DataSource`, `Row-Level Security Tests`, `Evaluation Follow-Up Tests`, `Club Repository Tests`, `Tenant Isolation Tests`, `Session Repository`, `Rider Eval History Tests`, `Payment Void API Tests`, `Payment Repository Tests`, `Per-Club Sign-In Tests`, `Admin Payment API Tests`, `Admin Lockout`, `Trainer Repository`, `Account Link Service`, `Admin Member Edit Tests`, `Rider Balance API Tests`, `Auth Tokens`, `Repository Isolation`, `Member Entity`, `Notices`, `Session Generator`, `Admin Slot API Tests`, `Evaluation Repository Tests`, `Assignment Repository`, `Refresh Tokens`, `Session Detail Service`, `Member Booking Tests`, `Admin Account Repo`?**
  _High betweenness centrality (0.031) - this node is a cross-community bridge._
- **Should `Spring Boot Test Wiring` be split into smaller, more focused modules?**
  _Cohesion score 0.05049580472921434 - nodes in this community are weakly interconnected._
- **Why does `ApiException` connect `Error Codes & Validation` to `Web Controllers & MVC Imports`, `Session Templates & Test Asserts`, `Subscriptions & Club Onboarding`, `Password Service`, `Assessment Results`, `Evaluation History & Security`, `Roles, Clubs & Threads`, `Assessment Booking Service`, `Sessions & Assignments`, `Assessment Slot Query`, `Security Beans & Config`, `Admin Rider Rows & Reassign`, `Platform Sign-In & Subs Tests`, `Rider App & Payments Query`, `My Levels & School Week`, `JPA Entities Core`, `Admin Overview & Bookings`, `Template Delete Impact`, `Auth Service & Invites`, `Template Rules & Warnings`, `Session Usage Service`, `Assignment Service`, `API Exception Handler`, `My Sessions & Evaluation`, `Level Change Rules`, `Evaluation Follow-Up`, `Admin Lockout`, `Account Link Service`, `Rider Level Query`, `Template Controller Tests`, `Rider Account Service`?**
  _High betweenness centrality (0.026) - this node is a cross-community bridge._
- **Should `Session Templates & Test Asserts` be split into smaller, more focused modules?**
  _Cohesion score 0.09427284427284427 - nodes in this community are weakly interconnected._