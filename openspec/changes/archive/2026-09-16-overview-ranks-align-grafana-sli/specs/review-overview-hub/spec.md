## MODIFIED Requirements

### Requirement: Review overview page as default review entry

The system SHALL serve a Vue overview at `/review/` and `/review/index.html` titled for SLA 复盘总览. The page MUST show, in this order after the owner table: a **shared month control** then (M) parent-system **monthly** SLI below threshold for the **selected month**, default Top 10 worst-first with a 更多 control for the full parent monthly low-SLI list; (M2) the same for monthly subsystems; then (1) parent-system **annual** SLI below threshold, default Top 10 worst-first with a 更多 control for the full parent annual low-SLI list; (2) the same for annual subsystems. Rank lists on those four cards MUST include every Grafana SLI row below `grafana.sli-threshold` for that card’s level and period and MUST NOT omit a row solely because counted-SLA downtime is missing. Each visible rank row MUST include a drill-down control as specified there. The page MUST also show: (3) the count of incomplete review **event rows** whose `system_level` is `父系统` (same system with different downtime times MUST count separately; MUST NOT distinct `system_name`); (4) the same row count for `子系统`; (5) each owner’s incomplete event row count with a navigation link to the review list. The page MUST offer a top-bar **去复盘** control that opens the review list with no query (current month, all review statuses). Threshold displayed MUST match `grafana.sli-threshold`. When annual Grafana SLI data is unavailable, the two **annual** ranking cards MUST show an error and MUST NOT invent ranks. When monthly Grafana SLI data is unavailable, the two **monthly** ranking cards MUST show an error and MUST NOT invent ranks. Counted-SLA downtime Grafana failure MUST NOT by itself hide those ranking cards. Incomplete counts and owner table MUST still render when SQL succeeds. Monthly ranking cards MUST appear above the annual ranking cards.

The month control MUST default to the current calendar month in `Asia/Shanghai`. Option labels MUST include the Grafana report year and the Chinese month name (example: `2026年八月`). Months after the current calendar month MUST be disabled. Parent and child monthly cards MUST share one selected month. Changing the month MUST update both monthly cards and monthly drill-down filters without requiring a new page load of the overview HTML. Rank rows MUST NOT display `--` (or Grafana `-`) as an SLI percentage. When the selected month has at least one numeric monthly SLI for that card’s level and none are below threshold, the card MUST show an empty state that is not `--`. When the selected month has no parseable numeric monthly SLI for that card’s level, the card MUST show an empty state using `--` (or equivalent “no monthly data”) and MUST NOT invent rank rows.

#### Scenario: Open review folder shows overview

- **WHEN** a user opens `/review/` or `/review/index.html`
- **THEN** the browser MUST show the overview (KPI, owner table, month control, monthly then annual parent and child low-SLI ranks) and MUST NOT show the per-row review save table as the primary view

#### Scenario: Top 10 then more

- **WHEN** the overview has more than 10 parent systems below threshold on the annual list
- **THEN** the annual parent rank MUST show 10 rows until the user activates 更多, after which the full parent annual low-SLI list MUST be visible

#### Scenario: Hide more when ten or fewer

- **WHEN** the parent annual low-SLI list has 10 or fewer items
- **THEN** the annual parent card MUST NOT require 更多 to see every parent item

#### Scenario: Monthly top 10 then more

- **WHEN** the selected month has more than 10 parent systems below threshold on the monthly list
- **THEN** the monthly parent rank MUST show 10 rows until the user activates 更多, after which the full parent monthly low-SLI list for that month MUST be visible

#### Scenario: Monthly hide more when ten or fewer

- **WHEN** the selected month’s parent monthly low-SLI list has 10 or fewer items
- **THEN** the monthly parent card MUST NOT require 更多 to see every monthly parent item for that month

#### Scenario: Default month is current

- **WHEN** a user opens the overview in August in `Asia/Shanghai`
- **THEN** the month control MUST select August and the monthly cards MUST show that month’s low-SLI lists (or the matching empty state)

