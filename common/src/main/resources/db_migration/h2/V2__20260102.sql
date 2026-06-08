CREATE SEQUENCE benchmark_report_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE computer_attribute_type_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE computer_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE consumer_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE cpu_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE generic_component_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE global_property_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE gpu_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE hardware_report_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE memory_module_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE network_device_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE nonce_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE provider_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE staff_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE storage_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE benchmark_report
(
    id                        BIGINT      NOT NULL,
    date_created              TIMESTAMP   NOT NULL,
    description               VARCHAR(255),
    elapsed_time              BIGINT,
    uuid                      VARCHAR(38) NOT NULL,
    provider_id               BIGINT,
    computer_id               BIGINT,
    cpu_benchmark_results     CLOB,
    gpu_benchmark_results     CLOB,
    memory_benchmark_results  CLOB,
    storage_benchmark_results CLOB,
    network_benchmark_results CLOB,
    other_benchmark_results   CLOB,
    CONSTRAINT pk_benchmark_report PRIMARY KEY (id)
);
COMMENT
ON COLUMN benchmark_report.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN benchmark_report.provider_id IS 'The provider of computer whose benchmark is done';
COMMENT
ON COLUMN benchmark_report.computer_id IS 'The computer whose benchmark is done';

CREATE TABLE computer
(
    id               BIGINT DEFAULT nextval('computer_seq') NOT NULL,
    name             VARCHAR(255),
    date_created     TIMESTAMP                              NOT NULL,
    voided           BOOLEAN,
    date_updated     TIMESTAMP,
    date_voided      TIMESTAMP,
    void_reason      VARCHAR(255),
    description      VARCHAR(255),
    uuid             VARCHAR(38)                            NOT NULL,
    provider_id      BIGINT,
    type             VARCHAR(50),
    operating_system VARCHAR(255),
    CONSTRAINT pk_computer PRIMARY KEY (id)
);
COMMENT
ON COLUMN computer.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN computer.provider_id IS 'The provider of computer';
COMMENT
ON COLUMN computer.type IS 'The type of the computer (e.g., Laptop, Desktop, Workstation)';
COMMENT
ON COLUMN computer.operating_system IS 'The operating system installed on this computer, e.g., Windows 11 Pro, Ubuntu 24.04 LTS';

CREATE TABLE computer_attribute
(
    computer_id           BIGINT DEFAULT nextval('computer_seq') NOT NULL,
    attribute_value       VARCHAR(255),
    computer_attribute_id BIGINT                                 NOT NULL,
    CONSTRAINT pk_computer_attribute PRIMARY KEY (computer_id, computer_attribute_id)
);
COMMENT
ON COLUMN computer_attribute.attribute_value IS 'This map collects various attributes of this computer. Anything that cannot be represented as computer parts can be categorized as attribute';

CREATE TABLE computer_attribute_type
(
    id               BIGINT DEFAULT nextval('computer_attribute_type_seq') NOT NULL,
    name             VARCHAR(50)                                           NOT NULL,
    datatype         VARCHAR(255),
    description      VARCHAR(255),
    version          INT,
    date_created     TIMESTAMP                                             NOT NULL,
    date_updated     TIMESTAMP,
    retired          BOOLEAN,
    date_retired     TIMESTAMP,
    retire_reason    VARCHAR(255),
    uuid             VARCHAR(38)                                           NOT NULL,
    is_searchable    BOOLEAN,
    is_mandatory     BOOLEAN,
    is_unique        BOOLEAN,
    validation_regex VARCHAR(255),
    display_order    INT,
    category         VARCHAR(255),
    CONSTRAINT pk_computer_attribute_type PRIMARY KEY (id)
);
COMMENT
ON COLUMN computer_attribute_type.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN computer_attribute_type.is_searchable IS 'Whether this attribute type will be included in search filters or not';
COMMENT
ON COLUMN computer_attribute_type.is_mandatory IS 'Is this attribute type mandatory to save a Computer entity?';
COMMENT
ON COLUMN computer_attribute_type.is_unique IS 'Can this attribute type be duplicated? True, by default';
COMMENT
ON COLUMN computer_attribute_type.validation_regex IS 'Regular expression to validate the value entered in the respective ComputerAttribute object';
COMMENT
ON COLUMN computer_attribute_type.display_order IS 'Useful in UI to define the order in which this attribute will be displayed in forms and reports';
COMMENT
ON COLUMN computer_attribute_type.category IS 'Useful in UI. Combine with displayOrder to create sections on UI interface';

