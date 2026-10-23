# sla-review-event-web

## Purpose

复盘主表与 Vue 明细页：人填分类与长文、按月/按区间/全部时间与复盘状态、层级及是否计算 SLA 查询、50/100 分页、逐行保存。总览见 `review-overview-hub`。

## Requirements

### Requirement: Review event schema without remark

The system SHALL ensure `zabbix.sla_review_event` has nullable TEXT columns `resolution_process`, `root_cause`, and `improvement`, and SHALL NOT keep a `remark` column. New-database `CREATE TABLE` MUST match this shape. On startup against an existing table, the system MUST add missing long-text columns and MUST drop `remark` if it exists. Daily sync inserts MUST still succeed when those three columns are NULL.

#### Scenario: Existing table is altered

- **WHEN** the application starts against a table that still has `remark` and lacks the three long-text columns
- **THEN** the table MUST have `resolution_process`, `root_cause`, and `improvement` as nullable TEXT and MUST NOT have `remark`

#### Scenario: Sync insert with empty review fields

- **WHEN** daily sync inserts a new downtime row
- **THEN** the insert MUST succeed with the three long-text columns NULL

### Requirement: Fuzzy list of review events

The system SHALL list `sla_review_event` rows filtered by optional system-name and owner substring matches (case-sensitive; SQL `LOCATE`/`LIKE` with user text treated as literals). Empty system or owner MUST be ignored. Time filtering MUST NOT use a free-text substring. Time MUST be exactly one of: (1) **month** — `period_month` equals the requested calendar month as `yyyyMM` in `Asia/Shanghai`; (2) **range** — `down_start` is greater than or equal to the from-date start-of-day and less than the to-date plus one day (to-date inclusive); (3) **all** — no `period_month` and no `down_start` time window. Month, range, and all MUST be mutually exclusive. When the client does not send a time mode and does not send a range, the system MUST apply the current calendar month in `Asia/Shanghai`. A range request with missing or inverted from/to MUST fail with a client error. Results MUST be paginated: `page` starting at 1, `size` MUST be 50 or 100 (default 50); other sizes MUST fail with a client error. The response MUST be an object with `total`, `page`, `size`, and `items`. Each item MUST include machine columns, human review columns, and `id`. Sort MUST remain `down_start DESC, id DESC`.

The list MUST also accept an optional review-status filter `reviewStatus` with values `all`, `incomplete`, and `complete`. Omitted, blank, or `all` MUST NOT constrain `count_sla`. `incomplete` MUST match rows whose `count_sla` is NULL or blank after trim (same predicate as incomplete-review WeCom nudge). `complete` MUST match rows whose `count_sla` is `是` or `否`. Any other `reviewStatus` value MUST fail with a client error.

The list MUST also accept an optional `countSla` filter with values `是` and `否`. When `countSla` is `是` or `否`, every returned item’s `count_sla` MUST equal that value exactly. Omitted, blank, or `all` MUST NOT add a `count_sla` equality constraint. Any other `countSla` value MUST fail with a client error.

The list MUST accept an optional `systemLevel` that, when non-blank, MUST equal `父系统` or `子系统` and MUST constrain `system_level` by exact match. Any other non-blank `systemLevel` MUST fail with a client error. Omitted or blank `systemLevel` MUST NOT constrain level.

Review status, count-SLA filter, system substring, owner substring, system level, and the active time filter MUST combine using AND. `total` MUST count rows after all applied filters. Combining `reviewStatus=incomplete` with `countSla` `是` or `否` MUST be allowed and MUST return an empty `items` list when no row can satisfy both.

#### Scenario: Default current month

- **WHEN** a client lists with no time-mode and no range
- **THEN** every returned item’s `period_month` MUST equal the current `yyyyMM` in `Asia/Shanghai`

#### Scenario: Month filter

- **WHEN** a client lists with month mode `2026-07`
- **THEN** every returned item MUST have `period_month` `202607`

#### Scenario: Inclusive date range on down_start

- **WHEN** a client lists with range from `2026-07-01` to `2026-07-31`
- **THEN** every returned item’s `down_start` MUST fall on or after `2026-07-01 00:00:00` and before `2026-08-01 00:00:00` Shanghai

