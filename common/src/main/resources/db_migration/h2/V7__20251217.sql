CREATE SEQUENCE IF NOT EXISTS staff_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE computer_cpu
    ADD CONSTRAINT pk_computer_cpu PRIMARY KEY (computer_id, cpu_id);

ALTER TABLE computer_gpu
    ADD CONSTRAINT pk_computer_gpu PRIMARY KEY (computer_id, gpu_id);

ALTER TABLE computer_memory
    ADD CONSTRAINT pk_computer_memory PRIMARY KEY (computer_id, memory_id);

ALTER TABLE computer_nics
    ADD CONSTRAINT pk_computer_nics PRIMARY KEY (computer_id, nic_id);

ALTER TABLE computer_other_component
    ADD CONSTRAINT pk_computer_other_component PRIMARY KEY (component_id, computer_id);

ALTER TABLE computer_storage
    ADD CONSTRAINT pk_computer_storage PRIMARY KEY (computer_id, storage_id);

ALTER TABLE hardware_report
DROP
COLUMN computer_id;

ALTER SEQUENCE benchmark_report_seq INCREMENT BY 1;

ALTER SEQUENCE computer_attribute_type_seq INCREMENT BY 1;

ALTER SEQUENCE computer_seq INCREMENT BY 1;

ALTER SEQUENCE consumer_seq INCREMENT BY 1;

ALTER SEQUENCE cpu_seq INCREMENT BY 1;

ALTER SEQUENCE global_property_seq INCREMENT BY 1;

ALTER SEQUENCE gpu_seq INCREMENT BY 1;

ALTER SEQUENCE hardware_report_seq INCREMENT BY 1;

ALTER SEQUENCE memory_module_seq INCREMENT BY 1;

ALTER SEQUENCE network_device_seq INCREMENT BY 1;

ALTER SEQUENCE nonce_seq INCREMENT BY 1;

ALTER SEQUENCE provider_seq INCREMENT BY 1;

ALTER SEQUENCE storage_seq INCREMENT BY 1;

ALTER TABLE global_property
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE global_property
    ALTER COLUMN value_reference SET NOT NULL;
