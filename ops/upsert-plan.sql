-- Creates a plan, or updates it when the code already exists. Run it by hand; Flyway never runs it.
--   psql "$DATABASE_URL" -v code=BASIC -v name=Basic -v trainers=2 -v riders=50 \
--        -v trial_days=NULL -v on_expiry=READ_ONLY -f ops/upsert-plan.sql
-- Unlimited: -v trainers=NULL -v riders=NULL. Paid plans: -v trial_days=NULL.
-- There is no delete script: a plan used by a subscription can't be removed.
INSERT INTO plan (code, name, max_trainers, max_riders, trial_days, on_expiry)
VALUES (upper(:'code'), :'name', :trainers, :riders, :trial_days, upper(:'on_expiry'))
ON CONFLICT (code) DO UPDATE
SET name         = EXCLUDED.name,
    max_trainers = EXCLUDED.max_trainers,
    max_riders   = EXCLUDED.max_riders,
    trial_days   = EXCLUDED.trial_days,
    on_expiry    = EXCLUDED.on_expiry,
    updated_at   = now();
