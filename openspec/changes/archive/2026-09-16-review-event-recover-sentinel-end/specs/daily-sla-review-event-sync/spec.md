## ADDED Requirements

### Requirement: Promote sentinel down_end when Grafana recovers

When the Grafana collect pass runs (scheduled sync or POST without `refreshExcludeTime`-only), after the idempotent insert attempt, if the collected row’s mapped `down_end` is a parseable recovery time (not `9999-12-31 00:00:00`), the system MUST treat existing `sla_review_event` rows with the same `period_month`, `system_level`, `system_name`, and `down_start` as the same downtime.

If exactly one such row has sentinel `down_end` `9999-12-31 00:00:00` and no row has the collected recovery `down_end`, the system MUST UPDATE that row’s `down_end` to the collected time and MUST NOT insert another row. Human columns `fault_category`, `fault_subcategory`, `fault_desc`, `count_sla`, `resolution_process`, `root_cause`, and `improvement` MUST NOT change. If that row’s `count_sla` is `否`, `exclude_time` MUST be set to the new `down_end`. If `count_sla` is `是` or blank, `exclude_time` MUST NOT change. The system MUST NOT write `sla_excluded_downtime` during this promote.

If a row with the collected recovery `down_end` already exists and a sentinel row exists for the same start identity, the system MUST copy non-blank human columns from the sentinel onto the recovered row only where the recovered row’s corresponding column is blank, then MUST delete the sentinel row, and MUST NOT leave two rows for that start identity.

#### Scenario: Sentinel row is updated to Grafana recovery time

- **WHEN** the table has `北京市级多行业交易系统交易平台` with `down_start` `2026-09-11 20:08:34` and `down_end` `9999-12-31 00:00:00`, and collect has the same start with end `2026-09-13 11:23:28`
- **THEN** that row’s `down_end` MUST become `2026-09-13 11:23:28` and the table MUST NOT gain a second row for that start

#### Scenario: Reviewed 否 exclude_time follows new end

- **WHEN** a sentinel row with `count_sla` `否` is promoted to a recovered `down_end`
- **THEN** that row’s `exclude_time` MUST equal the new `down_end`

#### Scenario: Duplicate sentinel and recovered is folded

- **WHEN** both a sentinel row and a recovered-end row exist for the same level, name, and `down_start`, and collect has the recovered end
- **THEN** the sentinel row MUST be deleted and exactly one row MUST remain with the recovered `down_end`

## MODIFIED Requirements

### Requirement: Idempotent insert into sla_review_event

The system SHALL insert working-set rows into `zabbix.sla_review_event` with `period_month` equal to the downtime start timestamp’s `yyyyMM` in `Asia/Shanghai`. Columns written MUST be limited to period_month, system_level, system_name, down_start, down_end, and owner_name. If a row already exists for the unique key `(period_month, system_level, system_name, down_start, down_end)`, the system MUST skip that insert and MUST NOT update fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, owner_name, or exclude_time **except** as specified by promoting a sentinel `down_end` when Grafana later has a parseable recovery time. Unparseable or blank/`未恢复` end times MUST be stored as `9999-12-31 00:00:00`.

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

The POST MUST accept a query parameter `refreshOwner` whose default is false. When both `refreshOwner` and `refreshExcludeTime` are omitted or false, the POST MUST use the idempotent insert behavior (new rows insert, existing unique keys skip including `owner_name`) plus sentinel promote. When `refreshOwner` is true, the system MUST collect the same Grafana working set, MUST update only `owner_name` (and `updated_at`) on rows that match `period_month`, `system_level`, `system_name`, and `down_start` (MUST NOT require `down_end` to match), MUST NOT insert any row, and MUST NOT change fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, or exclude_time. Collect rows with no matching start identity MUST be skipped. Cron MUST NOT use refresh-owner mode. The `owner_name` written MUST be the dictionary value for that row’s `system_level` and `system_name`.

#### Scenario: Refresh owner updates existing row only

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime already exists for the unique key
- **THEN** the system MUST set that row’s `owner_name` from `sla_system_owner` for the same `system_level` and `system_name` and MUST NOT insert a second row and MUST NOT change `root_cause`

#### Scenario: Refresh owner skips downtime not in table

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime has no matching unique key
- **THEN** the system MUST NOT insert that downtime

#### Scenario: Refresh owner matches after recovery

- **WHEN** the table row has sentinel `down_end` and collect has a parseable recovery `down_end` for the same start identity and `refreshOwner` is true
- **THEN** that row’s `owner_name` MUST still be updated
