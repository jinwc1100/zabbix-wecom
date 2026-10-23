## MODIFIED Requirements

### Requirement: Fuzzy list of review events

The system SHALL list `sla_review_event` rows filtered by optional system-name and owner substring matches (case-sensitive; SQL `LOCATE`/`LIKE` with user text treated as literals). Empty system or owner MUST be ignored. Time filtering MUST NOT use a free-text substring. Time MUST be exactly one of: (1) **month** — `period_month` equals the requested calendar month as `yyyyMM` in `Asia/Shanghai`; (2) **range** — `down_start` is greater than or equal to the from-date start-of-day and less than the to-date plus one day (to-date inclusive); (3) **all** — no `period_month` and no `down_start` time window. Month, range, and all MUST be mutually exclusive. When the client does not send a time mode and does not send a range, the system MUST apply the current calendar month in `Asia/Shanghai`. A range request with missing or inverted from/to MUST fail with a client error. Results MUST be paginated: `page` starting at 1, `size` MUST be 50 or 100 (default 50); other sizes MUST fail with a client error. The response MUST be an object with `total`, `page`, `size`, and `items`. Each item MUST include machine columns, human review columns, and `id`. Sort MUST remain `down_start DESC, id DESC`.

The list MUST also accept an optional review-status filter `reviewStatus` with values `all`, `incomplete`, and `complete`. Omitted, blank, or `all` MUST NOT constrain `count_sla`. `incomplete` MUST match rows whose `count_sla` is NULL or blank after trim (same predicate as incomplete-review WeCom nudge). `complete` MUST match rows whose `count_sla` is `是` or `否`. Any other `reviewStatus` value MUST fail with a client error.

The list MUST accept an optional `systemLevel` that, when non-blank, MUST equal `父系统` or `子系统` and MUST constrain `system_level` by exact match. Any other non-blank `systemLevel` MUST fail with a client error. Omitted or blank `systemLevel` MUST NOT constrain level.

Review status, system substring, owner substring, system level, and the active time filter MUST combine using AND. `total` MUST count rows after all applied filters.

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

### Requirement: Vue review page layout

The system SHALL serve the Vue review **list** page at `/review/events.html` (and the matching demo HTML) that lists events from the list API, offers fuzzy inputs for system and owner, an optional system-level control (全部 / 父系统 / 子系统) mapped to omitted, `父系统`, and `子系统`, and a review-status control with options 全部, 未复盘, and 已复盘 mapped to `reviewStatus` `all`, `incomplete`, and `complete`. The page MUST save one row at a time via the save API. Time controls MUST offer month mode (month picker), range mode (from/to dates), and all-time mode (`timeMode=all`), mutually exclusive. On first load **with no query string**, and after clearing filters, the page MUST query the current Shanghai month and MUST set review status to 全部 (`all`) and system level to 全部. The page MUST show total count, page size options 50 and 100 (default 50), and previous/next page controls. Changing filters or page size MUST reset to page 1. Machine fields MUST be read-only. Fault category and subcategory MUST cascade; `count_sla` MUST display from the dictionary after subcategory selection and MUST NOT be a free-text input. Fault description MUST appear on the main table. Resolution process, root cause, and improvement MUST appear only in an expand/drawer region, with a main-table completeness indicator of how many of those three are non-blank (`n/3`). The page MUST NOT include a remark field. Save MUST be blocked or rejected in the UI when any of the six required fields is blank. Unrecovered rows MUST use the same layout as recovered rows. Expand/collapse MUST sit immediately before the per-row save control. The list page MUST include a navigation control back to the overview (`/review/index.html`). Demo HTML MUST apply the same review-status, time, and level filters locally.

On load, if the URL query contains known keys, the page MUST apply them to the filter model and MUST issue the list request with those values instead of the no-query defaults. Known keys MUST include `reviewStatus`, `owner`, `system`, `timeMode`, `month`, `from`, `to`, and `systemLevel`.

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
