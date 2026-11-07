-- E17 · plans and the periods each club is covered by one. Global platform tables, like club and super_admin:
-- no club_id column on plan, no row-level security, no @TenantId. subscription.club_id is a plain foreign key.
-- Plans are created only by hand with ops/upsert-plan.sql. The app role gets its rights from the default
-- privileges of V24.

-- One row per plan. NULL limits mean unlimited. The trial plan is the one with trial_days set; there is at most one.
-- The code never checks a plan by its code: behaviour comes from these columns.
CREATE TABLE plan (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    code         varchar(20) NOT NULL,
    name         varchar(50) NOT NULL,
    max_trainers int,
    max_riders   int,
    trial_days   int,
    on_expiry    varchar(10) NOT NULL,
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_plan_code UNIQUE (code),
    CONSTRAINT ck_plan_code CHECK (code ~ '^[A-Z][A-Z_]{1,19}$'),
    CONSTRAINT ck_plan_max_trainers CHECK (max_trainers IS NULL OR max_trainers > 0),
    CONSTRAINT ck_plan_max_riders CHECK (max_riders IS NULL OR max_riders > 0),
    CONSTRAINT ck_plan_trial_days CHECK (trial_days IS NULL OR trial_days > 0),
    CONSTRAINT ck_plan_on_expiry CHECK (on_expiry IN ('BLOCK', 'READ_ONLY'))
);
CREATE UNIQUE INDEX uq_plan_single_trial ON plan ((trial_days IS NOT NULL)) WHERE trial_days IS NOT NULL;

-- One row per period a club is covered. Never edited: a wrong row is voided with a reason and stays in the history.
-- created_by / voided_by hold a super admin id, with no foreign key (like refresh_token.subject_id).
-- Which plan goes with which billing period is checked in Subscription.create (a CHECK can't read the plan row).
CREATE TABLE subscription (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    club_id        uuid         NOT NULL REFERENCES club (id),
    plan_id        uuid         NOT NULL REFERENCES plan (id) ON DELETE RESTRICT,
    billing_period varchar(10)  NOT NULL,
    starts_at      timestamptz  NOT NULL,
    ends_at        timestamptz  NOT NULL,
    note           varchar(500),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    created_by     uuid,
    voided_at      timestamptz,
    voided_by      uuid,
    void_reason    varchar(500),
    CONSTRAINT ck_subscription_period CHECK (billing_period IN ('MONTHLY', 'ANNUAL', 'TRIAL')),
    CONSTRAINT ck_subscription_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_subscription_void CHECK ((voided_at IS NULL) = (void_reason IS NULL)),
    CONSTRAINT ck_subscription_void_by CHECK ((voided_at IS NULL) = (voided_by IS NULL)),
    CONSTRAINT ck_subscription_void_reason CHECK (void_reason IS NULL OR length(trim(void_reason)) >= 5)
);
-- One trial per club, ever (a voided trial still counts).
CREATE UNIQUE INDEX uq_subscription_one_trial ON subscription (club_id) WHERE billing_period = 'TRIAL';
-- The current-subscription lookup: latest started, non-voided row of a club.
CREATE INDEX ix_subscription_club_start ON subscription (club_id, starts_at DESC) WHERE voided_at IS NULL;
