CREATE SEQUENCE IF NOT EXISTS hardware_report_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE hardware_report
(
    id           BIGINT                      NOT NULL,
    date_created TIMESTAMP NOT NULL,
    description  VARCHAR(255),
    elapsed_time BIGINT,
    uuid         VARCHAR(38)                 NOT NULL,
    computer_id  BIGINT,
    CONSTRAINT pk_hardware_report PRIMARY KEY (id)
);
COMMENT ON COLUMN hardware_report.uuid IS 'Globally unique identifier';

ALTER TABLE hardware_report
    ADD CONSTRAINT uc_hardware_report_uuid UNIQUE (uuid);

ALTER TABLE hardware_report
    ADD CONSTRAINT FK_HARDWARE_REPORT_ON_COMPUTER FOREIGN KEY (computer_id) REFERENCES computer (id);
