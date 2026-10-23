## ADDED Requirements

### Requirement: Review event schema without remark

The system SHALL ensure `zabbix.sla_review_event` has nullable TEXT columns `resolution_process`, `root_cause`, and `improvement`, and SHALL NOT keep a `remark` column. New-database `CREATE TABLE` MUST match this shape. On startup against an existing table, the system MUST add missing long-text columns and MUST drop `remark` if it exists. Daily sync inserts MUST still succeed when those three columns are NULL.

#### Scenario: Existing table is altered

- **WHEN** the application starts against a table that still has `remark` and lacks the three long-text columns
- **THEN** the table MUST have `resolution_process`, `root_cause`, and `improvement` as nullable TEXT and MUST NOT have `remark`

#### Scenario: Sync insert with empty review fields

- **WHEN** daily sync inserts a new downtime row
- **THEN** the insert MUST succeed with the three long-text columns NULL

### Requirement: Fuzzy list of review events

The system SHALL list `sla_review_event` rows filtered by optional query parameters for system name, time, and owner. Each provided parameter MUST be a case-sensitive substring match (SQL `LIKE` with wildcards escaped or treated as literals of user text). System name MUST match `system_name`. Owner MUST match `owner_name`. Time MUST match a concatenation of formatted `down_start` and `down_end` (`yyyy-MM-dd HH:mm:ss`). Empty parameters MUST be ignored. Results MUST include machine columns, human review columns, and `id`.

#### Scenario: Combined filters

- **WHEN** a client queries with system `大同`, time `2026-07-30`, and owner `卓`
- **THEN** every returned row MUST contain those substrings in system name, formatted start or end, and owner respectively

#### Scenario: Empty filters return all

- **WHEN** a client queries with no filter values
- **THEN** the system MUST return all review event rows

### Requirement: Per-row save with required human fields

The system SHALL update one `sla_review_event` row by `id` with fault category, subcategory, description, resolution process, root cause, and improvement. All six MUST be non-blank after trim or the update MUST fail without writing. The system MUST resolve `count_sla` from `sla_fault_category` for the category+subcategory pair and MUST reject unknown pairs. The update MUST NOT change `system_level`, `system_name`, `down_start`, `down_end`, `owner_name`, or `period_month`.

#### Scenario: Complete row saves

- **WHEN** a client PUTs a known id with all six fields filled and a valid category pair
- **THEN** the row MUST persist those fields and `count_sla` from the dictionary

#### Scenario: Missing long text rejected

- **WHEN** a client PUTs with category, subcategory, and description filled but empty `root_cause`
- **THEN** the system MUST NOT update the row and MUST return a client error

#### Scenario: Unknown subcategory rejected

- **WHEN** a client PUTs a category and subcategory that are not in `sla_fault_category`
- **THEN** the system MUST NOT update the row and MUST return a client error

### Requirement: Vue review page layout

The system SHALL serve a Vue review page that lists events from the list API, offers fuzzy inputs for system, time, and owner, and saves one row at a time via the save API. Machine fields MUST be read-only. Fault category and subcategory MUST cascade; `count_sla` MUST display from the dictionary after subcategory selection and MUST NOT be a free-text input. Fault description MUST appear on the main table. Resolution process, root cause, and improvement MUST appear only in an expand/drawer region, with a main-table completeness indicator of how many of those three are non-blank (`n/3`). The page MUST NOT include a remark field. Save MUST be blocked or rejected in the UI when any of the six required fields is blank. Unrecovered rows MUST use the same layout as recovered rows.

#### Scenario: Expand for long text

- **WHEN** the user opens the review expand control on a row
- **THEN** the page MUST show inputs for 解决过程, 根本原因, and 改进措施 and MUST NOT show 备注

#### Scenario: Incomplete save disabled

- **WHEN** a row is missing any of the six required fields
- **THEN** the per-row save control MUST NOT persist that row
