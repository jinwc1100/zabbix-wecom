## ADDED Requirements

### Requirement: exclude_time column on sla_review_event

The system SHALL ensure `zabbix.sla_review_event` has a nullable `DATETIME` column named `exclude_time`. New-database `CREATE TABLE` MUST include this column. On startup against an existing table that lacks it, the system MUST add it. Daily sync inserts MUST still succeed when `exclude_time` is NULL.

#### Scenario: Existing table is altered

- **WHEN** the application starts against a table that has no `exclude_time` column
- **THEN** the table MUST have nullable `DATETIME` `exclude_time`

#### Scenario: Sync insert leaves exclude_time null

- **WHEN** daily sync inserts a new downtime row
- **THEN** the insert MUST succeed with `exclude_time` NULL

### Requirement: Save derives exclude_time from count_sla

When a per-row review save succeeds, the system SHALL set that row’s `exclude_time` from the persisted `count_sla` and the row’s existing `down_start` and `down_end` as follows: if `count_sla` is `是`, `exclude_time` MUST be `down_start` plus one second, except when that instant is after or equal to `down_end`, in which case `exclude_time` MUST equal `down_end`; if `count_sla` is `否`, `exclude_time` MUST equal `down_end`. The save MUST NOT change `down_start` or `down_end`.

#### Scenario: Count yes uses start plus one second

- **WHEN** a client saves a row whose dictionary `count_sla` is `是` and `down_start` plus one second is before `down_end`
- **THEN** `exclude_time` MUST equal `down_start` plus one second

#### Scenario: Count yes clamps to down_end when shorter than one second

- **WHEN** a client saves a row whose dictionary `count_sla` is `是` and `down_start` plus one second is after or equal to `down_end`
- **THEN** `exclude_time` MUST equal `down_end`

#### Scenario: Count no uses down_end

- **WHEN** a client saves a row whose dictionary `count_sla` is `否`
- **THEN** `exclude_time` MUST equal that row’s `down_end`

### Requirement: Review pages hide exclude_time

The review overview page, the review list page (`/review/events.html`), and matching demo HTML MUST NOT display `exclude_time` (no column, label, or input).

#### Scenario: List page has no exclude_time UI

- **WHEN** a user opens `/review/events.html`
- **THEN** the page MUST NOT show a control or table column for `exclude_time`

#### Scenario: Overview has no exclude_time UI

- **WHEN** a user opens `/review/index.html`
- **THEN** the page MUST NOT show `exclude_time`

## MODIFIED Requirements

### Requirement: Per-row save with required human fields

The system SHALL update one `sla_review_event` row by `id` with fault category, subcategory, description, resolution process, root cause, and improvement. All six MUST be non-blank after trim or the update MUST fail without writing. The system MUST resolve `count_sla` from `sla_fault_category` for the category+subcategory pair and MUST reject unknown pairs. The update MUST NOT change `system_level`, `system_name`, `down_start`, `down_end`, `owner_name`, or `period_month`. On a successful update the system MUST also persist `exclude_time` using the save-derivation rules for that row’s `count_sla`, `down_start`, and `down_end`.

#### Scenario: Complete row saves

- **WHEN** a client PUTs a known id with all six fields filled and a valid category pair
- **THEN** the row MUST persist those fields and `count_sla` from the dictionary

#### Scenario: Missing long text rejected

- **WHEN** a client PUTs with category, subcategory, and description filled but empty `root_cause`
- **THEN** the system MUST NOT update the row and MUST return a client error

#### Scenario: Unknown subcategory rejected

- **WHEN** a client PUTs a category and subcategory that are not in `sla_fault_category`
- **THEN** the system MUST NOT update the row and MUST return a client error
