# daily-sla-review-event-sync

## Purpose

每日将 Grafana 未排除宕机幂等写入 `zabbix.sla_review_event`，供复盘页查询。冲突时不覆盖人填字段。

## Requirements

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

### Requirement: Manual POST can refresh owner without insert

The POST MUST accept a query parameter `refreshOwner` whose default is false. When both `refreshOwner` and `refreshExcludeTime` are omitted or false, the POST MUST use the idempotent insert behavior (new rows insert, existing unique keys skip including `owner_name`) plus overwrite/align of parseable Grafana `down_end` for the same start identity (MUST NOT insert a second end). When `refreshOwner` is true, the system MUST collect the same Grafana working set, MUST update only `owner_name` (and `updated_at`) on rows that match `period_month`, `system_level`, `system_name`, and `down_start` (MUST NOT require `down_end` to match), MUST NOT insert any row, and MUST NOT change fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, or exclude_time. Collect rows with no matching start identity MUST be skipped. Cron MUST NOT use refresh-owner mode. The `owner_name` written MUST be the dictionary value for that row’s `system_level` and `system_name`.

#### Scenario: Refresh owner updates existing row only

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime already exists for the unique key
- **THEN** the system MUST set that row’s `owner_name` from `sla_system_owner` for the same `system_level` and `system_name` and MUST NOT insert a second row and MUST NOT change `root_cause`

#### Scenario: Refresh owner skips downtime not in table

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true` and a collected downtime has no matching unique key
- **THEN** the system MUST NOT insert that downtime

#### Scenario: Refresh owner matches after recovery

- **WHEN** the table row has sentinel `down_end` and collect has a parseable recovery `down_end` for the same start identity and `refreshOwner` is true
- **THEN** that row’s `owner_name` MUST still be updated

### Requirement: Manual POST can refresh exclude_time from the table

The POST MUST accept a query parameter `refreshExcludeTime` whose default is false. When `refreshExcludeTime` is true, the system MUST NOT collect Grafana downtime for this mode, MUST NOT insert any row, and MUST recompute `exclude_time` on existing `sla_review_event` rows using the same derivation as review save: `count_sla` `是` → `down_start` plus one second (MUST NOT clamp to `down_end`); `count_sla` `否` → `down_end`; NULL or blank `count_sla` → `exclude_time` MUST be set NULL. The refresh MUST NOT change fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, owner_name, down_start, or down_end. Cron MUST NOT use this mode. This flag is independent of `refreshOwner`; both MAY be true on one request.

#### Scenario: Refresh sets yes rows from start plus one second

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `是` with `down_start` plus one second before `down_end`
- **THEN** that row’s `exclude_time` MUST equal `down_start` plus one second and the system MUST NOT insert rows from Grafana for this flag

#### Scenario: Refresh sets yes rows from start plus one second when not before down_end

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `是` and `down_start` plus one second is after or equal to `down_end`
- **THEN** that row’s `exclude_time` MUST equal `down_start` plus one second

#### Scenario: Refresh sets no rows to down_end

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `否`
- **THEN** that row’s `exclude_time` MUST equal `down_end`

#### Scenario: Refresh clears incomplete rows

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has empty `count_sla`
- **THEN** that row’s `exclude_time` MUST be NULL

### Requirement: Daily 17:00 schedule and manual POST

The system SHALL register a daily job at 17:00 `Asia/Shanghai` that runs this sync when `review-sync.enabled` is true. The shipped default for `review-sync.enabled` MUST be true. Disabling the switch MUST skip the scheduled sync and MUST skip the incomplete-review WeCom nudge from cron. The system SHALL expose an HTTP POST that runs the same sync regardless of the switch. After a successful scheduled sync, the system MUST invoke the incomplete-review WeCom nudge. The POST MUST accept a query parameter `notify` whose default is false: when omitted or false the system MUST sync and MUST NOT send the incomplete-review WeCom nudge; when true and the sync succeeds, the system MUST invoke the same incomplete-review WeCom nudge. If sync fails, the system MUST NOT send the nudge. `POST /api/sla/notify` MUST remain the annual-SLI pipeline and MUST NOT write `sla_review_event`.

#### Scenario: Cron at 17:00 when enabled

- **WHEN** `review-sync.enabled` is true and the clock is 17:00
- **THEN** the system MUST run the review-event sync

#### Scenario: Scheduled sync then nudge

- **WHEN** the scheduled sync completes successfully
- **THEN** the system MUST run the incomplete-review WeCom nudge

#### Scenario: Switch off skips cron

- **WHEN** `review-sync.enabled` is false and 17:00 arrives
- **THEN** the system MUST NOT run this sync from cron and MUST NOT send the incomplete-review WeCom nudge from cron

#### Scenario: Manual POST ignores switch and does not nudge by default

- **WHEN** a client calls `POST /api/sla/review-event-sync` without `notify` or with `notify=false`, while the switch is disabled, and the sync succeeds
- **THEN** the system MUST still run the sync and MUST NOT send the incomplete-review WeCom nudge

#### Scenario: Manual POST nudges when notify is true

- **WHEN** a client calls `POST /api/sla/review-event-sync?notify=true` and the sync succeeds
- **THEN** the system MUST run the incomplete-review WeCom nudge

#### Scenario: Failed sync does not nudge

- **WHEN** the review-event sync throws or otherwise fails (scheduled or POST)
- **THEN** the system MUST NOT send the incomplete-review WeCom nudge
