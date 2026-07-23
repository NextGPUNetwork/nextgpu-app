-- Convert anomaly_report.anomaly to JSON.
--
-- Kept in lockstep with the postgresql V17 migration. H2 databases created by
-- V9 already have this column as JSON, so this is effectively a no-op; it exists
-- so any H2 instance whose column predates the JSON mapping is realigned.
ALTER TABLE anomaly_report
    ALTER COLUMN anomaly SET DATA TYPE JSON;
