## MODIFIED Requirements

### Requirement: Idempotent insert into sla_review_event

The system SHALL insert working-set rows into `zabbix.sla_review_event` with `period_month` equal to the downtime start timestamp’s `yyyyMM` in `Asia/Shanghai`. Columns written MUST be limited to period_month, system_level, system_name, down_start, down_end, and owner_name. If a row already exists for the unique key `(period_month, system_level, system_name, down_start, down_end)`, the system MUST skip that insert and MUST NOT update fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, or owner_name. Unparseable or blank/`未恢复` end times MUST be stored as `9999-12-31 00:00:00`.

#### Scenario: period_month from start time

- **WHEN** a downtime starts at `2026-07-30 13:14:54` and is inserted
- **THEN** `period_month` MUST be `202607` even if the job runs in August

#### Scenario: Duplicate is skipped

- **WHEN** the same four identity fields plus derived period_month already exist
- **THEN** the job MUST NOT insert a second row and MUST NOT change existing fault_category

#### Scenario: Unrecovered end sentinel

- **WHEN** downtime end is `未恢复` or blank
- **THEN** `down_end` MUST be stored as `9999-12-31 00:00:00`

#### Scenario: Human long text is not overwritten

- **WHEN** an existing row has non-null `root_cause` and the same unique key appears again in collect
- **THEN** the job MUST leave `root_cause`, `resolution_process`, and `improvement` unchanged
