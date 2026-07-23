-- Convert anomaly_report.anomaly to jsonb.
--
-- Databases created via Hibernate ddl-auto (before the entity mapped this field
-- as JSON) have this column as varchar(5000). The entity now maps it as jsonb,
-- and Postgres will not implicitly cast varchar -> jsonb, so an explicit USING
-- clause is required. NULLIF guards against empty-string rows, which are not
-- valid JSON. The type check makes this a no-op on databases where V9 already
-- created the column as jsonb.
DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'anomaly_report'
                     AND column_name = 'anomaly'
                     AND data_type <> 'jsonb') THEN
            ALTER TABLE anomaly_report
                ALTER COLUMN anomaly TYPE jsonb USING NULLIF(anomaly, '')::jsonb;
        END IF;
    END
$$;