#### Scenario: Combined system owner and month

- **WHEN** a client lists with system `大同`, owner `卓`, and month `2026-07`
- **THEN** every item MUST match those substrings and that month

#### Scenario: Page size 100

- **WHEN** a client lists with `size=100` and `page=1` and more than 100 matching rows
- **THEN** `items` MUST contain at most 100 rows and `total` MUST equal the full match count

#### Scenario: Invalid size rejected

- **WHEN** a client lists with `size=20`
- **THEN** the system MUST NOT return a page of 20 and MUST return a client error

#### Scenario: Omitted reviewStatus lists all statuses

- **WHEN** a client lists with no `reviewStatus` (or `all`) for a month that has both empty and filled `count_sla`
- **THEN** `items` MAY include both incomplete and complete rows and `total` MUST count both

#### Scenario: Incomplete reviewStatus

- **WHEN** a client lists with `reviewStatus=incomplete`
- **THEN** every returned item MUST have NULL or blank `count_sla`

#### Scenario: Complete reviewStatus with other filters

- **WHEN** a client lists with `reviewStatus=complete`, system `大同`, and month `2026-07`
- **THEN** every item MUST have `count_sla` `是` or `否`, MUST match system `大同`, and MUST have `period_month` `202607`

#### Scenario: Invalid reviewStatus rejected

- **WHEN** a client lists with `reviewStatus=pending`
- **THEN** the system MUST return a client error

#### Scenario: All-time mode includes other months

- **WHEN** a client lists with time mode `all` and the table has incomplete rows in two different `period_month` values
- **THEN** `items` MAY include both months and MUST NOT be limited to the current calendar month

#### Scenario: Parent systemLevel

- **WHEN** a client lists with `systemLevel=父系统`
- **THEN** every returned item’s `systemLevel` MUST be `父系统`

#### Scenario: Invalid systemLevel rejected

- **WHEN** a client lists with `systemLevel=未知`
- **THEN** the system MUST return a client error

#### Scenario: CountSla yes

- **WHEN** a client lists with `countSla=是` for a month that has rows with `count_sla` `是`, `否`, and empty
- **THEN** every returned item MUST have `count_sla` `是` and `total` MUST NOT count `否` or empty rows

#### Scenario: CountSla no with other filters

- **WHEN** a client lists with `countSla=否`, system `大同`, and month `2026-07`
- **THEN** every item MUST have `count_sla` `否`, MUST match system `大同`, and MUST have `period_month` `202607`

#### Scenario: Omitted countSla lists both yes and no

- **WHEN** a client lists with no `countSla` (or `all`) for a month that has both `是` and `否`
- **THEN** `items` MAY include both values

#### Scenario: Invalid countSla rejected

- **WHEN** a client lists with `countSla=true`
- **THEN** the system MUST return a client error

#### Scenario: Incomplete plus countSla yes is empty

- **WHEN** a client lists with `reviewStatus=incomplete` and `countSla=是`
- **THEN** `items` MUST be empty and `total` MUST be 0

### Requirement: List page shows unrecovered sentinel as 未恢复

The Vue review list page at `/review/events.html` and the matching demo HTML MUST display a row’s downtime end as `未恢复` when `downEnd` is the unrecovered sentinel `9999-12-31 00:00:00` (including ISO strings that start with `9999-12-31`). Other parseable `downEnd` values MUST still display as `yyyy-MM-dd HH:mm:ss` (replacing `T` with a space). The list API MAY still return the sentinel datetime. The end-time cell MUST remain read-only.

#### Scenario: Sentinel displays as 未恢复

- **WHEN** a listed row’s `downEnd` is `9999-12-31T00:00:00`
- **THEN** the 宕机结束 cell MUST show `未恢复` and MUST NOT show `9999`

#### Scenario: Recovered end displays the clock time

- **WHEN** a listed row’s `downEnd` is `2026-09-13 11:23:28`
- **THEN** the 宕机结束 cell MUST show that timestamp

### Requirement: List page applies template fields to checked rows

The Vue review list at `/review/events.html` and the matching demo HTML MUST provide per-row checkboxes and a page-level control to apply a template row’s human fields onto checked rows on the **current page only**.

