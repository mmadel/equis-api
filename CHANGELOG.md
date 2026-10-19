# Changelog

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
