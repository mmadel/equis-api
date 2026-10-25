-- E12 · payment packages and session usage
CREATE TABLE payment (
    id            uuid          NOT NULL DEFAULT gen_random_uuid(),
    rider_id      uuid          NOT NULL,
    amount_egp    numeric(10,2) NOT NULL,
    method        varchar(20)   NOT NULL,
    start_month   date          NOT NULL,   -- always the 1st of the month
    months        smallint      NOT NULL,
    sessions      smallint      NOT NULL,
    valid_until   date          NOT NULL,   -- last day of the grace month
    recorded_at   timestamptz   NOT NULL DEFAULT now(),
    recorded_by   uuid          NOT NULL,
    voided_at     timestamptz,
    voided_by     uuid,
    void_reason   varchar(300),
    version       bigint        NOT NULL DEFAULT 0,
    CONSTRAINT pk_payment                PRIMARY KEY (id),
    CONSTRAINT uq_payment_id_rider       UNIQUE (id, rider_id),          -- target of session_usage's composite FK
    CONSTRAINT fk_payment_rider          FOREIGN KEY (rider_id)    REFERENCES rider (id),
    CONSTRAINT fk_payment_recorded_by    FOREIGN KEY (recorded_by) REFERENCES admin_account (id),
    CONSTRAINT fk_payment_voided_by      FOREIGN KEY (voided_by)   REFERENCES admin_account (id),
    CONSTRAINT ck_payment_amount         CHECK (amount_egp > 0),
    CONSTRAINT ck_payment_method         CHECK (method IN ('cash','card','bank_transfer','mobile_wallet','other')),
    CONSTRAINT ck_payment_start_month    CHECK (extract(day FROM start_month) = 1),
    CONSTRAINT ck_payment_months         CHECK (months BETWEEN 1 AND 12),
    CONSTRAINT ck_payment_sessions       CHECK (sessions BETWEEN 1 AND 200),
    CONSTRAINT ck_payment_valid_until    CHECK (valid_until = (start_month + make_interval(months => months + 1))::date - 1),
    CONSTRAINT ck_payment_void           CHECK (
        (voided_at IS NULL AND voided_by IS NULL AND void_reason IS NULL)
        OR (voided_at IS NOT NULL AND voided_by IS NOT NULL AND length(trim(void_reason)) > 0))
);

-- Balance and history read a rider's packages; the balance only the live ones.
CREATE INDEX ix_payment_rider_live ON payment (rider_id, valid_until) WHERE voided_at IS NULL;

CREATE TABLE session_usage (
    id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    rider_id    uuid        NOT NULL,
    payment_id  uuid        NOT NULL,
    session_id  uuid        NOT NULL,
    used_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_session_usage               PRIMARY KEY (id),
    CONSTRAINT uq_session_usage_rider_session UNIQUE (rider_id, session_id),
    CONSTRAINT fk_session_usage_payment       FOREIGN KEY (payment_id, rider_id) REFERENCES payment (id, rider_id),
    CONSTRAINT fk_session_usage_session       FOREIGN KEY (session_id) REFERENCES session (id)
);

CREATE INDEX ix_session_usage_payment ON session_usage (payment_id);