The template MUST be the row the user marked as template. If none is marked, the template MUST be the first row in current page order whose six required fields are all non-blank after trim (fault category, subcategory, description, resolution process, root cause, improvement). If no such row exists, apply MUST NOT change any row and MUST tell the user to fill a template first.

Apply MUST copy those six fields onto every checked row whose `id` is not the template’s `id`. Apply MUST set `count_sla` from the fault-category dictionary for the copied pair (same as changing subcategory). Apply MUST NOT copy `system_name`, times, owner, or `id`. Apply MUST NOT call the save API and MUST NOT write `sla_excluded_downtime`. After apply, those target rows MUST be editable and marked dirty. Apply MUST NOT use each prior target as the source for the next (no chain of empty rows).

If any target already has at least one of the six fields non-blank, the page MUST ask for confirmation before overwriting; cancel MUST leave all rows unchanged.

#### Scenario: Fill first then apply to the next two

- **WHEN** row 1 has all six fields filled, rows 2 and 3 are blank, the user checks rows 2 and 3, and applies with no explicit template other than the default
- **THEN** rows 2 and 3 MUST have the same six fields (and dictionary `count_sla`) as row 1, and the list API MUST NOT have been called for save

#### Scenario: Apply does not write exclusion

- **WHEN** the user applies a template to checked rows
- **THEN** the system MUST NOT insert or update `sla_excluded_downtime`

#### Scenario: No complete template

- **WHEN** no current-page row has all six fields filled and the user applies
- **THEN** no row’s fields MUST change

#### Scenario: Explicit template row is highlighted

- **WHEN** the user marks a row as template
- **THEN** that row (including its expanded review panel if open) MUST use a pale-blue full-row wash distinct from the unsaved dirty background, MUST NOT use a brand-color left accent bar, and that row’s template control MUST show as the current template with an outline style distinct from the primary solid button

#### Scenario: Default template is not pre-highlighted

- **WHEN** a current-page row is complete but the user has not marked any template
- **THEN** no row MUST use the template highlight class

#### Scenario: Switching template moves the highlight

- **WHEN** the user marks row A as template then marks row B
- **THEN** only row B MUST keep the template highlight

### Requirement: List page batch-saves checked complete rows

The same list pages MUST provide a control to save checked rows on the current page. For each checked row in page order that has all six required fields non-blank, the system MUST perform the same persist as the existing per-row save (including deriving `count_sla` and `exclude_time`, and writing `sla_excluded_downtime` via the existing save path). Checked rows that are incomplete MUST be skipped and MUST NOT be sent. The per-row six-field requirement MUST NOT be relaxed.

Saves MUST run one row after another. A failure on one row MUST NOT roll back already succeeded rows and MUST NOT skip remaining complete checked rows. After the pass the page MUST report how many succeeded, how many were skipped as incomplete, and which ids failed. Unchecked complete rows MUST NOT be saved by this control.

#### Scenario: Three checked complete rows save like three single saves

- **WHEN** three checked rows are complete and the user batch-saves
- **THEN** each MUST be persisted as if saved individually, including one exclusion write per successful save keyed by that row’s system and `down_start`

#### Scenario: Incomplete checked rows are skipped

- **WHEN** two rows are checked, only the first is complete, and the user batch-saves
- **THEN** the first MUST be saved and the second MUST NOT be sent to the save API

#### Scenario: Later row still saves after an earlier failure

- **WHEN** the first of two complete checked rows fails to save and the second would succeed
- **THEN** the second MUST still be saved and the first MUST remain unsaved

### Requirement: Per-row save with required human fields

The system SHALL update one `sla_review_event` row by `id` with fault category, subcategory, description, resolution process, root cause, and improvement. All six MUST be non-blank after trim or the update MUST fail without writing. The system MUST resolve `count_sla` from `sla_fault_category` for the category+subcategory pair and MUST reject unknown pairs. The update MUST NOT change `system_level`, `system_name`, `down_start`, `down_end`, `owner_name`, or `period_month`. On a successful update the system MUST also persist `exclude_time` using the save-derivation rules for that row’s `count_sla`, `down_start`, and `down_end`. After that update the system MUST write `sla_excluded_downtime` as specified by `review-excluded-downtime-write`. If that write fails, the save request MUST fail.

