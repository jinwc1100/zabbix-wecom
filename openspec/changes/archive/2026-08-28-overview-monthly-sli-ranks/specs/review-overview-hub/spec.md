## MODIFIED Requirements

### Requirement: Review overview page as default review entry

The system SHALL serve a Vue overview at `/review/` and `/review/index.html` titled for SLA 复盘总览. The page MUST show, in this order after the owner table: (M) parent-system **monthly** SLI below threshold, default Top 10 worst-first with a 更多 control for the full parent monthly low-SLI list; (M2) the same for monthly subsystems; then (1) parent-system **annual** SLI below threshold, default Top 10 worst-first with a 更多 control for the full parent annual low-SLI list; (2) the same for annual subsystems. The page MUST also show: (3) the count of incomplete review **event rows** whose `system_level` is `父系统` (same system with different downtime times MUST count separately; MUST NOT distinct `system_name`); (4) the same row count for `子系统`; (5) each owner’s incomplete event row count with a navigation link to the review list. The page MUST offer a top-bar **去复盘** control that opens the review list with no query (current month, all review statuses). Threshold displayed MUST match `grafana.sli-threshold`. When annual Grafana data is unavailable, the two **annual** ranking cards MUST show an error and MUST NOT invent ranks. When monthly Grafana data is unavailable, the two **monthly** ranking cards MUST show an error and MUST NOT invent ranks. Incomplete counts and owner table MUST still render when SQL succeeds. Monthly ranking cards MUST appear above the annual ranking cards.

#### Scenario: Open review folder shows overview

- **WHEN** a user opens `/review/` or `/review/index.html`
- **THEN** the browser MUST show the overview (KPI, owner table, monthly then annual parent and child low-SLI ranks) and MUST NOT show the per-row review save table as the primary view

#### Scenario: Top 10 then more

- **WHEN** the overview has more than 10 parent systems below threshold on the annual list
- **THEN** the annual parent rank MUST show 10 rows until the user activates 更多, after which the full parent annual low-SLI list MUST be visible

#### Scenario: Hide more when ten or fewer

- **WHEN** the parent annual low-SLI list has 10 or fewer items
- **THEN** the annual parent card MUST NOT require 更多 to see every parent item

#### Scenario: Monthly top 10 then more

- **WHEN** the overview has more than 10 parent systems below threshold on the monthly list
- **THEN** the monthly parent rank MUST show 10 rows until the user activates 更多, after which the full parent monthly low-SLI list MUST be visible

#### Scenario: Monthly hide more when ten or fewer

- **WHEN** the parent monthly low-SLI list has 10 or fewer items
- **THEN** the monthly parent card MUST NOT require 更多 to see every monthly parent item

### Requirement: Review overview API

The system SHALL expose `GET /api/sla/review-overview` that returns JSON with `sliThreshold`, `incompleteParentCount`, `incompleteChildCount`, `incompleteByOwner` (array of `ownerName` and `count`), `lowSliParents`, `lowSliChildren`, `lowMonthlySliParents`, `lowMonthlySliChildren`, `grafanaError`, and `monthlyGrafanaError`. Incomplete MUST use the same empty-`count_sla` predicate as the WeCom incomplete nudge and MUST NOT apply a month or date-range filter. `incompleteParentCount` MUST be `COUNT(*)` of incomplete rows whose `system_level` is `父系统` (MUST NOT `COUNT DISTINCT system_name`). `incompleteChildCount` MUST use `子系统` the same way. `incompleteByOwner` MUST group incomplete rows by `owner_name` with `COUNT(*)` (NULL and blank names MUST form one group shown as empty or a dedicated empty label), ordered by `count` descending. `lowSliParents` and `lowSliChildren` MUST come from the Grafana **annual** low-SLI collection used for notify (`grafana.sli-sources` / `collectLowSliItems`), split by `sourceLevel` `父系统` / `子系统`, each list sorted by SLI ascending. `lowMonthlySliParents` and `lowMonthlySliChildren` MUST come from `grafana.monthly-sli-sources` (MUST NOT use `sli-sources`), split the same way, each sorted by monthly SLI ascending. When annual Grafana query fails, `grafanaError` MUST be non-blank and both annual low-SLI arrays MUST be empty. When monthly Grafana query fails, `monthlyGrafanaError` MUST be non-blank and both monthly low-SLI arrays MUST be empty. Annual failure MUST NOT by itself empty the monthly arrays, and monthly failure MUST NOT by itself empty the annual arrays. Incomplete fields MUST still be populated from MySQL when SQL succeeds. Notify and Excel MUST NOT read `monthly-sli-sources`.

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
- **THEN** `monthlyGrafanaError` MUST be non-blank, monthly arrays MUST be empty, and annual `lowSliParents` / `lowSliChildren` MUST still be populated
