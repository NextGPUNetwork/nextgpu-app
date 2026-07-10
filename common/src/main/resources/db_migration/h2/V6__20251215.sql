-- Staff
CREATE SEQUENCE IF NOT EXISTS staff_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS staff
(
    id           BIGINT NOT NULL DEFAULT NEXT VALUE FOR staff_seq,
    username     VARCHAR(255)                            NOT NULL,
    role         VARCHAR(255),
    name         VARCHAR(50),
    date_created TIMESTAMP             NOT NULL,
    voided       BOOLEAN,
    date_updated TIMESTAMP,
    date_voided  TIMESTAMP,
    void_reason  VARCHAR(255),
    description  VARCHAR(255),
    uuid         VARCHAR(38)                             NOT NULL,
    CONSTRAINT pk_staff PRIMARY KEY (id)
);

ALTER TABLE staff
    ADD CONSTRAINT uc_staff_username UNIQUE (username);

ALTER TABLE staff
    ADD CONSTRAINT uc_staff_uuid UNIQUE (uuid);

COMMENT
ON COLUMN staff.username IS 'Username chosen by the User. Should be unique';
COMMENT
ON COLUMN staff.uuid IS 'Globally unique identifier';

-- Constraints on different tables
ALTER TABLE global_property
    ADD CONSTRAINT uc_global_property_uuid UNIQUE (uuid);

ALTER TABLE global_property
    ADD CONSTRAINT unique_name UNIQUE (name);

ALTER TABLE computer_attribute_type
    ADD CONSTRAINT uc_computer_attribute_type_name UNIQUE (name);

ALTER TABLE cpu
    ADD CONSTRAINT uk_cpu_manufacturer UNIQUE (manufacturer, model);

ALTER TABLE gpu
    ADD CONSTRAINT uk_gpu_product_identifier_model UNIQUE (manufacturer, model);

ALTER TABLE memory_module
    ADD CONSTRAINT uk_memory_module_product_identifier_model UNIQUE (manufacturer, model);

ALTER TABLE network_device
    ADD CONSTRAINT uk_network_device_product_identifier_model UNIQUE (manufacturer, model);

ALTER TABLE storage
    ADD CONSTRAINT uk_storage_product_identifier_model UNIQUE (manufacturer, model);


