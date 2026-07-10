ALTER TABLE computer
    ADD hardware_fingerprint VARCHAR(64) UNIQUE;

COMMENT ON COLUMN computer.hardware_fingerprint IS
    'SHA-256 hash (hex encoded, 64 characters) of the unique physical machine serial number. Used for hardware fingerprinting. Fallback sequence: System Serial -> Motherboard Baseboard Serial.';
