## MODIFIED Requirements

### Requirement: Low-SLI review sheet content

For sheet `{month}-SLA小于99.9%待复盘`, the system SHALL take systems from the subsystem and parent annual SLA inventories whose annual SLI is below 99.9%, match each system to the corresponding leaf or parent downtime dashboard by name and SLI source level, and emit one Excel row per matched downtime record whose exclusion status is `未排除` or `-` (literal hyphen). Each row MUST include: system name, system category (`子系统` or `父系统` from SLI source), interrupt reason left empty, downtime start time, downtime end time, and owner name from `sla_system_owner` for that category and system name.

#### Scenario: Only low-SLI systems on sheet 1

- **WHEN** a system has annual SLI at or above 99.9%
- **THEN** its downtime rows MUST NOT appear on the low-SLI review sheet

#### Scenario: Exclusion filter on sheet 1

- **WHEN** a low-SLI system has downtime with exclusion status `部分排除` or other non-allowed values
- **THEN** that downtime record MUST NOT appear on the low-SLI review sheet

#### Scenario: Empty interrupt reason

- **WHEN** a row is written to the low-SLI review sheet
- **THEN** the interrupt-reason cell MUST be empty for manual fill-in

### Requirement: Owner column uses existing resolver

The owner column MUST be filled from `sla_system_owner` using exact `(system_level, system_name)` where `system_level` is the sheet’s system category (`子系统` or `父系统`). The cell MUST contain that row’s `owner_name`. Resolution MUST NOT use `owners.rules` region or TRADE/BID matching.

#### Scenario: Matched owner name written

- **WHEN** a dictionary row exists for the row’s level and system name
- **THEN** the owner cell MUST contain that dictionary `owner_name`
