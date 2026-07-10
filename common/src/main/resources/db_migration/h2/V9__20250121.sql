CREATE SEQUENCE IF NOT EXISTS anomaly_report_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE anomaly_report
(
    id              BIGINT DEFAULT NEXT VALUE FOR anomaly_report_seq NOT NULL,
    date_created    TIMESTAMP                  NOT NULL,
    description     VARCHAR(255),
    elapsed_time    BIGINT,
    uuid            VARCHAR(38)                                  NOT NULL,
    computer_id     BIGINT,
    anomaly         JSON,
    CONSTRAINT pk_anomaly_report PRIMARY KEY (id)
);
COMMENT
ON COLUMN anomaly_report.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN anomaly_report.computer_id IS 'The computer to generate the report for';

ALTER TABLE anomaly_report
    ADD CONSTRAINT uc_anomaly_report_uuid UNIQUE (uuid);

ALTER TABLE anomaly_report
    ADD CONSTRAINT FK_ANOMALY_REPORT_ON_COMPUTER FOREIGN KEY (computer_id) REFERENCES computer (id);