#### Scenario: Future months disabled

- **WHEN** the current calendar month is August
- **THEN** September through December MUST be disabled in the month control

#### Scenario: All numeric monthly SLI meet threshold

- **WHEN** the selected month has numeric parent monthly SLI values and every such value is at or above threshold
- **THEN** the monthly parent card MUST show a non-`--` empty state and MUST NOT list rank rows

#### Scenario: No numeric monthly SLI

- **WHEN** the selected month has no parseable numeric parent monthly SLI (including Grafana `-`)
- **THEN** the monthly parent card MUST show `--` (or equivalent no-data empty state) and MUST NOT list rank rows whose SLI is `--`

#### Scenario: Month label includes year

- **WHEN** the monthly report year is 2026 and the option is August
- **THEN** the control MUST display `2026年八月`

#### Scenario: Low SLI without counted downtime still ranks

- **WHEN** monthly child Grafana SLI includes `山西省金服` below threshold and counted-SLA downtime has no matching child row
- **THEN** the monthly child rank MUST include `山西省金服`

### Requirement: Review overview API

The system SHALL expose `GET /api/sla/review-overview` that returns JSON with `sliThreshold`, `incompleteParentCount`, `incompleteChildCount`, `incompleteByOwner` (array of `ownerName` and `count`), `lowSliParents`, `lowSliChildren`, `lowMonthlySliParents`, `lowMonthlySliChildren`, `grafanaError`, `monthlyGrafanaError`, `monthlyReportYear`, `monthlyCurrentMonth`, per-month monthly low-SLI lists plus per-month parent/child status for months 1–12, counted-SLA downtime rows (level, name, start, end, review description), and an error field when counted-SLA downtime Grafana fails. Incomplete MUST use the same empty-`count_sla` predicate as the WeCom incomplete nudge and MUST NOT apply a month or date-range filter. `incompleteParentCount` MUST be `COUNT(*)` of incomplete rows whose `system_level` is `父系统` (MUST NOT `COUNT DISTINCT system_name`). `incompleteChildCount` MUST use `子系统` the same way. `incompleteByOwner` MUST group incomplete rows by `owner_name` with `COUNT(*)` (NULL and blank names MUST form one group shown as empty or a dedicated empty label), ordered by `count` descending.

`lowSliParents` and `lowSliChildren` MUST come from the Grafana **annual** low-SLI collection used for notify (`grafana.sli-sources` / `collectLowSliItems`), split by `sourceLevel` `父系统` / `子系统`, sorted by SLI ascending, and MUST NOT be restricted by counted-SLA downtime. Monthly lists MUST come from `grafana.monthly-sli-sources` (MUST NOT use `sli-sources`) and MUST NOT be restricted by counted-SLA downtime. Monthly collection MUST read Grafana table columns named `一月` through `十二月` (MUST NOT filter on a column named `月度SLI`). `grafana.monthly-sli-sources` MUST NOT require an `sli-column` property. A monthly row MUST be included in a month’s low-SLI list when that month’s cell parses as a number (after stripping `%`) and the number is below `grafana.sli-threshold`. Grafana `-`, blank, and non-numeric cells MUST NOT be treated as zero and MUST NOT appear as rank rows. Each monthly source’s configured panels MUST contribute separate SLI rows (same system name MAY appear twice when old and new SLI panels both match). Lists for each month MUST be split by `sourceLevel` `父系统` / `子系统` and sorted by that month’s SLI ascending. `lowMonthlySliParents` and `lowMonthlySliChildren` MUST equal the current-calendar-month lists. Per-month status for each level MUST be `low` when that low-SLI list is non-empty, `ok` when there is at least one numeric cell for that level and month and the low-SLI list is empty, and `empty` when there is no numeric cell for that level and month.

