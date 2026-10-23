## ADDED Requirements

### Requirement: Review overview page as default review entry

The system SHALL serve a Vue overview at `/review/` and `/review/index.html` titled for SLA 复盘总览. The page MUST show: (1) parent-system annual SLI below threshold, default Top 10 worst-first with a 更多 control for the full parent low-SLI list; (2) the same for subsystems; (3) the count of incomplete review **event rows** whose `system_level` is `父系统` (same system with different downtime times MUST count separately; MUST NOT distinct `system_name`); (4) the same row count for `子系统`; (5) each owner’s incomplete event row count with a navigation link to the review list. The page MUST offer a top-bar **去复盘** control that opens the review list with no query (current month, all review statuses). Threshold displayed MUST match `grafana.sli-threshold`. When Grafana data is unavailable, the two ranking cards MUST show an error and MUST NOT invent ranks; incomplete counts and owner table MUST still render when SQL succeeds.

#### Scenario: Open review folder shows overview

- **WHEN** a user opens `/review/` or `/review/index.html`
- **THEN** the browser MUST show the overview (KPI, owner table, parent and child low-SLI ranks) and MUST NOT show the per-row review save table as the primary view

#### Scenario: Top 10 then more

- **WHEN** the overview has more than 10 parent systems below threshold
- **THEN** the parent rank MUST show 10 rows until the user activates 更多, after which the full parent low-SLI list MUST be visible

#### Scenario: Hide more when ten or fewer

- **WHEN** the parent low-SLI list has 10 or fewer items
- **THEN** the parent card MUST NOT require 更多 to see every parent item

### Requirement: Review overview API

The system SHALL expose `GET /api/sla/review-overview` that returns JSON with `sliThreshold`, `incompleteParentCount`, `incompleteChildCount`, `incompleteByOwner` (array of `ownerName` and `count`), `lowSliParents`, `lowSliChildren`, and `grafanaError`. Incomplete MUST use the same empty-`count_sla` predicate as the WeCom incomplete nudge and MUST NOT apply a month or date-range filter. `incompleteParentCount` MUST be `COUNT(*)` of incomplete rows whose `system_level` is `父系统` (MUST NOT `COUNT DISTINCT system_name`). `incompleteChildCount` MUST use `子系统` the same way. `incompleteByOwner` MUST group incomplete rows by `owner_name` with `COUNT(*)` (NULL and blank names MUST form one group shown as empty or a dedicated empty label), ordered by `count` descending. `lowSliParents` and `lowSliChildren` MUST come from the same Grafana annual low-SLI collection used for notify (`collectLowSliItems`), split by `sourceLevel` `父系统` / `子系统`, each list sorted by annual SLI ascending. When Grafana login or query fails, `grafanaError` MUST be non-blank, both low-SLI arrays MUST be empty, and the incomplete fields MUST still be populated from MySQL.

#### Scenario: Same parent system two downtimes count twice

- **WHEN** one parent system has three incomplete events (different `down_start`) and another parent has one
- **THEN** `incompleteParentCount` MUST be 4

#### Scenario: Owner event counts

- **WHEN** owner `卓松林` has 7 incomplete rows and owner `王华聪` has 4
- **THEN** `incompleteByOwner` MUST include those two names with counts 7 and 4 and MUST list the larger count first

#### Scenario: Grafana down still returns SQL

- **WHEN** Grafana cannot be queried and MySQL incomplete aggregations succeed
- **THEN** the response MUST include incomplete counts and owner groups, empty low-SLI arrays, and a non-blank `grafanaError`

### Requirement: Overview links into incomplete list

Each owner row’s navigation MUST open `/review/events.html` with query `reviewStatus=incomplete`, `timeMode=all`, and `owner` equal to that owner’s name. The empty-name owner group MUST use `owner=__empty__` (MUST NOT omit `owner`). The parent incomplete KPI link MUST use `reviewStatus=incomplete`, `timeMode=all`, and `systemLevel=父系统`. The child incomplete KPI link MUST use `systemLevel=子系统` with the same review status and time mode. The top-bar **去复盘** control MUST open `/review/events.html` with no query string so the list uses its default current month and review status 全部.

#### Scenario: Owner link

- **WHEN** the user follows the owner-row 去复盘 for owner `卓松林`
- **THEN** the location MUST be `/review/events.html` with `reviewStatus=incomplete`, `timeMode=all`, and `owner` `卓松林`

#### Scenario: Parent KPI link

- **WHEN** the user follows the parent incomplete KPI list link
- **THEN** the location MUST include `reviewStatus=incomplete`, `timeMode=all`, and `systemLevel=父系统`

#### Scenario: Top-bar 去复盘 is default list

- **WHEN** the user follows the overview top-bar 去复盘
- **THEN** the location MUST be `/review/events.html` with no `reviewStatus`, `timeMode`, `owner`, or `systemLevel` query parameters