CREATE TABLE computer_cpu
(
    computer_id BIGINT DEFAULT nextval('computer_seq') NOT NULL,
    cpu_id      BIGINT DEFAULT nextval('cpu_seq')      NOT NULL,
    CONSTRAINT pk_computer_cpu PRIMARY KEY (computer_id, cpu_id)
);

CREATE TABLE computer_gpu
(
    computer_id BIGINT DEFAULT nextval('computer_seq') NOT NULL,
    gpu_id      BIGINT DEFAULT nextval('gpu_seq')      NOT NULL,
    CONSTRAINT pk_computer_gpu PRIMARY KEY (computer_id, gpu_id)
);

CREATE TABLE computer_memory
(
    computer_id BIGINT DEFAULT nextval('computer_seq')      NOT NULL,
    memory_id   BIGINT DEFAULT nextval('memory_module_seq') NOT NULL,
    CONSTRAINT pk_computer_memory PRIMARY KEY (computer_id, memory_id)
);

CREATE TABLE computer_nics
(
    computer_id BIGINT DEFAULT nextval('computer_seq')       NOT NULL,
    nic_id      BIGINT DEFAULT nextval('network_device_seq') NOT NULL,
    CONSTRAINT pk_computer_nics PRIMARY KEY (computer_id, nic_id)
);

CREATE TABLE computer_other_component
(
    component_id BIGINT DEFAULT nextval('generic_component_seq') NOT NULL,
    computer_id  BIGINT DEFAULT nextval('computer_seq')          NOT NULL,
    CONSTRAINT pk_computer_other_component PRIMARY KEY (component_id, computer_id)
);

CREATE TABLE computer_storage
(
    computer_id BIGINT DEFAULT nextval('computer_seq') NOT NULL,
    storage_id  BIGINT DEFAULT nextval('storage_seq')  NOT NULL,
    CONSTRAINT pk_computer_storage PRIMARY KEY (computer_id, storage_id)
);

CREATE TABLE consumer
(
    id             BIGINT DEFAULT nextval('consumer_seq') NOT NULL,
    username       VARCHAR(255)                           NOT NULL,
    role           VARCHAR(255),
    name           VARCHAR(255),
    date_created   TIMESTAMP                              NOT NULL,
    voided         BOOLEAN,
    date_updated   TIMESTAMP,
    date_voided    TIMESTAMP,
    void_reason    VARCHAR(255),
    description    VARCHAR(255),
    uuid           VARCHAR(38)                            NOT NULL,
    wallet_address VARCHAR(255)                           NOT NULL,
    CONSTRAINT pk_consumer PRIMARY KEY (id)
);
COMMENT
ON COLUMN consumer.username IS 'Username chosen by the User. Should be unique';
COMMENT
ON COLUMN consumer.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN consumer.wallet_address IS 'Wallet address used to Sign-up';

CREATE TABLE cpu
(
    id                 BIGINT DEFAULT nextval('cpu_seq') NOT NULL,
    manufacturer       VARCHAR(255),
    model              VARCHAR(255),
    year_released      INT,
    is_discontinued    BOOLEAN,
    tdp_watts          INT,
    product_identifier VARCHAR(255),
    name               VARCHAR(255),
    date_created       TIMESTAMP                         NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP,
    date_voided        TIMESTAMP,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                       NOT NULL,
    architecture       VARCHAR(255),
    cores              INT,
    threads            INT,
    min_clock          INT,
    max_clock          INT,
    l3_cache           INT,
    CONSTRAINT pk_cpu PRIMARY KEY (id)
);
COMMENT
ON COLUMN cpu.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT
ON COLUMN cpu.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT
ON COLUMN cpu.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT
ON COLUMN cpu.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT
ON COLUMN cpu.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT
ON COLUMN cpu.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT
ON COLUMN cpu.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN cpu.architecture IS 'The underlying architecture like x86_64, ARM, etc.';
COMMENT
ON COLUMN cpu.cores IS 'Number of physical cores inside the chip package';
COMMENT
ON COLUMN cpu.threads IS 'Number of logical cores. It is usually double the no. of cores if hyper-threading is enabled';
COMMENT
ON COLUMN cpu.min_clock IS 'Minimum CPU frequency. A.k.a. Base Clock in MHz';
COMMENT
ON COLUMN cpu.max_clock IS 'Maximum CPU frequency. A.k.a. Boost/Turbo Clock in MHz';
COMMENT
ON COLUMN cpu.l3_cache IS 'The highest level cache in KBs. Usually, it is level 3 or in some cases, level 4 cache';