Counted-SLA downtime MUST be queried only through Grafana `/api/ds/query` using `grafana.downtime-leaf` and `grafana.downtime-parent` panel SQL and each panel’s datasource uid (old and new panels). The query MUST keep a row when `排除状态` is `未排除` or `-`, **or** when `排除状态` is `部分排除` and `排除故障秒数` equals `1`. Rows that are fully excluded MUST NOT be kept. The application JDBC datasource MUST NOT be used for this collection. Notify and Excel MUST NOT read `monthly-sli-sources` and MUST NOT use this downtime filter.

When a kept downtime row is unreviewed — `排除状态` is `未排除` or 排除原因 is blank or `-` (same predicate as incomplete Grafana review) — `reviewDesc` MUST be empty. When the row is reviewed, `reviewDesc` MUST equal the trimmed Grafana 排除原因 text. The drill-down page MUST render empty `reviewDesc` as blank and MUST NOT substitute `-`.

When annual Grafana SLI query fails, `grafanaError` MUST be non-blank and both annual low-SLI arrays MUST be empty. When monthly Grafana SLI query fails, `monthlyGrafanaError` MUST be non-blank and monthly arrays MUST be empty. When counted-SLA downtime Grafana query fails, `countedDowntimeError` MUST be non-blank and the downtime array MUST be empty, and annual and monthly low-SLI arrays MUST still be populated when their own Grafana queries succeeded. Annual SLI failure MUST NOT by itself empty the monthly arrays; monthly SLI failure MUST NOT by itself empty the annual arrays. Incomplete fields MUST still be populated from MySQL when SQL succeeds.

#### Scenario: Same parent system two downtimes count twice

- **WHEN** one parent system has three incomplete events (different `down_start`) and another parent has one
- **THEN** `incompleteParentCount` MUST be 4

#### Scenario: Owner event counts

- **WHEN** owner `卓松林` has 7 incomplete rows and owner `王华聪` has 4
- **THEN** `incompleteByOwner` MUST include those two names with counts 7 and 4 and MUST list the larger count first

#### Scenario: Grafana down still returns SQL

- **WHEN** Grafana cannot be queried at all and MySQL incomplete aggregations succeed
- **THEN** the response MUST include incomplete counts and owner groups, empty annual and monthly low-SLI arrays, and non-blank `grafanaError` and `monthlyGrafanaError`

#### Scenario: Monthly Grafana failure keeps annual ranks

- **WHEN** monthly SLI Grafana query fails and annual SLI Grafana query succeeds
- **THEN** `monthlyGrafanaError` MUST be non-blank, monthly arrays MUST be empty, and annual `lowSliParents` / `lowSliChildren` MUST still be populated from annual SLI without a counted-downtime intersection

#### Scenario: Dash cell is not ranked

- **WHEN** a parent system’s August cell is `-` and another parent’s August cell is `98.5%` with threshold `99.9`
- **THEN** the August parent low-SLI list MUST include only the `98.5%` row and MUST NOT include a rank whose SLI is `-` or `--`

#### Scenario: Current month snapshot matches August lists

- **WHEN** the server calendar month in `Asia/Shanghai` is August
- **THEN** `monthlyCurrentMonth` MUST be 8 and `lowMonthlySliParents` MUST equal the August parent low-SLI list

#### Scenario: Low SLI without counted downtime still listed

- **WHEN** annual child SLI includes `系统甲` below threshold and counted-SLA downtime has no child row named `系统甲`
- **THEN** `lowSliChildren` MUST include `系统甲`

#### Scenario: Downtime Grafana failure keeps SLI ranks

- **WHEN** counted-SLA downtime Grafana queries fail and annual and monthly SLI Grafana queries succeed
- **THEN** `countedDowntimeError` MUST be non-blank, downtime rows MUST be empty, and `lowSliParents` / `lowMonthlySliChildren` MUST still contain the below-threshold SLI rows

### Requirement: Counted-SLA downtime drill-down on overview ranks

