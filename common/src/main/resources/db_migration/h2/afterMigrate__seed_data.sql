-----------------------------------
-- Insert Operating Systems List --
-----------------------------------
MERGE INTO global_property (id, uuid, name, datatype, description, version, date_created, retired, value_reference)
VALUES
(1, '11111111-0000-1111-2222-3456789abcde', 'operating_systems', 'ai.nextgpu.common.model.BaseJson', 'List of operating systems could be used by the provider.', 1, '2025-12-29 00:00:00', FALSE, '{"operating_systems": ["Windows 7", "Windows 8.1", "Windows 10", "Windows 11", "Windows Server 2016", "Windows Server 2019", "Windows Server 2022", "macOS 13.5", "macOS 14", "Mac OS X 10.15", "Mac OS X 11.7", "Mac OS X 12.6", "macOS Mojave", "macOS Catalina", "macOS Big Sur", "macOS Monterey", "macOS Ventura", "macOS Sonoma", "macOS Sequoia", "FreeBSD 13.2", "FreeBSD 14", "OpenBSD 7.3", "OpenBSD 7.4", "Solaris 11.4", "Solaris 10", "AIX 7.3", "AIX 7.2", "Black Cat 1.0", "BlueWhite64 2.1", "SME Server 9.0", "FreeEOS 1.2", "HLFS 3.0", "Linux-From-Scratch 11.0", "Linux-PPC 1.0", "MeeGo 1.2", "Mandrake 10.2", "MkLinux 1.0", "Novell Linux Desktop 9.0", "SUSE Linux 12.0", "SUSE Linux ES9 9.0", "Sun JDS 5.0", "Synology 7.0", "Tiny Sofa 1.0", "TurboLinux 10.0", "UltraPenguin 1.0", "VA-Linux 3.0", "VMWareESX 6.7", "Yellow Dog 5.0", "Ubuntu", "Debian", "Fedora", "CentOS", "Red Hat Enterprise Linux", "Linux Mint", "Pop!_OS", "Arch Linux", "Manjaro"]}'),
(2, 'af4c9e2b-ac2d-1111-a456-b2af1c17bcde', 'min_required_storage_capacity', 'java.lang.Integer', 'Minimum required capacity of storage in GB.', 1, '2025-12-29 00:00:00', FALSE, '256'),
(3, '7a4c3f2c-1c5c-e89b-7ba6-9f1c2a9f4c25', 'min_required_memory_capacity', 'java.lang.Integer', 'Minimum required capacity of memory in GB.', 1, '2025-12-29 00:00:00', FALSE, '8')
;

-----------------------------------
-- Seed Computer Attribute Types --
-----------------------------------
MERGE INTO computer_attribute_type (id, uuid, name, datatype, description, version, date_created, is_searchable, is_mandatory, is_unique, display_order, category, retired)
VALUES
-- System Locale
(3, '7f3c9e2b-7a5d-4c9e-8e1f-3d5a9b8c7d1a', 'system_locale', 'java.lang.String',  'Locale of the operating system (e.g., en_US)', 1, '2025-12-29 00:00:00', TRUE, FALSE, FALSE, 1, 'system', FALSE),
-- System Timezone
(4, 'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d', 'system_timezone', 'java.time.ZoneId', 'Current system timezone', 1,  '2025-12-29 00:00:00', TRUE, FALSE, FALSE, 2, 'system', FALSE),
-- Last Audit Date
(5, '123e4567-e89b-12d3-a456-426614174000', 'last_audit_date', 'java.time.LocalDateTime',  'Timestamp of the last audit performed on the system', 1, '2025-12-29 00:00:00', FALSE, FALSE, FALSE, 3, 'audit',  FALSE),
-- Last Audit Status
(6, '456f7890-e12b-34d5-c678-901234567890', 'last_audit_status', 'java.lang.String',  'Indicates whether the system has been audited', 1, '2025-12-29 00:00:00', TRUE, FALSE, FALSE, 4, 'audit', FALSE),
-- Registration Status
(7, 'e4cbf165-d70e-4829-935b-244797efe704', 'registration_status', 'java.lang.String',  'Indicates whether the user has completed registration', 1, '2025-12-29 00:00:00', TRUE, FALSE, FALSE, 5, 'registration',  FALSE),
-- Was Previously Banned
(8, '789a123b-4c5d-6e7f-8a9b-0c1d2e3f4a5b', 'was_previously_banned', 'java.lang.Boolean',  'Indicates whether the user was banned previously', 1, '2025-12-29 00:00:00', TRUE, FALSE, FALSE, 5, 'compliance',  FALSE),
-- User Average Ranking
(9, '0ab1c23d-4e5f-6a7b-8c9d-0e1f2a3b4c5d', 'user_average_ranking', 'java.lang.Double',  'Average ranking score of the Provider PC ', 1, '2025-12-29 00:00:00', TRUE, FALSE, FALSE, 6, 'ranking', FALSE)
;

-----------------------------------
-- Seed Provider Attribute Types --
-----------------------------------
MERGE INTO provider_attribute_type (id, uuid, name, datatype, description, version, date_created, date_updated, retired, date_retired, retire_reason, is_mandatory, is_unique, validation_regex)
VALUES
-- minimum_stake_status
(1, '61a19419-5f09-45db-92eb-b0bb01ba5f2e', 'minimum_stake_status', 'java.lang.String', 'Indicates whether the provider has staked the minimum required amount in the staking contract', 1, '2024-01-15 10:00:00', NULL, FALSE, NULL, NULL, FALSE, FALSE, NULL),
(2, '4f9e8e59-4111-45d4-b7c8-397c33668e8a', 'is_email_verified', 'java.lang.Boolean', 'Represents if the provider has verified the email', 1, '2024-01-15 10:00:00', NULL, FALSE, NULL, NULL, TRUE, FALSE, NULL)
;