CREATE TABLE generic_component
(
    id                  BIGINT DEFAULT nextval('generic_component_seq') NOT NULL,
    manufacturer        VARCHAR(255),
    model               VARCHAR(255),
    year_released       INT,
    is_discontinued     BOOLEAN,
    tdp_watts           INT,
    product_identifier  VARCHAR(255),
    name                VARCHAR(255),
    date_created        TIMESTAMP                                       NOT NULL,
    voided              BOOLEAN,
    date_updated        TIMESTAMP,
    date_voided         TIMESTAMP,
    void_reason         VARCHAR(255),
    description         VARCHAR(255),
    uuid                VARCHAR(38)                                     NOT NULL,
    type                VARCHAR(50),
    specification_key   VARCHAR(50)                                     NOT NULL,
    specification_value VARCHAR(255),
    CONSTRAINT pk_generic_component PRIMARY KEY (id)
);
COMMENT
ON COLUMN generic_component.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT
ON COLUMN generic_component.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT
ON COLUMN generic_component.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT
ON COLUMN generic_component.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT
ON COLUMN generic_component.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT
ON COLUMN generic_component.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT
ON COLUMN generic_component.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN generic_component.specification_key IS 'Name most important specification for this component.';
COMMENT
ON COLUMN generic_component.specification_value IS 'Specification value';

CREATE TABLE global_property
(
    id              BIGINT DEFAULT nextval('global_property_seq') NOT NULL,
    name            VARCHAR(50)                                   NOT NULL,
    datatype        VARCHAR(255),
    description     VARCHAR(255),
    version         INT,
    date_created    TIMESTAMP                                     NOT NULL,
    date_updated    TIMESTAMP,
    retired         BOOLEAN,
    date_retired    TIMESTAMP,
    retire_reason   VARCHAR(255),
    uuid            VARCHAR(38)                                   NOT NULL,
    value_reference VARCHAR(8192)                                 NOT NULL,
    CONSTRAINT pk_global_property PRIMARY KEY (id)
);
COMMENT
ON COLUMN global_property.uuid IS 'Globally unique identifier';

CREATE TABLE gpu
(
    id                 BIGINT DEFAULT nextval('gpu_seq') NOT NULL,
    manufacturer       VARCHAR(255),
    model              VARCHAR(255),
    year_released      INT,
    is_discontinued    BOOLEAN,
    tdp_watts          INT,
    product_identifier VARCHAR(255),
    name               VARCHAR(255),
    date_created       TIMESTAMP                         NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP,
    date_voided        TIMESTAMP,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                       NOT NULL,
    architecture       VARCHAR(255),
    shader_cores       INT,
    tensor_cores       INT,
    min_clock          INT,
    max_clock          INT,
    capacity           INT,
    capacity_unit      VARCHAR(50),
    type               VARCHAR(50),
    CONSTRAINT pk_gpu PRIMARY KEY (id)
);
COMMENT
ON COLUMN gpu.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT
ON COLUMN gpu.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT
ON COLUMN gpu.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT
ON COLUMN gpu.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT
ON COLUMN gpu.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT
ON COLUMN gpu.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT
ON COLUMN gpu.uuid IS 'Globally unique identifier';

