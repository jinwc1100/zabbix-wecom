## MODIFIED Requirements

### Requirement: Fuzzy list of review events

The system SHALL list `sla_review_event` rows filtered by optional system-name and owner substring matches (case-sensitive; SQL `LOCATE`/`LIKE` with user text treated as literals). Empty system or owner MUST be ignored. Time filtering MUST NOT use a free-text substring. Time MUST be exactly one of: (1) **month** — `period_month` equals the requested calendar month as `yyyyMM` in `Asia/Shanghai`; (2) **range** — `down_start` is greater than or equal to the from-date start-of-day and less than the to-date plus one day (to-date inclusive). Month and range MUST be mutually exclusive. When the client does not send a range mode, the system MUST apply the current calendar month in `Asia/Shanghai`. A range request with missing or inverted from/to MUST fail with a client error. Results MUST be paginated: `page` starting at 1, `size` MUST be 50 or 100 (default 50); other sizes MUST fail with a client error. The response MUST be an object with `total`, `page`, `size`, and `items`. Each item MUST include machine columns, human review columns, and `id`. Sort MUST remain `down_start DESC, id DESC`.

The list MUST also accept an optional review-status filter `reviewStatus` with values `all`, `incomplete`, and `complete`. Omitted, blank, or `all` MUST NOT constrain `count_sla`. `incomplete` MUST match rows whose `count_sla` is NULL or blank after trim (same predicate as incomplete-review WeCom nudge). `complete` MUST match rows whose `count_sla` is `是` or `否`. Any other `reviewStatus` value MUST fail with a client error. Review status MUST combine with system, owner, and time filters using AND. `total` MUST count rows after all applied filters.

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

### Requirement: Vue review page layout

The system SHALL serve a Vue review page that lists events from the list API, offers fuzzy inputs for system and owner, and a review-status control with options 全部, 未复盘, and 已复盘 mapped to `reviewStatus` `all`, `incomplete`, and `complete`. The page MUST save one row at a time via the save API. Time controls MUST offer month mode (month picker) and range mode (from/to dates), mutually exclusive. On first load and after clearing filters, the page MUST query the current Shanghai month and MUST set review status to 全部 (`all`). The page MUST show total count, page size options 50 and 100 (default 50), and previous/next page controls. Changing filters or page size MUST reset to page 1. Machine fields MUST be read-only. Fault category and subcategory MUST cascade; `count_sla` MUST display from the dictionary after subcategory selection and MUST NOT be a free-text input. Fault description MUST appear on the main table. Resolution process, root cause, and improvement MUST appear only in an expand/drawer region, with a main-table completeness indicator of how many of those three are non-blank (`n/3`). The page MUST NOT include a remark field. Save MUST be blocked or rejected in the UI when any of the six required fields is blank. Unrecovered rows MUST use the same layout as recovered rows. Expand/collapse MUST sit immediately before the per-row save control. Demo HTML (`file://` and `review-web-demo`) MUST apply the same review-status filter locally.

#### Scenario: Expand for long text

- **WHEN** the user opens the review expand control on a row
- **THEN** the page MUST show inputs for 解决过程, 根本原因, and 改进措施 and MUST NOT show 备注

#### Scenario: Incomplete save disabled

- **WHEN** a row is missing any of the six required fields
- **THEN** the per-row save control MUST NOT persist that row

#### Scenario: Open page uses current month

- **WHEN** the user opens the review page with no stored filter
- **THEN** the month control MUST show the current Shanghai month and the list request MUST use month mode for that month

#### Scenario: Switch to range

- **WHEN** the user selects range mode and sets from and to dates then queries
- **THEN** the list request MUST use range mode and MUST NOT send a month period as the active time filter

#### Scenario: Open page defaults review status to all

- **WHEN** the user opens the review page with no stored filter
- **THEN** the review-status control MUST be 全部 and the list request MUST omit `reviewStatus` or send `all`

#### Scenario: Filter incomplete then query

- **WHEN** the user selects 未复盘 and queries
- **THEN** the list request MUST include `reviewStatus=incomplete`

#### Scenario: Clear restores all

- **WHEN** the user had selected 未复盘 and then clears filters
- **THEN** review status MUST return to 全部 and the list request MUST omit `reviewStatus` or send `all`
