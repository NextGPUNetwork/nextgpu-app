-- Reordered schema in natural order
-- Independent tables come first, followed by dependent tables
-- Sequences are placed right before their respective tables
-- Constraints are placed right after their respective tables
-- All statements use IF NOT EXISTS for safety

-- Computer Attribute Type
CREATE SEQUENCE IF NOT EXISTS computer_attribute_type_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS computer_attribute_type
(
    id            BIGINT NOT NULL DEFAULT nextval('computer_attribute_type_seq'),
    name          VARCHAR(50)                 NOT NULL ,
    datatype      VARCHAR(255),
    description   VARCHAR(255),
    version       INTEGER,
    date_created  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    date_updated  TIMESTAMP WITHOUT TIME ZONE,
    retired       BOOLEAN,
    date_retired  TIMESTAMP WITHOUT TIME ZONE,
    retire_reason VARCHAR(255),
    uuid          VARCHAR(38)                 NOT NULL,
    is_searchable BOOLEAN,
    is_mandatory  BOOLEAN,
    is_unique     BOOLEAN,
    CONSTRAINT pk_computer_attribute_type PRIMARY KEY (id)
);
COMMENT ON COLUMN computer_attribute_type.uuid IS 'Globally unique identifier';

ALTER TABLE computer_attribute_type
    ADD CONSTRAINT uc_computer_attribute_type_uuid UNIQUE (uuid);

-- Computer
CREATE SEQUENCE IF NOT EXISTS computer_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS computer
(
    id           BIGINT NOT NULL DEFAULT nextval('computer_seq'),
    name         VARCHAR(50),
    date_created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided       BOOLEAN,
    date_updated TIMESTAMP WITHOUT TIME ZONE,
    date_voided  TIMESTAMP WITHOUT TIME ZONE,
    void_reason  VARCHAR(255),
    description  VARCHAR(255),
    uuid         VARCHAR(38)                 NOT NULL,
    type         VARCHAR(50),
    CONSTRAINT pk_computer PRIMARY KEY (id)
);
COMMENT ON COLUMN computer.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN computer.type IS 'The type of the computer (e.g., Laptop, Desktop, Workstation)';

ALTER TABLE computer
    ADD CONSTRAINT uc_computer_uuid UNIQUE (uuid);

-- Consumer
CREATE SEQUENCE IF NOT EXISTS consumer_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS consumer
(
    id             BIGINT NOT NULL DEFAULT nextval('consumer_seq'),
    username       VARCHAR(255)                NOT NULL,
    role           VARCHAR(255),
    name           VARCHAR(50),
    date_created   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided         BOOLEAN,
    date_updated   TIMESTAMP WITHOUT TIME ZONE,
    date_voided    TIMESTAMP WITHOUT TIME ZONE,
    void_reason    VARCHAR(255),
    description    VARCHAR(255),
    uuid           VARCHAR(38)                 NOT NULL,
    wallet_address VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_consumer PRIMARY KEY (id)
);
COMMENT ON COLUMN consumer.username IS 'Username chosen by the User. Should be unique';
COMMENT ON COLUMN consumer.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN consumer.wallet_address IS 'Wallet address used to Sign-up';

ALTER TABLE consumer
    ADD CONSTRAINT uc_consumer_username UNIQUE (username);

ALTER TABLE consumer
    ADD CONSTRAINT uc_consumer_uuid UNIQUE (uuid);

ALTER TABLE consumer
    ADD CONSTRAINT uc_consumer_walletaddress UNIQUE (wallet_address);

