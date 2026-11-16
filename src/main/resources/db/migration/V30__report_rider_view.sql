-- E19 · Rider balances report: the club's riders with the names the file shows. The balances are not here: they
-- come from RiderBalanceService in Java, so the report uses the same rules as the Riders screen and the apps.
-- security_invoker: the view runs with the rights of the caller (equis_app), so the row-level security of rider and
-- trainer still applies and a club sees only its own riders.
-- Every rider is listed: applicants and closed people are members without a rider row, and a rider can't be closed.
-- trainer_name is null when the rider has no trainer.
CREATE VIEW report_rider_v WITH (security_invoker = true) AS
SELECT r.id,
       r.club_id,
       r.name  AS rider_name,
       t.name  AS trainer_name
FROM rider r
LEFT JOIN trainer t ON t.id = r.trainer_id;

GRANT SELECT ON report_rider_v TO equis_app;