#### Scenario: Complete row saves

- **WHEN** a client PUTs a known id with all six fields filled and a valid category pair
- **THEN** the row MUST persist those fields and `count_sla` from the dictionary

#### Scenario: Missing long text rejected

- **WHEN** a client PUTs with category, subcategory, and description filled but empty `root_cause`
- **THEN** the system MUST NOT update the row and MUST return a client error

#### Scenario: Unknown subcategory rejected

- **WHEN** a client PUTs a category and subcategory that are not in `sla_fault_category`
- **THEN** the system MUST NOT update the row and MUST return a client error

#### Scenario: Exclusion write failure fails save

- **WHEN** human fields are valid but inserting `sla_excluded_downtime` fails
- **THEN** the save API MUST return an error

### Requirement: exclude_time column on sla_review_event

The system SHALL ensure `zabbix.sla_review_event` has a nullable `DATETIME` column named `exclude_time`. New-database `CREATE TABLE` MUST include this column. On startup against an existing table that lacks it, the system MUST add it. Daily sync inserts MUST still succeed when `exclude_time` is NULL.

#### Scenario: Existing table is altered

- **WHEN** the application starts against a table that has no `exclude_time` column
- **THEN** the table MUST have nullable `DATETIME` `exclude_time`

#### Scenario: Sync insert leaves exclude_time null

- **WHEN** daily sync inserts a new downtime row
- **THEN** the insert MUST succeed with `exclude_time` NULL

### Requirement: Save derives exclude_time from count_sla

When a per-row review save succeeds, the system SHALL set that row’s `exclude_time` from the persisted `count_sla` and the row’s existing `down_start` and `down_end` as follows: if `count_sla` is `是`, `exclude_time` MUST be `down_start` plus one second (MUST NOT clamp to `down_end`, even when that instant is after or equal to `down_end`); if `count_sla` is `否`, `exclude_time` MUST equal `down_end`. The save MUST NOT change `down_start` or `down_end`.

#### Scenario: Count yes uses start plus one second

- **WHEN** a client saves a row whose dictionary `count_sla` is `是` and `down_start` plus one second is before `down_end`
- **THEN** `exclude_time` MUST equal `down_start` plus one second

#### Scenario: Count yes uses start plus one second when not before down_end

- **WHEN** a client saves a row whose dictionary `count_sla` is `是` and `down_start` plus one second is after or equal to `down_end`
- **THEN** `exclude_time` MUST equal `down_start` plus one second and MUST NOT equal `down_end` unless `down_end` is already that instant

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

### Requirement: Vue review page layout

The system SHALL serve the Vue review **list** page at `/review/events.html` (and the matching demo HTML) that lists events from the list API, offers fuzzy inputs for system and owner, an optional system-level control (全部 / 父系统 / 子系统) mapped to omitted, `父系统`, and `子系统`, a review-status control with options 全部, 未复盘, and 已复盘 mapped to `reviewStatus` `all`, `incomplete`, and `complete`, and a count-SLA control with options 全部, 是, and 否 mapped to omitted/`all`, `是`, and `否`. The page MUST save one row at a time via the save API. Time controls MUST offer month mode (month picker), range mode (from/to dates), and all-time mode (`timeMode=all`), mutually exclusive. On first load **with no query string**, and after clearing filters, the page MUST query the current Shanghai month and MUST set review status to 全部 (`all`), system level to 全部, and count-SLA to 全部. The page MUST show total count, page size options 50 and 100 (default 50), and previous/next page controls. Changing filters or page size MUST reset to page 1. Machine fields MUST be read-only. Fault category and subcategory MUST cascade; `count_sla` MUST display from the dictionary after subcategory selection and MUST NOT be a free-text input. Fault description MUST appear on the main table. Resolution process, root cause, and improvement MUST appear only in an expand/drawer region, with a main-table completeness indicator of how many of those three are non-blank (`n/3`). The page MUST NOT include a remark field. Save MUST be blocked or rejected in the UI when any of the six required fields is blank. Unrecovered rows MUST use the same layout as recovered rows. Expand/collapse MUST sit immediately before the per-row save control. The list page MUST include a navigation control back to the overview (`/review/index.html`). Demo HTML MUST apply the same review-status, count-SLA, time, and level filters locally.