-- CPU
CREATE SEQUENCE IF NOT EXISTS cpu_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS cpu
(
    id                 BIGINT NOT NULL DEFAULT nextval('cpu_seq'),
    manufacturer       VARCHAR(50),
    model              VARCHAR(50),
    year_released      INTEGER,
    is_discontinued    BOOLEAN,
    tdp_watts          INTEGER,
    product_identifier VARCHAR(255),
    name               VARCHAR(50),
    date_created       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP WITHOUT TIME ZONE,
    date_voided        TIMESTAMP WITHOUT TIME ZONE,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                 NOT NULL,
    architecture       VARCHAR(50),
    cores              INTEGER,
    threads            INTEGER,
    min_clock          INTEGER,
    max_clock          INTEGER,
    l3_cache           INTEGER,
    CONSTRAINT pk_cpu PRIMARY KEY (id)
);
COMMENT ON COLUMN cpu.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT ON COLUMN cpu.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT ON COLUMN cpu.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT ON COLUMN cpu.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT ON COLUMN cpu.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT ON COLUMN cpu.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT ON COLUMN cpu.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN cpu.architecture IS 'The underlying architecture like x86_64, ARM, etc.';
COMMENT ON COLUMN cpu.cores IS 'Number of physical cores inside the chip package';
COMMENT ON COLUMN cpu.threads IS 'Number of logical cores. It is usually double the no. of cores if hyper-threading is enabled';
COMMENT ON COLUMN cpu.min_clock IS 'Minimum CPU frequency. A.k.a. Base Clock in MHz';
COMMENT ON COLUMN cpu.max_clock IS 'Maximum CPU frequency. A.k.a. Boost/Turbo Clock in MHz';
COMMENT ON COLUMN cpu.l3_cache IS 'The highest level cache in KBs. Usually, it is level 3 or in some cases, level 4 cache';

ALTER TABLE cpu
    ADD CONSTRAINT uc_cpu_uuid UNIQUE (uuid);

-- Generic Component
CREATE SEQUENCE IF NOT EXISTS generic_component_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS generic_component
(
    id                  BIGINT NOT NULL DEFAULT nextval('generic_component_seq'),
    manufacturer        VARCHAR(50),
    model               VARCHAR(50),
    year_released       INTEGER,
    is_discontinued     BOOLEAN,
    tdp_watts           INTEGER,
    product_identifier  VARCHAR(255),
    name                VARCHAR(50),
    date_created        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided              BOOLEAN,
    date_updated        TIMESTAMP WITHOUT TIME ZONE,
    date_voided         TIMESTAMP WITHOUT TIME ZONE,
    void_reason         VARCHAR(255),
    description         VARCHAR(255),
    uuid                VARCHAR(38)                 NOT NULL,
    type                VARCHAR(50),
    specification_key   VARCHAR(50)                 NOT NULL,
    specification_value VARCHAR(255),
    CONSTRAINT pk_generic_component PRIMARY KEY (id)
);
COMMENT ON COLUMN generic_component.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT ON COLUMN generic_component.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT ON COLUMN generic_component.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT ON COLUMN generic_component.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT ON COLUMN generic_component.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT ON COLUMN generic_component.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT ON COLUMN generic_component.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN generic_component.specification_key IS 'Name most important specification for this component.';
COMMENT ON COLUMN generic_component.specification_value IS 'Specification value';

ALTER TABLE generic_component
    ADD CONSTRAINT uc_generic_component_uuid UNIQUE (uuid);

-- GPU
CREATE SEQUENCE IF NOT EXISTS gpu_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS gpu
(
    id                 BIGINT NOT NULL DEFAULT nextval('gpu_seq'),
    manufacturer       VARCHAR(50),
    model              VARCHAR(50),
    year_released      INTEGER,
    is_discontinued    BOOLEAN,
    tdp_watts          INTEGER,
    product_identifier VARCHAR(255),
    name               VARCHAR(50),
    date_created       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP WITHOUT TIME ZONE,
    date_voided        TIMESTAMP WITHOUT TIME ZONE,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                 NOT NULL,
    architecture       VARCHAR(50),
    shader_cores       INTEGER,
    tensor_cores       INTEGER,
    min_clock          INTEGER,
    max_clock          INTEGER,
    capacity           INTEGER,
    type               VARCHAR(50),
    CONSTRAINT pk_gpu PRIMARY KEY (id)
);
COMMENT ON COLUMN gpu.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT ON COLUMN gpu.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT ON COLUMN gpu.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT ON COLUMN gpu.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT ON COLUMN gpu.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT ON COLUMN gpu.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT ON COLUMN gpu.uuid IS 'Globally unique identifier';

ALTER TABLE gpu
    ADD CONSTRAINT uc_gpu_uuid UNIQUE (uuid);

