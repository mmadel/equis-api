-- E18 · export engine: the rows of the Trainers report. Reports read views, never the tables directly, so a report
-- can join and shape its rows without touching the entities the app writes.
-- security_invoker: the view runs with the rights of the caller (equis_app), so the trainer table's row-level
-- security still applies and a club sees only its own trainers, on top of Hibernate's @TenantId filter.
CREATE VIEW report_trainer_v WITH (security_invoker = true) AS
SELECT t.id, t.club_id, t.name, t.email, t.phone, t.status
FROM trainer t;

GRANT SELECT ON report_trainer_v TO equis_app;
