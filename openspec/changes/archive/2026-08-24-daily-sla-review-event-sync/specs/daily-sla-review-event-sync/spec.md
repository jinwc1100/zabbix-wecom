## ADDED Requirements

### Requirement: Daily unexcluded downtime collect window

When the review-event sync job runs (scheduled or manual HTTP), the system SHALL load downtime rows from both the subsystem and parent downtime dashboards. A row MUST be included only when downtime start is parseable and lies in `[configured window-start, trigger instant]` inclusive (default window-start `2026-07-30 00:00:00` in `Asia/Shanghai`) and exclusion status is `未排除` or `-`. Annual SLI MUST NOT filter rows. Owner names MUST come from the existing owner-rule resolver without changing those rules.

#### Scenario: In-window unexcluded row is collected

- **WHEN** a subsystem downtime starts at `2026-07-30 13:14:54` with exclusion `未排除` and the job triggers later
- **THEN** that row MUST be in the sync working set with level `子系统`

#### Scenario: Start before window is omitted

- **WHEN** a downtime starts before the configured window-start
- **THEN** that row MUST NOT be inserted by this job

### Requirement: Idempotent insert into sla_review_event

The system SHALL insert working-set rows into `zabbix.sla_review_event` with `period_month` equal to the downtime start timestamp’s `yyyyMM` in `Asia/Shanghai`. Columns written MUST be limited to period_month, system_level, system_name, down_start, down_end, and owner_name. If a row already exists for the unique key `(period_month, system_level, system_name, down_start, down_end)`, the system MUST skip that insert and MUST NOT update fault_category, fault_subcategory, fault_desc, count_sla, remark, or owner_name. Unparseable or blank/`未恢复` end times MUST be stored as `9999-12-31 00:00:00`.

#### Scenario: period_month from start time

- **WHEN** a downtime starts at `2026-07-30 13:14:54` and is inserted
- **THEN** `period_month` MUST be `202607` even if the job runs in August

#### Scenario: Duplicate is skipped

- **WHEN** the same four identity fields plus derived period_month already exist
- **THEN** the job MUST NOT insert a second row and MUST NOT change existing fault_category

#### Scenario: Unrecovered end sentinel

- **WHEN** downtime end is `未恢复` or blank
- **THEN** `down_end` MUST be stored as `9999-12-31 00:00:00`

### Requirement: Seventeen-hundred schedule and manual POST

The system SHALL register a daily job at 17:00 `Asia/Shanghai` that runs this sync when `review-sync.enabled` is true. Disabling the switch MUST skip cron only. The system SHALL expose an HTTP POST that runs the same sync regardless of the switch. `POST /api/sla/notify` MUST remain the annual-SLI pipeline and MUST NOT write `sla_review_event`.

#### Scenario: Cron at 17:00 when enabled

- **WHEN** `review-sync.enabled` is true and the clock is 17:00
- **THEN** the system MUST run the review-event sync

#### Scenario: Switch off skips cron

- **WHEN** `review-sync.enabled` is false and 17:00 arrives
- **THEN** the system MUST NOT run this sync from cron

#### Scenario: Manual POST ignores switch

- **WHEN** a client calls the review-event-sync POST while the switch is disabled
- **THEN** the system MUST still run the sync