CREATE TABLE hardware_report
(
    id             BIGINT      NOT NULL,
    date_created   TIMESTAMP   NOT NULL,
    description    VARCHAR(255),
    elapsed_time   BIGINT,
    uuid           VARCHAR(38) NOT NULL,
    computer_uuid  VARCHAR(38),
    report_content CLOB,
    CONSTRAINT pk_hardware_report PRIMARY KEY (id)
);
COMMENT
ON COLUMN hardware_report.uuid IS 'Globally unique identifier';

CREATE TABLE memory_module
(
    id                 BIGINT DEFAULT nextval('memory_module_seq') NOT NULL,
    manufacturer       VARCHAR(255),
    model              VARCHAR(255),
    year_released      INT,
    is_discontinued    BOOLEAN,
    tdp_watts          INT,
    product_identifier VARCHAR(255),
    name               VARCHAR(255),
    date_created       TIMESTAMP                                   NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP,
    date_voided        TIMESTAMP,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                                 NOT NULL,
    type               VARCHAR(50),
    capacity           INT,
    capacity_unit      VARCHAR(50),
    bus_speed          INT,
    CONSTRAINT pk_memory_module PRIMARY KEY (id)
);
COMMENT
ON COLUMN memory_module.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT
ON COLUMN memory_module.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT
ON COLUMN memory_module.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT
ON COLUMN memory_module.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT
ON COLUMN memory_module.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT
ON COLUMN memory_module.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT
ON COLUMN memory_module.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN memory_module.type IS 'Memory type, e.g. DDR3, DDR4, etc.';
COMMENT
ON COLUMN memory_module.capacity IS 'Capacity of memory module';
COMMENT
ON COLUMN memory_module.bus_speed IS 'Front side bus speed (clock) in MHz';

CREATE TABLE network_device
(
    id                 BIGINT DEFAULT nextval('network_device_seq') NOT NULL,
    manufacturer       VARCHAR(255),
    model              VARCHAR(255),
    year_released      INT,
    is_discontinued    BOOLEAN,
    tdp_watts          INT,
    product_identifier VARCHAR(255),
    name               VARCHAR(255),
    date_created       TIMESTAMP                                    NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP,
    date_voided        TIMESTAMP,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                                  NOT NULL,
    interface_type     VARCHAR(50),
    mac_address        VARCHAR(255),
    speed              INT                                          NOT NULL,
    CONSTRAINT pk_network_device PRIMARY KEY (id)
);
COMMENT
ON COLUMN network_device.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT
ON COLUMN network_device.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT
ON COLUMN network_device.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT
ON COLUMN network_device.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT
ON COLUMN network_device.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT
ON COLUMN network_device.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT
ON COLUMN network_device.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN network_device.mac_address IS 'Hardware (MAC) address of the network device';
COMMENT
ON COLUMN network_device.speed IS 'Data transfer speed in Mega bits per second';

CREATE TABLE nonce
(
    id             BIGINT DEFAULT nextval('nonce_seq') NOT NULL,
    name           VARCHAR(255),
    date_created   TIMESTAMP                           NOT NULL,
    voided         BOOLEAN,
    date_updated   TIMESTAMP,
    date_voided    TIMESTAMP,
    void_reason    VARCHAR(255),
    description    VARCHAR(255),
    uuid           VARCHAR(38)                         NOT NULL,
    wallet_address VARCHAR(255)                        NOT NULL,
    nonce          VARCHAR(255)                        NOT NULL,
    CONSTRAINT pk_nonce PRIMARY KEY (id)
);
COMMENT
ON COLUMN nonce.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN nonce.wallet_address IS 'The wallet address whom this nonce belongs to';
COMMENT
ON COLUMN nonce.nonce IS 'Nonce used to verify the wallet';