The overview RankCards for monthly parent, monthly child, annual parent, and annual child MUST each offer a per-row control that reveals counted-SLA downtime for that row’s `systemName`. Counted-SLA downtime MUST be loaded via Grafana HTTP using each downtime panel’s own dashboard SQL and datasource uid (`grafana.downtime-leaf` and `grafana.downtime-parent`, both configured panels including old id 4 and new id 2). The query MUST keep rows where Grafana `排除状态` is `未排除` or `-`, or where `排除状态` is `部分排除` and `排除故障秒数` equals `1`. The system MUST NOT read counted-SLA downtime from the application `spring.datasource`. Matching a rank row to downtime MUST use exact equality of system name (child name-column vs child rank name; parent name-column vs parent rank name). Display columns MUST be system level (`子系统` or `父系统`), system name, downtime start, downtime end, and **复盘描述**. Duplicate rows with the same level, name, start, and end from old and new panels MUST appear once. Annual drill-down MUST list all matching kept rows for that name and level. Monthly drill-down MUST list only matching kept rows whose start timestamp falls in the currently selected calendar month of `monthlyReportYear` in `Asia/Shanghai`.

If counted-SLA downtime Grafana collection fails, the drill-down MUST show an error and MUST NOT invent detail rows; the four low-SLI ranking cards MUST still show their SLI ranks when SLI Grafana succeeded. Incomplete KPI and owner table MAY still render. A system MUST appear on an annual rank when it is below threshold on annual SLI, even with no matching downtime row. A system MUST appear on a monthly rank for the selected month when it is below threshold on that month’s SLI, even with no matching downtime row in that month. Activating drill-down MUST NOT require a further Grafana round-trip; the detail list MAY be empty.

Unreviewed kept rows MUST show a blank 复盘描述 (MUST NOT show `-` or `未排除` as the description). Reviewed kept rows MUST show the Grafana 排除原因 text as 复盘描述.

#### Scenario: Per-row control on all four cards

- **WHEN** the overview shows a monthly parent rank row and an annual child rank row
- **THEN** each of those rows MUST offer a counted-SLA downtime drill-down control

#### Scenario: Exact name match only

- **WHEN** annual child ranks include `雄安服务平台` and counted downtime includes a child row named `雄安服务平台` and another named `雄安服务平台备份`
- **THEN** drill-down for `雄安服务平台` MUST include only rows whose system name equals `雄安服务平台`

#### Scenario: Annual lists all counted rows

- **WHEN** a parent has counted-SLA downtime in March and in August
- **THEN** annual parent drill-down for that name MUST include both rows

#### Scenario: Monthly lists only selected month

- **WHEN** the shared month control is August 2026 and a child has counted-SLA downtime in March and in August
- **THEN** monthly child drill-down for that name MUST include only the August row(s)

#### Scenario: Downtime Grafana failure hides details not ranks

- **WHEN** counted-SLA downtime Grafana queries fail and monthly SLI Grafana succeeds with below-threshold child rows
- **THEN** the monthly child rank MUST still list those rows and the drill-down page MUST show an error instead of detail rows

#### Scenario: Rank row may have empty details

- **WHEN** a child appears on the annual low-SLI rank and counted-SLA downtime has no row for that name
- **THEN** its drill-down MUST show no detail rows and MUST NOT hide the rank row

#### Scenario: Reason labeled 复盘描述

- **WHEN** a drill-down row is shown
- **THEN** the column for the review text MUST be titled 复盘描述

#### Scenario: Unreviewed description is blank

- **WHEN** a kept downtime row has `排除状态` `未排除` and 排除原因 `-`
- **THEN** 复盘描述 MUST be empty

#### Scenario: Reviewed description uses 排除原因

- **WHEN** a kept downtime row has `排除状态` `部分排除`, `排除故障秒数` `1`, and 排除原因 `故障分类:网络--故障子类:专线--故障描述:中断`
- **THEN** 复盘描述 MUST equal that 排除原因 text

#### Scenario: Unexcluded downtime appears in details

- **WHEN** a child rank row named `山西省金服` has a Grafana downtime row with `排除状态` `未排除`
- **THEN** annual drill-down for that name MUST include that row
