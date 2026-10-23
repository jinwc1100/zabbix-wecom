## ADDED Requirements

### Requirement: Manual POST can refresh exclude_time from the table

The POST MUST accept a query parameter `refreshExcludeTime` whose default is false. When `refreshExcludeTime` is true, the system MUST NOT collect Grafana downtime for this mode, MUST NOT insert any row, and MUST recompute `exclude_time` on existing `sla_review_event` rows using the same derivation as review save: `count_sla` `是` → `down_start` plus one second unless that instant is after or equal to `down_end` then `down_end`; `count_sla` `否` → `down_end`; NULL or blank `count_sla` → `exclude_time` MUST be set NULL. The refresh MUST NOT change fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, owner_name, down_start, or down_end. Cron MUST NOT use this mode. This flag is independent of `refreshOwner`; both MAY be true on one request.

#### Scenario: Refresh sets yes rows from start plus one second

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `是` with `down_start` plus one second before `down_end`
- **THEN** that row’s `exclude_time` MUST equal `down_start` plus one second and the system MUST NOT insert rows from Grafana for this flag

#### Scenario: Refresh sets no rows to down_end

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `否`
- **THEN** that row’s `exclude_time` MUST equal `down_end`

#### Scenario: Refresh clears incomplete rows

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has empty `count_sla`
- **THEN** that row’s `exclude_time` MUST be NULL

## MODIFIED Requirements

### Requirement: Idempotent insert into sla_review_event

The system SHALL insert working-set rows into `zabbix.sla_review_event` with `period_month` equal to the downtime start timestamp’s `yyyyMM` in `Asia/Shanghai`. Columns written MUST be limited to period_month, system_level, system_name, down_start, down_end, and owner_name. If a row already exists for the unique key `(period_month, system_level, system_name, down_start, down_end)`, the system MUST skip that insert and MUST NOT update fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, owner_name, or exclude_time. Unparseable or blank/`未恢复` end times MUST be stored as `9999-12-31 00:00:00`.

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

### Requirement: Manual POST can refresh owner without insert

The POST MUST accept a query parameter `refreshOwner` whose default is false. When both `refreshOwner` and `refreshExcludeTime` are omitted or false, the POST MUST use the idempotent insert behavior (new rows insert, existing unique keys skip including `owner_name`). When `refreshOwner` is true, the system MUST collect the same Grafana working set, MUST update only `owner_name` (and `updated_at`) on rows that match the unique key, MUST NOT insert any row, and MUST NOT change fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, or exclude_time. Collect rows with no matching unique key MUST be skipped. Cron MUST NOT use refresh-owner mode. The `owner_name` written MUST be the dictionary value for that row’s `system_level` and `system_name`.

#### Scenario: Refresh owner updates existing row only

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime already exists for the unique key
- **THEN** the system MUST set that row’s `owner_name` from `sla_system_owner` for the same `system_level` and `system_name` and MUST NOT insert a second row and MUST NOT change `root_cause`

#### Scenario: Refresh owner skips downtime not in table

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime has no matching unique key
- **THEN** the system MUST NOT insert that downtime
