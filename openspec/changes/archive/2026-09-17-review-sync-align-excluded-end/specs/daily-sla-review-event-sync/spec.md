## MODIFIED Requirements

### Requirement: Daily unexcluded downtime collect window

When the review-event sync job runs (scheduled or manual HTTP), the system SHALL load downtime rows from both the subsystem and parent downtime dashboards. A row MUST be included in the Grafana working set when downtime start is parseable and lies in `[configured window-start, trigger instant]` inclusive (default window-start `2026-07-30 00:00:00` in `Asia/Shanghai`). Exclusion status MUST NOT drop the row from this working set. The Grafana `/api/ds/query` time range for this collect MUST start at the configured window-start (`Asia/Shanghai` epoch millis) and MUST end at the trigger instant (MUST NOT use a default rolling window such as `now-1h`). Annual SLI MUST NOT filter rows. Owner names MUST come from `sla_system_owner` matched on the row’s `system_level` and `system_name`. Insert into `sla_review_event` remains restricted as specified by idempotent insert (only `未排除` or `-`).

#### Scenario: In-window unexcluded row is collected

- **WHEN** a subsystem downtime starts at `2026-07-30 13:14:54` with exclusion `未排除` and the job triggers later
- **THEN** that row MUST be in the sync working set with level `子系统`

#### Scenario: Start before window is omitted

- **WHEN** a downtime starts before the configured window-start
- **THEN** that row MUST NOT be inserted by this job

#### Scenario: Owner name from dictionary

- **WHEN** a collected downtime has `system_level` and `system_name` matching a dictionary row
- **THEN** the working-set owner name MUST equal that dictionary `owner_name`

#### Scenario: Query range covers window-start not last hour

- **WHEN** the job runs and window-start is `2026-07-30 00:00:00` Shanghai
- **THEN** the Grafana query `from` MUST be that instant’s epoch millis and MUST NOT be `now-1h`

#### Scenario: Partial exclusion is still collected

- **WHEN** a parent downtime starts in-window with exclusion `部分排除`
- **THEN** that row MUST be in the sync working set

### Requirement: Idempotent insert into sla_review_event

The system SHALL insert working-set rows into `zabbix.sla_review_event` with `period_month` equal to the downtime start timestamp’s `yyyyMM` in `Asia/Shanghai` **only when** Grafana exclusion status is `未排除` or `-`. Columns written on insert MUST be limited to period_month, system_level, system_name, down_start, down_end, and owner_name. If no row exists for the start identity `(period_month, system_level, system_name, down_start)` and the row is unexcluded, the system MUST insert. If exclusion is not `未排除` or `-` and no start identity exists, the system MUST NOT insert. If a row already exists for that start identity, the system MUST NOT insert another row for a different `down_end`; it MUST instead align `down_end` as specified by overwriting end from Grafana for the same start. Unparseable or blank/`未恢复` end times MUST be stored as `9999-12-31 00:00:00` on insert. Human columns MUST NOT be overwritten except by the fold copy-blank rules.

#### Scenario: period_month from start time

- **WHEN** a downtime starts at `2026-07-30 13:14:54` and is inserted
- **THEN** `period_month` MUST be `202607` even if the job runs in August

#### Scenario: Duplicate start is not inserted again

- **WHEN** the same level, name, and `down_start` (and derived period_month) already exist
- **THEN** the job MUST NOT insert a second row for a new Grafana `down_end`

#### Scenario: Unrecovered end sentinel

- **WHEN** downtime end is `未恢复` or blank
- **THEN** `down_end` MUST be stored as `9999-12-31 00:00:00`

#### Scenario: Human long text is not overwritten on insert skip

- **WHEN** an existing row has non-null `root_cause` and Grafana repeats the same start identity
- **THEN** the job MUST leave `root_cause`, `resolution_process`, and `improvement` unchanged except where fold copies a blank destination from a deleted duplicate

#### Scenario: Excluded Grafana row does not insert

- **WHEN** Grafana exclusion is `部分排除` and no `sla_review_event` row exists for that start identity
- **THEN** the job MUST NOT insert a row

### Requirement: Overwrite down_end from Grafana for the same start identity

When the Grafana collect pass runs (scheduled or POST that collects Grafana), after considering insert, if the collected mapped `down_end` is a parseable recovery time (not sentinel `9999-12-31 00:00:00`), the system MUST treat all `sla_review_event` rows with the same `period_month`, `system_level`, `system_name`, and `down_start` as one downtime.

If none exist and Grafana exclusion is `未排除` or `-`, the system MUST insert as today. If none exist and exclusion is not `未排除` or `-`, the system MUST skip. If any exist, the system MUST NOT insert. It MUST keep one row whose `down_end` equals the collected time when such a row exists; otherwise it MUST UPDATE one kept row’s `down_end` to the collected time. The kept row MUST be the existing row with non-blank `count_sla` if any, otherwise the smallest `id`. Human columns on the kept row MUST NOT change except blank fields filled from a folded duplicate. If the kept row’s `count_sla` is `否`, `exclude_time` MUST equal the new `down_end`. If `count_sla` is `是` or blank, `exclude_time` MUST NOT change.

Every other row for that start identity MUST be folded: copy non-blank human columns onto the kept row only where the kept column is blank, then DELETE the duplicate. After the pass exactly one row MUST remain for that start identity with the collected `down_end`.

When `exclude_time` was set to the new `down_end` (`否`), the system MUST UPDATE `period_to` on any existing `sla_excluded_downtime` row for that system’s dictionary pool `slaid` and `period_from` equal to `down_start` Unix seconds. If no exclusion row exists, the system MUST NOT insert one. This path MUST NOT change exclusion `name`. A missing `sla` or failed exclusion UPDATE MUST NOT fail the whole sync job. Grafana exclusion `部分排除` or `全部排除` MUST NOT block this overwrite or `period_to` UPDATE.

#### Scenario: Sentinel unrecovered becomes Grafana recovery

- **WHEN** the table has start `2026-09-13 13:02:18` and `down_end` `9999-12-31 00:00:00`, and collect has the same start with end `2026-09-13 22:27:49`
- **THEN** that row’s `down_end` MUST become `2026-09-13 22:27:49` and MUST NOT insert a second row

#### Scenario: Existing recovered end is overwritten

- **WHEN** the table row has `down_end` `2026-09-13 22:27:49` and collect has `2026-09-13 23:00:00` for the same start identity
- **THEN** that row’s `down_end` MUST become `2026-09-13 23:00:00` and MUST NOT insert another row

#### Scenario: 否 exclude_time and exclusion period_to follow new end

- **WHEN** a kept row with `count_sla` `否` is updated to a new `down_end` and an exclusion row exists for that `slaid` and `period_from`
- **THEN** `exclude_time` MUST equal the new `down_end` and that exclusion row’s `period_to` MUST equal the Unix seconds of the new `down_end`

#### Scenario: Two recovered ends fold

- **WHEN** two rows exist for the same start with different recovered `down_end` values and collect has one of those ends
- **THEN** exactly one row MUST remain with the collected `down_end` and the other MUST be deleted

#### Scenario: Partial exclusion overwrites existing review end

- **WHEN** Grafana exclusion is `部分排除`, start is `2026-09-13 13:02:18`, collected end is `2026-09-13 22:27:49`, and a review row already exists for that start identity with a different `down_end`
- **THEN** that row’s `down_end` MUST become `2026-09-13 22:27:49` and MUST NOT insert a second row
