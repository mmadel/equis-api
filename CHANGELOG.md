# Changelog

## [3.0.0] - 2026-11-08

- E17 · Multi-tenancy: one deployment serves many clubs. Every school table has a club_id, isolated by Hibernate @TenantId and PostgreSQL row-level security; the existing data becomes the default club.
- Platform super admin (created with ops/create-super-admin.sql): signs in on POST /platform/auth/login, creates clubs (the first admin gets a set-password link and the club starts on a trial), adds, lists and voids monthly or annual subscriptions, and suspends or reactivates clubs.
- Per-club sign-in: club code plus email or phone for admins, trainers and riders; email and phone are unique per club. Breaking: sign-in now needs a club code.
- Plans (Trial, Basic, Pro, Max, set with ops/upsert-plan.sql) limit trainers and riders: intake, reopen, trainer invite and trainer enable return PLAN_LIMIT_REACHED at the limit. New GET /admin/club/usage and GET /club/status.
- Club access state: an expired trial, a suspended club or a club with no subscription is blocked (CLUB_BLOCKED); an expired paid subscription is read-only (writes return SUBSCRIPTION_EXPIRED).
- CI: coverage report and a faster test run; mail tests left out of the default run.

### Database

- V23__club_and_club_id.sql, V24__row_level_security.sql, V25__super_admin.sql, V26__plan_and_subscription.sql, V27__admin_invite.sql.
- V24 creates the role equis_app: the user that runs Flyway must be a superuser or have CREATEROLE (the postgres image user is).

## [2.0.2] - 2026-11-04

- Release pipeline: publishing a GitHub Release builds ghcr.io/mmadel/equis-api:<version> from the tag and pushes it to GHCR (GitHub Actions, never tagged latest); the image digest is in the job summary.
- No application code changes.

### Database

- No new Flyway scripts.

## [2.0.1] - 2026-11-04

- No code changes. Version aligned with the 2.0.1 deployment bundle (compose hardening and HTTPS in equis-deploy).

## [2.0.0] - 2026-11-02

- E11 · Admin intake: the admin records new people and books their assessments; trainers no longer register walk-ins.
- E12 · Payments and balance: the admin records and voids payments; each payment adds a package of sessions that expires after a grace month, and every rider's remaining balance is available.
- E13 · Same-day sessions: trainers add riders to a session only on the day, from riders who can ride that day; marking a session Finished uses one session from each rider's balance.
- E14 · Session evaluations: after a Finished session the trainer gives each rider 1–5 stars and an optional comment; the admin sees which riders and sessions still need evaluating, per trainer.
- E15 · Rider accounts and data: every rider gets an email login when they become a rider, and the API serves the rider app's data (status, balance, sessions with evaluations, assessment, payments, profile and progress).
- CI: GitHub Actions builds and tests every pull request.

### Database

- V16__applicant_intake.sql
- V17__member_closed.sql
- V18__assessment_cancel_reason.sql
- V19__payments.sql
- V20__session_finished.sql
- V21__session_evaluation.sql
- V22__rider_accounts.sql

### Configuration

- APP_RIDER_BASE_URL: public address of the rider app, used in rider invite and set-password links (default http://localhost:4202).

## 1.0.0 — 2026-10-19

First release.

### E1 · Auth & accounts
- Trainers and the admin sign in with short-lived access tokens and refresh tokens.
- Invited trainers accept their invite and set a password.
- Repeated failed admin sign-ins lock the account for a while.
- Riding levels are stored and seeded.
- All errors share one format with an error code and per-field messages.

### E2 · Session setup
- The admin creates, edits and deletes weekly session templates per level and for assessments.
- Dated sessions are generated 8 weeks ahead by a daily job, with catch-up on startup.
- Session rules: end after start, capacity 1–12 and never below seats already booked, warning on overlaps.
- Deleting a template removes its future sessions from the trainer app.

### E3 · Scheduling riders
- Trainers fill and free seats in a session with their own riders of the matching level.
- Capacity is enforced and seats held by other trainers are protected.

### E4 · Assessment
- Trainers register walk-in members and book them into an assessment slot.
- Recording the result places the member as a rider at the chosen level.

### E5 · Rider levels
- Trainers list their riders and promote or demote a rider one level with a reason.
- Each rider has a level history, and seats at the old level are freed on a change.

### E6 · Trainer accounts
- The admin lists and invites trainers.
- The admin can resend an invite, send a password reset, disable, enable or cancel an invite.
- Invite and set-password emails are sent.
- Disabling a trainer revokes their access.

### E7 · Admin oversight
- The admin lists every assessment booking with totals, search and sort.
- The admin reschedules, rebooks or changes the assessor of a booking.
- The admin reminds an assessor of an overdue result with an in-app notice.
- The admin lists riders with their trainer, assessor and how they were assigned.
- The admin changes a rider's trainer one at a time or in bulk, or resets it to the assessor.

### E8 · Week view & hardening
- Trainers see a Saturday-to-Friday week of every level's sessions.

### E9 · Admin console redesign
- The admin Overview shows today's sessions, week fill by track, items needing attention and recent trainer activity.

### E10 · Release packaging
- The API ships as a Docker image with a production profile.
- Demo and test data is removed automatically in production.

### Database scripts
- V1__create_level.sql
- V2__seed_levels.sql
- V3__create_accounts.sql
- V4__seed_test_trainers.sql
- V5__seed_admin.sql
- V6__create_refresh_token.sql
- V7__create_session_template.sql
- V8__seed_sample_templates.sql
- V9__create_session.sql
- V10__create_rider.sql
- V11__create_assignment.sql
- V12__seed_sample_riders.sql
- V13__create_member_and_assessment_booking.sql
- V14__create_level_change.sql
- V15__create_notice.sql
- Production clean-up: db/prod/afterMigrate__remove_demo_data.sql

### Not included
- Trainer change history.
- Self-service password reset.
- SMS invites.