-- Memory Module
CREATE SEQUENCE IF NOT EXISTS memory_module_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS memory_module
(
    id                 BIGINT NOT NULL DEFAULT nextval('memory_module_seq'),
    manufacturer       VARCHAR(50),
    model              VARCHAR(50),
    year_released      INTEGER,
    is_discontinued    BOOLEAN,
    tdp_watts          INTEGER,
    product_identifier VARCHAR(255),
    name               VARCHAR(50),
    date_created       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP WITHOUT TIME ZONE,
    date_voided        TIMESTAMP WITHOUT TIME ZONE,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                 NOT NULL,
    type               VARCHAR(50),
    capacity           INTEGER,
    bus_speed          INTEGER,
    CONSTRAINT pk_memory_module PRIMARY KEY (id)
);
COMMENT ON COLUMN memory_module.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT ON COLUMN memory_module.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT ON COLUMN memory_module.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT ON COLUMN memory_module.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT ON COLUMN memory_module.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT ON COLUMN memory_module.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT ON COLUMN memory_module.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN memory_module.type IS 'Memory type, e.g. DDR3, DDR4, etc.';
COMMENT ON COLUMN memory_module.capacity IS 'Capacity in MBs (considering 512MB still exists)';
COMMENT ON COLUMN memory_module.bus_speed IS 'Front side bus speed (clock) in MHz';

ALTER TABLE memory_module
    ADD CONSTRAINT uc_memory_module_uuid UNIQUE (uuid);

-- Network Device
CREATE SEQUENCE IF NOT EXISTS network_device_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS network_device
(
    id                 BIGINT NOT NULL DEFAULT nextval('network_device_seq'),
    manufacturer       VARCHAR(50),
    model              VARCHAR(50),
    year_released      INTEGER,
    is_discontinued    BOOLEAN,
    tdp_watts          INTEGER,
    product_identifier VARCHAR(255),
    name               VARCHAR(50),
    date_created       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP WITHOUT TIME ZONE,
    date_voided        TIMESTAMP WITHOUT TIME ZONE,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                 NOT NULL,
    mac_address        VARCHAR(255),
    speed              INTEGER                     NOT NULL,
    CONSTRAINT pk_network_device PRIMARY KEY (id)
);
COMMENT ON COLUMN network_device.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT ON COLUMN network_device.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT ON COLUMN network_device.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT ON COLUMN network_device.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT ON COLUMN network_device.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT ON COLUMN network_device.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT ON COLUMN network_device.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN network_device.mac_address IS 'Hardware (MAC) address of the network device';
COMMENT ON COLUMN network_device.speed IS 'Data transfer speed in Mega bits per second';

ALTER TABLE network_device
    ADD CONSTRAINT uc_network_device_uuid UNIQUE (uuid);

-- Nonce
CREATE SEQUENCE IF NOT EXISTS nonce_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS nonce
(
    id             BIGINT NOT NULL DEFAULT nextval('nonce_seq'),
    name           VARCHAR(50),
    date_created   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided         BOOLEAN,
    date_updated   TIMESTAMP WITHOUT TIME ZONE,
    date_voided    TIMESTAMP WITHOUT TIME ZONE,
    void_reason    VARCHAR(255),
    description    VARCHAR(255),
    uuid           VARCHAR(38)                 NOT NULL,
    wallet_address VARCHAR(255)                NOT NULL,
    nonce          VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_nonce PRIMARY KEY (id)
);
COMMENT ON COLUMN nonce.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN nonce.wallet_address IS 'The wallet address whom this nonce belongs to';
COMMENT ON COLUMN nonce.nonce IS 'Nonce used to verify the wallet';

ALTER TABLE nonce
    ADD CONSTRAINT uc_nonce_uuid UNIQUE (uuid);

ALTER TABLE nonce
    ADD CONSTRAINT uc_nonce_walletaddress UNIQUE (wallet_address);

-- Provider
CREATE SEQUENCE IF NOT EXISTS provider_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS provider
(
    id             BIGINT NOT NULL DEFAULT nextval('provider_seq'),
    username       VARCHAR(255)                NOT NULL,
    role           VARCHAR(255),
    name           VARCHAR(50),
    date_created   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided         BOOLEAN,
    date_updated   TIMESTAMP WITHOUT TIME ZONE,
    date_voided    TIMESTAMP WITHOUT TIME ZONE,
    void_reason    VARCHAR(255),
    description    VARCHAR(255),
    uuid           VARCHAR(38)                 NOT NULL,
    wallet_address VARCHAR(255)                NOT NULL,
    provider_email VARCHAR(255)                NOT NULL UNIQUE,
    city           VARCHAR(255),
    country        VARCHAR(255),
    CONSTRAINT pk_provider PRIMARY KEY (id)
);
COMMENT ON COLUMN provider.username IS 'Username chosen by the User. Should be unique';
COMMENT ON COLUMN provider.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN provider.wallet_address IS 'Wallet address used to Sign-up';
COMMENT ON COLUMN provider.provider_email IS 'Email address is applicable only to Providers';
COMMENT ON COLUMN provider.city IS 'City part of provider address';
COMMENT ON COLUMN provider.country IS 'Country part of provider address';