CREATE TABLE provider
(
    id             BIGINT DEFAULT nextval('provider_seq') NOT NULL,
    username       VARCHAR(255)                           NOT NULL,
    role           VARCHAR(255),
    name           VARCHAR(255),
    date_created   TIMESTAMP                              NOT NULL,
    voided         BOOLEAN,
    date_updated   TIMESTAMP,
    date_voided    TIMESTAMP,
    void_reason    VARCHAR(255),
    description    VARCHAR(255),
    uuid           VARCHAR(38)                            NOT NULL,
    wallet_address VARCHAR(255)                           NOT NULL,
    provider_email VARCHAR(255)                           NOT NULL,
    city           VARCHAR(255),
    country        VARCHAR(255),
    CONSTRAINT pk_provider PRIMARY KEY (id)
);
COMMENT
ON COLUMN provider.username IS 'Username chosen by the User. Should be unique';
COMMENT
ON COLUMN provider.uuid IS 'Globally unique identifier';
COMMENT
ON COLUMN provider.wallet_address IS 'Wallet address used to Sign-up';
COMMENT
ON COLUMN provider.provider_email IS 'Email address is applicable only to Providers';
COMMENT
ON COLUMN provider.city IS 'City part of provider address';
COMMENT
ON COLUMN provider.country IS 'Country part of provider address';

CREATE TABLE staff
(
    id           BIGINT DEFAULT nextval('staff_seq') NOT NULL,
    username     VARCHAR(255)                        NOT NULL,
    role         VARCHAR(255),
    name         VARCHAR(255),
    date_created TIMESTAMP                           NOT NULL,
    voided       BOOLEAN,
    date_updated TIMESTAMP,
    date_voided  TIMESTAMP,
    void_reason  VARCHAR(255),
    description  VARCHAR(255),
    uuid         VARCHAR(38)                         NOT NULL,
    CONSTRAINT pk_staff PRIMARY KEY (id)
);
COMMENT
ON COLUMN staff.username IS 'Username chosen by the User. Should be unique';
COMMENT
ON COLUMN staff.uuid IS 'Globally unique identifier';

CREATE TABLE storage
(
    id                 BIGINT DEFAULT nextval('storage_seq') NOT NULL,
    manufacturer       VARCHAR(255),
    model              VARCHAR(255),
    year_released      INT,
    is_discontinued    BOOLEAN,
    tdp_watts          INT,
    product_identifier VARCHAR(255),
    name               VARCHAR(255),
    date_created       TIMESTAMP                             NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP,
    date_voided        TIMESTAMP,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                           NOT NULL,
    type               VARCHAR(50),
    capacity           INT,
    capacity_unit      VARCHAR(50),
    cache              INT,
    is_removable       BOOLEAN,
    CONSTRAINT pk_storage PRIMARY KEY (id)
);
COMMENT
ON COLUMN storage.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT
ON COLUMN storage.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT
ON COLUMN storage.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT
ON COLUMN storage.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT
ON COLUMN storage.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT
ON COLUMN storage.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT
ON COLUMN storage.uuid IS 'Globally unique identifier';

ALTER TABLE benchmark_report
    ADD CONSTRAINT uc_benchmark_report_uuid UNIQUE (uuid);

ALTER TABLE computer_attribute_type
    ADD CONSTRAINT uc_computer_attribute_type_name UNIQUE (name);

ALTER TABLE computer_attribute_type
    ADD CONSTRAINT uc_computer_attribute_type_uuid UNIQUE (uuid);

ALTER TABLE computer
    ADD CONSTRAINT uc_computer_uuid UNIQUE (uuid);

ALTER TABLE consumer
    ADD CONSTRAINT uc_consumer_username UNIQUE (username);

ALTER TABLE consumer
    ADD CONSTRAINT uc_consumer_uuid UNIQUE (uuid);

ALTER TABLE consumer
    ADD CONSTRAINT uc_consumer_walletaddress UNIQUE (wallet_address);

ALTER TABLE cpu
    ADD CONSTRAINT uc_cpu_uuid UNIQUE (uuid);

ALTER TABLE generic_component
    ADD CONSTRAINT uc_generic_component_uuid UNIQUE (uuid);

ALTER TABLE global_property
    ADD CONSTRAINT uc_global_property_name UNIQUE (name);

ALTER TABLE global_property
    ADD CONSTRAINT uc_global_property_uuid UNIQUE (uuid);

ALTER TABLE gpu
    ADD CONSTRAINT uc_gpu_uuid UNIQUE (uuid);

