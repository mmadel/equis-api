-- E19 · Payments report: one row per payment, voided ones included, with the names the file shows.
-- security_invoker: the view runs with the rights of the caller (equis_app), so the row-level security of payment,
-- rider, trainer and admin_account still applies and a club sees only its own payments.
-- trainer_name is the rider's trainer today (there is no history of trainer changes); null when unassigned.
-- recorded_by_name falls back to the email, although admin_account.name is required since V27.
CREATE VIEW report_payment_v WITH (security_invoker = true) AS
SELECT p.id,
       p.club_id,
       p.recorded_at,
       r.name                         AS rider_name,
       t.name                         AS trainer_name,
       p.amount_egp,
       p.method,
       p.start_month,
       p.months::int                  AS months,     -- smallint in payment; int in the report row
       p.sessions::int                AS sessions,
       (p.voided_at IS NOT NULL)      AS voided,
       p.void_reason,
       COALESCE(a.name, a.email)      AS recorded_by_name
FROM payment p
JOIN rider r             ON r.id = p.rider_id
LEFT JOIN trainer t      ON t.id = r.trainer_id
JOIN admin_account a     ON a.id = p.recorded_by;

GRANT SELECT ON report_payment_v TO equis_app;
