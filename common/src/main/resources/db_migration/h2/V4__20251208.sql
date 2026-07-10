ALTER TABLE computer
    ADD COLUMN (provider_id BIGINT, operating_system VARCHAR(255));

ALTER TABLE computer
    ADD CONSTRAINT fk_computer_provider FOREIGN KEY (provider_id) REFERENCES provider (id);

COMMENT ON COLUMN computer.provider_id IS 'Foreign key reference to the provider table';
COMMENT ON COLUMN computer.operating_system IS 'The operating system installed on this computer, e.g., Windows 11 Pro, Ubuntu 24.04 LTS';

ALTER TABLE computer_attribute_type
    ADD COLUMN (display_order INTEGER, category VARCHAR(255), validation_regex VARCHAR(255));

COMMENT ON COLUMN computer_attribute_type.display_order IS 'Useful in UI. What''s the order in which this attribute will be displayed in forms/reports';
COMMENT ON COLUMN computer_attribute_type.category IS 'Useful in UI. Combine with displayOrder to create sections on UI interface';
COMMENT ON COLUMN computer_attribute_type.validation_regex IS 'Regular expression to validate the value entered in the respective ComputerAttribute object';

ALTER TABLE gpu
    ADD COLUMN capacity_unit VARCHAR(50);

ALTER TABLE memory_module
    ADD COLUMN capacity_unit VARCHAR(50);

ALTER TABLE storage
    ADD COLUMN (is_removable BOOLEAN, capacity_unit VARCHAR(50));

ALTER TABLE network_device
    ADD COLUMN interface_type VARCHAR(50);

ALTER TABLE hardware_report
    ADD COLUMN (computer_uuid VARCHAR(38), report_content TEXT);