ALTER TABLE hardware_report
    ADD CONSTRAINT uc_hardware_report_uuid UNIQUE (uuid);

ALTER TABLE memory_module
    ADD CONSTRAINT uc_memory_module_uuid UNIQUE (uuid);

ALTER TABLE network_device
    ADD CONSTRAINT uc_network_device_uuid UNIQUE (uuid);

ALTER TABLE nonce
    ADD CONSTRAINT uc_nonce_uuid UNIQUE (uuid);

ALTER TABLE nonce
    ADD CONSTRAINT uc_nonce_walletaddress UNIQUE (wallet_address);

ALTER TABLE provider
    ADD CONSTRAINT uc_provider_username UNIQUE (username);

ALTER TABLE provider
    ADD CONSTRAINT uc_provider_uuid UNIQUE (uuid);

ALTER TABLE provider
    ADD CONSTRAINT uc_provider_walletaddress UNIQUE (wallet_address);

ALTER TABLE staff
    ADD CONSTRAINT uc_staff_username UNIQUE (username);

ALTER TABLE staff
    ADD CONSTRAINT uc_staff_uuid UNIQUE (uuid);

ALTER TABLE storage
    ADD CONSTRAINT uc_storage_uuid UNIQUE (uuid);

ALTER TABLE cpu
    ADD CONSTRAINT uk_cpu_manufacturer_model UNIQUE (manufacturer, model);

ALTER TABLE gpu
    ADD CONSTRAINT uk_gpu_manufacturer_model UNIQUE (manufacturer, model);

ALTER TABLE memory_module
    ADD CONSTRAINT uk_memory_module_manufacturer_model UNIQUE (manufacturer, model);

ALTER TABLE network_device
    ADD CONSTRAINT uk_network_device_manufacturer_model UNIQUE (manufacturer, model);

ALTER TABLE storage
    ADD CONSTRAINT uk_storage_manufacturer_model UNIQUE (manufacturer, model);

ALTER TABLE benchmark_report
    ADD CONSTRAINT FK_BENCHMARK_REPORT_ON_COMPUTER FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE benchmark_report
    ADD CONSTRAINT FK_BENCHMARK_REPORT_ON_PROVIDER FOREIGN KEY (provider_id) REFERENCES provider (id);

ALTER TABLE computer
    ADD CONSTRAINT FK_COMPUTER_ON_PROVIDER FOREIGN KEY (provider_id) REFERENCES provider (id);

ALTER TABLE computer_cpu
    ADD CONSTRAINT fk_comcpu_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_cpu
    ADD CONSTRAINT fk_comcpu_on_cpu FOREIGN KEY (cpu_id) REFERENCES cpu (id);

ALTER TABLE computer_gpu
    ADD CONSTRAINT fk_comgpu_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_gpu
    ADD CONSTRAINT fk_comgpu_on_gpu FOREIGN KEY (gpu_id) REFERENCES gpu (id);

ALTER TABLE computer_memory
    ADD CONSTRAINT fk_commem_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_memory
    ADD CONSTRAINT fk_commem_on_memory_module FOREIGN KEY (memory_id) REFERENCES memory_module (id);

ALTER TABLE computer_nics
    ADD CONSTRAINT fk_comnic_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_nics
    ADD CONSTRAINT fk_comnic_on_network_device FOREIGN KEY (nic_id) REFERENCES network_device (id);

ALTER TABLE computer_other_component
    ADD CONSTRAINT fk_comothcom_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_other_component
    ADD CONSTRAINT fk_comothcom_on_generic_component FOREIGN KEY (component_id) REFERENCES generic_component (id);

ALTER TABLE computer_attribute
    ADD CONSTRAINT fk_computer_attribute_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_attribute
    ADD CONSTRAINT fk_computer_attribute_on_computer_attribute_type FOREIGN KEY (computer_attribute_id) REFERENCES computer_attribute_type (id);

ALTER TABLE computer_storage
    ADD CONSTRAINT fk_comsto_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_storage
    ADD CONSTRAINT fk_comsto_on_storage FOREIGN KEY (storage_id) REFERENCES storage (id);