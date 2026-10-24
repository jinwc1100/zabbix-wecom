# Spec Delta

## MODIFIED Requirements

### Requirement: Daily unexcluded downtime collect window

When the review-event sync job runs the **window collect pass (Pass A)** (scheduled or manual HTTP default insert sync), the system SHALL load downtime rows from both the subsystem and parent downtime dashboards. A row MUST be included in the Pass A working set when downtime start is parseable and lies in `[configured window-start, trigger instant]` inclusive (default window-start `2026-07-30 00:00:00` in `Asia/Shanghai`). Exclusion status MUST NOT drop the row from this working set. The Grafana `/api/ds/query` time range for Pass A MUST start at the configured window-start (`Asia/Shanghai` epoch millis) and MUST end at the trigger instant (MUST NOT use a default rolling window such as `now-1h`). Annual SLI MUST NOT filter Pass A rows. Owner names MUST come from `sla_system_owner` matched on the row’s `system_level` and `system_name`. Insert into `sla_review_event` from Pass A remains restricted as specified by idempotent insert (only `未排除` or `-`).

#### Scenario: In-window unexcluded row is collected

- **WHEN** a subsystem downtime starts at `2026-07-30 13:14:54` with exclusion `未排除` and the job triggers later
- **THEN** that row MUST be in the sync working set with level `子系统`

#### Scenario: Start before window is omitted

- **WHEN** a downtime starts before the configured window-start
- **THEN** that row MUST NOT be in the Pass A working set and MUST NOT be inserted by Pass A

#### Scenario: Owner name from dictionary

- **WHEN** a collected downtime has `system_level` and `system_name` matching a dictionary row
- **THEN** the working-set owner name MUST equal that dictionary `owner_name`

#### Scenario: Query range covers window-start not last hour

- **WHEN** Pass A runs and window-start is `2026-07-30 00:00:00` Shanghai
- **THEN** the Grafana query `from` MUST be that instant’s epoch millis and MUST NOT be `now-1h`

#### Scenario: Partial exclusion is still collected

- **WHEN** a parent downtime starts in-window with exclusion `部分排除`
- **THEN** that row MUST be in the Pass A working set

## ADDED Requirements

### Requirement: Low-SLI pre-window backfill pass runs with default sync

On each default review-event Grafana insert sync, the system SHALL run **Pass B** in the same hourly job as Pass A, immediately before Pass A. Pass B MUST NOT run on `refreshOwner`, `refreshExcludeTime`-only, or routing-only requests. Cron MUST NOT use a separate schedule for Pass B.

#### Scenario: Pass B runs before Pass A on hourly cron

- **WHEN** `review-sync.enabled` is true and the scheduled sync runs at 10:00 Shanghai
- **THEN** the system MUST run Pass B then Pass A in one job and MUST NOT schedule Pass B separately

#### Scenario: Refresh owner skips Pass B

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshOwner=true`
- **THEN** the system MUST NOT run Pass B

### Requirement: Low-SLI pre-window candidate selection

Pass B MUST select systems whose annual SLI is below the configured threshold using the same rule as review Excel low-SLI sheet. It MUST load downtime indexes with the same full-panel Grafana query range as review Excel export. It MUST match systems to leaf or parent downtime by name and SLI source level. It MUST consider only rows with exclusion `未排除` or `-` and parseable start strictly before configured window-start.

#### Scenario: Pre-window low-SLI row inserts

- **WHEN** a subsystem has annual SLI below threshold, a downtime starts at `2026-06-01 10:00:00` with exclusion `未排除`, and no `sla_review_event` row exists for that start identity
- **THEN** Pass B MUST insert one row with `count_sla` NULL

#### Scenario: Pre-window partial exclusion does not insert

- **WHEN** a low-SLI system has a pre-window downtime with exclusion `部分排除` and no matching start identity in the table
- **THEN** Pass B MUST NOT insert a row

#### Scenario: SLI recovery does not remove rows

- **WHEN** a row was inserted by Pass B and the system’s annual SLI later rises to at or above the threshold
- **THEN** subsequent Pass B runs MUST NOT delete that row

### Requirement: Low-SLI pre-window insert-only behavior

Pass B MUST insert eligible rows with the same idempotent insert rules as Pass A. It MUST NOT delete rows when SLI recovers. It MUST NOT run end overwrite, fold, or exclusion `period_to` alignment from Pass B collect.

#### Scenario: Pre-window row already in table skips insert

- **WHEN** Pass B sees a pre-window eligible downtime whose start identity already exists
- **THEN** Pass B MUST NOT insert a duplicate and MUST NOT change `down_end` from Pass B collect

### Requirement: Pre-window backfill sync result visibility

When the default insert sync runs Pass B, the sync result exposed to HTTP and logs MUST report Pass B insert counts separately from Pass A insert counts (field names or message segments are implementation-defined but MUST be distinguishable).

#### Scenario: Message mentions pre-window inserts

- **WHEN** Pass B inserts at least one new row during default sync
- **THEN** the sync result MUST indicate how many rows Pass B inserted
