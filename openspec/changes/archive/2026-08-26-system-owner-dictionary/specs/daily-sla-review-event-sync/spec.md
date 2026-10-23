## MODIFIED Requirements

### Requirement: Daily unexcluded downtime collect window

When the review-event sync job runs (scheduled or manual HTTP), the system SHALL load downtime rows from both the subsystem and parent downtime dashboards. A row MUST be included only when downtime start is parseable and lies in `[configured window-start, trigger instant]` inclusive (default window-start `2026-07-30 00:00:00` in `Asia/Shanghai`) and exclusion status is `未排除` or `-`. Annual SLI MUST NOT filter rows. Owner names MUST come from `sla_system_owner` matched on the row’s `system_level` and `system_name`.

#### Scenario: In-window unexcluded row is collected

- **WHEN** a subsystem downtime starts at `2026-07-30 13:14:54` with exclusion `未排除` and the job triggers later
- **THEN** that row MUST be in the sync working set with level `子系统`

#### Scenario: Start before window is omitted

- **WHEN** a downtime starts before the configured window-start
- **THEN** that row MUST NOT be inserted by this job

#### Scenario: Owner name from dictionary

- **WHEN** a collected downtime has `system_level` and `system_name` matching a dictionary row
- **THEN** the working-set owner name MUST equal that dictionary `owner_name`

### Requirement: Manual POST can refresh owner without insert

The POST MUST accept a query parameter `refreshOwner` whose default is false. When omitted or false, the POST MUST use the idempotent insert behavior (new rows insert, existing unique keys skip including `owner_name`). When `refreshOwner` is true, the system MUST collect the same Grafana working set, MUST update only `owner_name` (and `updated_at`) on rows that match the unique key, MUST NOT insert any row, and MUST NOT change fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, or improvement. Collect rows with no matching unique key MUST be skipped. Cron MUST NOT use refresh-owner mode. The `owner_name` written MUST be the dictionary value for that row’s `system_level` and `system_name`.

#### Scenario: Refresh owner updates existing row only

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime already exists for the unique key
- **THEN** the system MUST set that row’s `owner_name` from `sla_system_owner` for the same `system_level` and `system_name` and MUST NOT insert a second row and MUST NOT change `root_cause`

#### Scenario: Refresh owner skips downtime not in table

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime has no matching unique key
- **THEN** the system MUST NOT insert that downtime
