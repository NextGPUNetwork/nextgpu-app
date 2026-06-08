CREATE SEQUENCE IF NOT EXISTS connection_session_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE connection_session
(
    id                BIGINT DEFAULT nextval('connection_session_seq') NOT NULL,
    name              VARCHAR(255),
    date_created      TIMESTAMP WITHOUT TIME ZONE                      NOT NULL,
    voided            BOOLEAN,
    date_updated      TIMESTAMP WITHOUT TIME ZONE,
    date_voided       TIMESTAMP WITHOUT TIME ZONE,
    void_reason       VARCHAR(255),
    description       VARCHAR(255),
    uuid              VARCHAR(38)                                      NOT NULL,
    consumer_id       BIGINT,
    provider_pc_id    BIGINT,
    start_time        TIMESTAMP WITHOUT TIME ZONE                      NOT NULL,
    end_time          TIMESTAMP WITHOUT TIME ZONE,
    disconnect_reason SMALLINT,
    CONSTRAINT pk_connection_session PRIMARY KEY (id)
);
COMMENT
ON COLUMN connection_session.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN connection_session.provider_pc_id IS 'The computer of the provider, providing the service to the given Consumer';
COMMENT
ON COLUMN connection_session.end_time IS 'The time when the session ended, null while the session is active';
COMMENT
ON COLUMN connection_session.disconnect_reason IS 'The reason for the disconnection, if any';

ALTER TABLE connection_session
    ADD CONSTRAINT uc_connection_session_uuid UNIQUE (uuid);

ALTER TABLE connection_session
    ADD CONSTRAINT FK_CONNECTION_SESSION_ON_CONSUMER FOREIGN KEY (consumer_id) REFERENCES consumer (id);

ALTER TABLE connection_session
    ADD CONSTRAINT FK_CONNECTION_SESSION_ON_PROVIDER_PC FOREIGN KEY (provider_pc_id) REFERENCES computer (id);