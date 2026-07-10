ALTER TABLE hardware_report
DROP
CONSTRAINT fk_hardware_report_on_computer;

CREATE SEQUENCE IF NOT EXISTS global_property_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS global_property
(
    id              BIGINT NOT NULL DEFAULT NEXT VALUE FOR global_property_seq,
    name            VARCHAR(50),
    datatype        VARCHAR(255),
    description     VARCHAR(255),
    version         INTEGER,
    date_created    TIMESTAMP NOT NULL,
    date_updated    TIMESTAMP,
    retired         BOOLEAN,
    date_retired    TIMESTAMP,
    retire_reason   VARCHAR(255),
    uuid            VARCHAR(38) NOT NULL,
    value_reference VARCHAR(8192),
    CONSTRAINT pk_global_property PRIMARY KEY (id)
);