ALTER TABLE provider
    ADD CONSTRAINT uc_provider_username UNIQUE (username);

ALTER TABLE provider
    ADD CONSTRAINT uc_provider_uuid UNIQUE (uuid);

ALTER TABLE provider
    ADD CONSTRAINT uc_provider_walletaddress UNIQUE (wallet_address);

-- Storage
CREATE SEQUENCE IF NOT EXISTS storage_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS storage
(
    id                 BIGINT NOT NULL DEFAULT nextval('storage_seq'),
    manufacturer       VARCHAR(50),
    model              VARCHAR(50),
    year_released      INTEGER,
    is_discontinued    BOOLEAN,
    tdp_watts          INTEGER,
    product_identifier VARCHAR(255),
    name               VARCHAR(50),
    date_created       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    voided             BOOLEAN,
    date_updated       TIMESTAMP WITHOUT TIME ZONE,
    date_voided        TIMESTAMP WITHOUT TIME ZONE,
    void_reason        VARCHAR(255),
    description        VARCHAR(255),
    uuid               VARCHAR(38)                 NOT NULL,
    type               VARCHAR(50),
    capacity           INTEGER,
    cache              INTEGER,
    CONSTRAINT pk_storage PRIMARY KEY (id)
);
COMMENT ON COLUMN storage.manufacturer IS 'Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.';
COMMENT ON COLUMN storage.model IS 'Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC';
COMMENT ON COLUMN storage.year_released IS 'Year of release helps in identifying end of life of components';
COMMENT ON COLUMN storage.is_discontinued IS 'If a component is discontinued, it has a low change to be replaced with the same part.';
COMMENT ON COLUMN storage.tdp_watts IS 'Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage';
COMMENT ON COLUMN storage.product_identifier IS 'The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise';
COMMENT ON COLUMN storage.uuid IS 'Globally unique identifier';

ALTER TABLE storage
    ADD CONSTRAINT uc_storage_uuid UNIQUE (uuid);

-- Dependent tables

-- Computer Attribute (depends on computer and computer_attribute_type)
CREATE TABLE IF NOT EXISTS computer_attribute
(
    computer_id           BIGINT NOT NULL,
    attribute_value       VARCHAR(255),
    computer_attribute_id BIGINT NOT NULL,
    CONSTRAINT pk_computer_attribute PRIMARY KEY (computer_id, computer_attribute_id)
);
COMMENT ON COLUMN computer_attribute.attribute_value IS 'This map collects various attributes of this computer. Anything that cannot be represented as computer parts can be categorized as attribute';

ALTER TABLE computer_attribute
    ADD CONSTRAINT fk_computer_attribute_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_attribute
    ADD CONSTRAINT fk_computer_attribute_on_computer_attribute_type FOREIGN KEY (computer_attribute_id) REFERENCES computer_attribute_type (id);

-- Computer CPU (depends on computer and cpu)
CREATE TABLE IF NOT EXISTS computer_cpu
(
    computer_id BIGINT NOT NULL,
    cpu_id      BIGINT NOT NULL
);

ALTER TABLE computer_cpu
    ADD CONSTRAINT fk_comcpu_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_cpu
    ADD CONSTRAINT fk_comcpu_on_cpu FOREIGN KEY (cpu_id) REFERENCES cpu (id);

-- Computer GPU (depends on computer and gpu)
CREATE TABLE IF NOT EXISTS computer_gpu
(
    computer_id BIGINT NOT NULL,
    gpu_id      BIGINT NOT NULL
);

ALTER TABLE computer_gpu
    ADD CONSTRAINT fk_comgpu_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_gpu
    ADD CONSTRAINT fk_comgpu_on_gpu FOREIGN KEY (gpu_id) REFERENCES gpu (id);

-- Computer Memory (depends on computer and memory_module)
CREATE TABLE IF NOT EXISTS computer_memory
(
    computer_id BIGINT NOT NULL,
    memory_id   BIGINT NOT NULL
);

