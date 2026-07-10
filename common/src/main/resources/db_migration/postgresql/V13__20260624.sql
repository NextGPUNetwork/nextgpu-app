CREATE SEQUENCE IF NOT EXISTS provider_attribute_type_seq START WITH 1 INCREMENT BY 1;


CREATE TABLE provider_attribute_type
(
    id               BIGINT DEFAULT nextval('provider_attribute_type_seq') NOT NULL,
    name             VARCHAR(50)                                           NOT NULL,
    datatype         VARCHAR(255),
    description      VARCHAR(255),
    version          INTEGER,
    date_created     TIMESTAMP WITHOUT TIME ZONE                           NOT NULL,
    date_updated     TIMESTAMP WITHOUT TIME ZONE,
    retired          BOOLEAN,
    date_retired     TIMESTAMP WITHOUT TIME ZONE,
    retire_reason    VARCHAR(255),
    uuid             VARCHAR(38)                                           NOT NULL,
    is_mandatory     BOOLEAN,
    is_unique        BOOLEAN,
    validation_regex VARCHAR(255),
    CONSTRAINT pk_provider_attribute_type PRIMARY KEY (id)
);
COMMENT
ON COLUMN provider_attribute_type.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN provider_attribute_type.is_mandatory IS 'Is this attribute type mandatory to save a User entity?';
COMMENT
ON COLUMN provider_attribute_type.is_unique IS 'Can this attribute type be duplicated? True, by default';
COMMENT
ON COLUMN provider_attribute_type.validation_regex IS 'Regular expression to validate the value entered in the respective UserAttribute object';

ALTER TABLE provider_attribute_type
    ADD CONSTRAINT uc_provider_attribute_type_name UNIQUE (name);

ALTER TABLE provider_attribute_type
    ADD CONSTRAINT uc_provider_attribute_type_uuid UNIQUE (uuid);