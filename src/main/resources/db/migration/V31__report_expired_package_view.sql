-- E19 · Expired sessions report: the live packages that still have unused sessions, with the names the file shows.
-- Whether a package has already expired depends on today, so the report filters on valid_until in Java.
-- security_invoker: the view runs with the rights of the caller (equis_app), so the row-level security of payment,
-- session_usage, rider and trainer still applies and a club sees only its own packages.
-- Voided packages are never listed; a package used to the last session lost nothing and is left out.
-- trainer_name is the rider's trainer today; null when unassigned.
CREATE VIEW report_expired_package_v WITH (security_invoker = true) AS
SELECT p.id,
       p.club_id,
       r.name                           AS rider_name,
       t.name                           AS trainer_name,
       p.start_month,
       p.months::int                    AS months,     -- smallint in payment; int in the report row
       p.valid_until,
       p.sessions::int                  AS sessions,
       COALESCE(u.used, 0)              AS used,
       p.sessions - COALESCE(u.used, 0) AS expired
FROM payment p
JOIN rider r        ON r.id = p.rider_id
LEFT JOIN trainer t ON t.id = r.trainer_id
LEFT JOIN (SELECT payment_id, count(*)::int AS used
           FROM session_usage
           GROUP BY payment_id) u ON u.payment_id = p.id
WHERE p.voided_at IS NULL
  AND p.sessions > COALESCE(u.used, 0);

GRANT SELECT ON report_expired_package_v TO equis_app;