On load, if the URL query contains known keys, the page MUST apply them to the filter model and MUST issue the list request with those values instead of the no-query defaults. Known keys MUST include `reviewStatus`, `owner`, `system`, `timeMode`, `month`, `from`, `to`, `systemLevel`, and `countSla`.

#### Scenario: Expand for long text

- **WHEN** the user opens the review expand control on a row
- **THEN** the page MUST show inputs for 解决过程, 根本原因, and 改进措施 and MUST NOT show 备注

#### Scenario: Incomplete save disabled

- **WHEN** a row is missing any of the six required fields
- **THEN** the per-row save control MUST NOT persist that row

#### Scenario: Open page uses current month

- **WHEN** the user opens the review list page with no stored filter and no query string
- **THEN** the month control MUST show the current Shanghai month and the list request MUST use month mode for that month

#### Scenario: Switch to range

- **WHEN** the user selects range mode and sets from and to dates then queries
- **THEN** the list request MUST use range mode and MUST NOT send a month period as the active time filter

#### Scenario: Open page defaults review status to all

- **WHEN** the user opens the review list page with no stored filter and no query string
- **THEN** the review-status control MUST be 全部 and the list request MUST omit `reviewStatus` or send `all`

#### Scenario: Filter incomplete then query

- **WHEN** the user selects 未复盘 and queries
- **THEN** the list request MUST include `reviewStatus=incomplete`

#### Scenario: Clear restores all

- **WHEN** the user had selected 未复盘 and then clears filters
- **THEN** review status MUST return to 全部 and the list request MUST omit `reviewStatus` or send `all`

#### Scenario: Query string hydrates incomplete all-time owner

- **WHEN** the user opens `/review/events.html?reviewStatus=incomplete&timeMode=all&owner=卓松林`
- **THEN** the review-status control MUST be 未复盘, time mode MUST be all-time, owner MUST be `卓松林`, and the list request MUST include those filters

#### Scenario: List page links to overview

- **WHEN** the user activates 总览 on the list page
- **THEN** the browser MUST navigate to `/review/index.html`

#### Scenario: Open page defaults countSla to all

- **WHEN** the user opens the review list page with no stored filter and no query string
- **THEN** the count-SLA control MUST be 全部 and the list request MUST omit `countSla` or send `all`

#### Scenario: Filter countSla yes then query

- **WHEN** the user selects 是否计算SLA 是 and queries
- **THEN** the list request MUST include `countSla=是`

#### Scenario: Clear restores countSla all

- **WHEN** the user had selected 是否计算SLA 否 and then clears filters
- **THEN** the count-SLA control MUST return to 全部 and the list request MUST omit `countSla` or send `all`

#### Scenario: Query string hydrates countSla

- **WHEN** the user opens `/review/events.html?countSla=否&reviewStatus=complete`
- **THEN** the count-SLA control MUST be 否, review status MUST be 已复盘, and the list request MUST include those filters

### Requirement: Saved row edit then save

The Vue review list page SHALL treat a row as already saved when `count_sla` is non-blank (`是` or `否`). For an already-saved row that is not in edit mode, human fields (fault category, subcategory, description, resolution process, root cause, improvement) MUST be read-only and the page MUST offer an edit control instead of persisting from the locked state. Entering edit mode MUST enable those human fields. Saving MUST still use the per-row PUT and MUST require all six fields; after a successful save the row MUST return to read-only. Rows with empty `count_sla` MUST remain editable without requiring the edit control first. Machine fields MUST stay read-only in all modes. The served `/review/events.html` page and the demo list HTML MUST behave the same.

#### Scenario: Saved row locked until edit

- **WHEN** a listed row has `count_sla` `是` and the user has not entered edit mode
- **THEN** the human fields MUST NOT be editable and using the save control MUST NOT persist until the user enters edit mode

#### Scenario: Edit then save persists

- **WHEN** the user enters edit mode on a saved row, changes a required field, keeps all six fields filled, and saves
- **THEN** the PUT MUST persist the new values and the row MUST become read-only again

#### Scenario: Never-saved row needs no edit click

- **WHEN** a listed row has empty `count_sla`
- **THEN** the user MUST be able to fill and save without first activating the edit control
