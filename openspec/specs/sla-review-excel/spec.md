# sla-review-excel

## Purpose

通过 HTTP API 导出 SLA 复盘 Excel（双页签）：低 SLI 待复盘与全量待复盘，行来自年度清单与未排除宕机明细，责任人来自 `sla_system_owner`。

## Requirements

### Requirement: Export review workbook via API

The system SHALL provide an HTTP API that generates and returns an Excel workbook (`.xlsx`) for SLA review. The workbook MUST contain exactly two sheets named `{month}-SLA小于99.9%待复盘` and `{month}-所有待复盘`, where `{month}` is a month label used for naming only (default: current calendar month in `Asia/Shanghai`). The API MUST NOT filter downtime rows by whether the downtime start time falls inside that month.

#### Scenario: Successful download

- **WHEN** a client calls the review Excel export API
- **THEN** the response MUST be an `.xlsx` attachment containing the two named sheets

#### Scenario: Month label does not filter downtime

- **WHEN** a downtime row has exclusion status `未排除` or `-` and its start time is outside the labeled month
- **THEN** that row MUST still be eligible for inclusion if its system is in the sheet's system set

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

### Requirement: All-systems review sheet content

For sheet `{month}-所有待复盘`, the system SHALL take **all** systems from the subsystem and parent annual SLA inventories (regardless of SLI), match downtime the same way, and emit one Excel row per matched downtime record whose exclusion status is `未排除` or `-`. Row columns MUST be the same as the low-SLI sheet.

#### Scenario: High-SLI system can appear on sheet 2

- **WHEN** a system has annual SLI at or above 99.9% and has an unexcluded downtime row
- **THEN** that downtime row MUST appear on the all-systems review sheet and MUST NOT be required on the low-SLI sheet

#### Scenario: Same columns as sheet 1

- **WHEN** rows are written to the all-systems review sheet
- **THEN** columns MUST be system name, system category, empty interrupt reason, downtime start, downtime end, and owner name

### Requirement: Owner column uses existing resolver

The owner column MUST be filled from `sla_system_owner` using exact `(system_level, system_name)` where `system_level` is the sheet’s system category (`子系统` or `父系统`). The cell MUST contain that row’s `owner_name`. Resolution MUST NOT use `owners.rules` region or TRADE/BID matching.

#### Scenario: Matched owner name written

- **WHEN** a dictionary row exists for the row’s level and system name
- **THEN** the owner cell MUST contain that dictionary `owner_name`

### Requirement: Low-SLI sheet shares data rules with Pass B

The low-SLI review sheet and review-event Pass B MUST use the same annual SLI threshold, low-SLI inventory, full-panel downtime index query, dashboard matching, and exclusion filter (`未排除` or `-`).

#### Scenario: Excel row eligible implies Pass B candidate when pre-window

- **WHEN** a row appears on the low-SLI review sheet, its downtime start is before configured window-start, and no table row exists for that start identity
- **THEN** the next successful default review-event sync Pass B MUST insert that row unless Grafana data changed between export and sync

### Requirement: Excel export ignores window-start for row eligibility

The low-SLI review sheet MUST NOT drop rows because downtime start is before configured `weekly-review.window-start`. Pass B alone applies that filter for table inserts.

#### Scenario: Excel export not filtered by window-start

- **WHEN** a low-SLI system has an unexcluded downtime starting before configured window-start
- **THEN** that row MUST remain eligible for the low-SLI review sheet