ALTER TABLE computer_memory
    ADD CONSTRAINT fk_commem_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_memory
    ADD CONSTRAINT fk_commem_on_memory_module FOREIGN KEY (memory_id) REFERENCES memory_module (id);

-- Computer NICs (depends on computer and network_device)
CREATE TABLE IF NOT EXISTS computer_nics
(
    computer_id BIGINT NOT NULL,
    nic_id      BIGINT NOT NULL
);

ALTER TABLE computer_nics
    ADD CONSTRAINT fk_comnic_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_nics
    ADD CONSTRAINT fk_comnic_on_network_device FOREIGN KEY (nic_id) REFERENCES network_device (id);

-- Computer Other Component (depends on computer and generic_component)
CREATE TABLE IF NOT EXISTS computer_other_component
(
    component_id BIGINT NOT NULL,
    computer_id  BIGINT NOT NULL
);

ALTER TABLE computer_other_component
    ADD CONSTRAINT fk_comothcom_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_other_component
    ADD CONSTRAINT fk_comothcom_on_generic_component FOREIGN KEY (component_id) REFERENCES generic_component (id);

-- Computer Storage (depends on computer and storage)
CREATE TABLE IF NOT EXISTS computer_storage
(
    computer_id BIGINT NOT NULL,
    storage_id  BIGINT NOT NULL
);

ALTER TABLE computer_storage
    ADD CONSTRAINT fk_comsto_on_computer FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE computer_storage
    ADD CONSTRAINT fk_comsto_on_storage FOREIGN KEY (storage_id) REFERENCES storage (id);

-- Benchmark Report (depends on computer and provider)
CREATE SEQUENCE IF NOT EXISTS benchmark_report_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS benchmark_report
(
    id                              BIGINT NOT NULL DEFAULT nextval('benchmark_report_seq'),
    date_created                    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    description                     VARCHAR(255),
    elapsed_time                    BIGINT,
    uuid                            VARCHAR(38)                 NOT NULL,
    provider_id                     BIGINT,
    computer_id                     BIGINT,
    cpu_benchmark_tool              VARCHAR(255),
    cpu_single_core_score           INTEGER,
    cpu_multi_core_score            INTEGER,
    gpu_benchmark_tool              VARCHAR(255),
    gpu_openglscore                 INTEGER,
    gpu_vulkan_score                INTEGER,
    gpu_float_point_score           INTEGER,
    secondary_gpu_benchmark_tool    VARCHAR(255),
    secondary_gpu_openglscore       INTEGER,
    secondary_gpu_vulkan_score      INTEGER,
    secondary_gpu_float_point_score INTEGER,
    tertiary_gpu_benchmark_tool     VARCHAR(255),
    tertiary_gpu_openglscore        INTEGER,
    tertiary_gpu_vulkan_score       INTEGER,
    tertiary_gpu_float_point_score  INTEGER,
    memory_benchmark_tool           VARCHAR(255),
    memory_read_speed               INTEGER,
    memory_write_speed              INTEGER,
    memory_latency                  INTEGER,
    storage_benchmark_tool          VARCHAR(255),
    storage_sequential_read_speed   INTEGER,
    storage_sequential_write_speed  INTEGER,
    storage_random_readiops         INTEGER,
    storage_random_writeiops        INTEGER,
    network_benchmark_tool          VARCHAR(255),
    network_download_speed          INTEGER,
    network_upload_speed            INTEGER,
    network_latency                 INTEGER,
    CONSTRAINT pk_benchmark_report PRIMARY KEY (id)
);
COMMENT ON COLUMN benchmark_report.uuid IS 'Globally unique identifier';
COMMENT ON COLUMN benchmark_report.provider_id IS 'The provider of computer whose benchmark is done';
COMMENT ON COLUMN benchmark_report.computer_id IS 'The computer whose benchmark is done';

ALTER TABLE benchmark_report
    ADD CONSTRAINT uc_benchmark_report_uuid UNIQUE (uuid);

ALTER TABLE benchmark_report
    ADD CONSTRAINT FK_BENCHMARK_REPORT_ON_COMPUTER FOREIGN KEY (computer_id) REFERENCES computer (id);

ALTER TABLE benchmark_report
    ADD CONSTRAINT FK_BENCHMARK_REPORT_ON_PROVIDER FOREIGN KEY (provider_id) REFERENCES provider (id);