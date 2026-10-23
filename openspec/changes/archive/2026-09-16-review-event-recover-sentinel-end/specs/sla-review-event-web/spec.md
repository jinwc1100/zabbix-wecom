## ADDED Requirements

### Requirement: List page shows unrecovered sentinel as 未恢复

The Vue review list page at `/review/events.html` and the matching demo HTML MUST display a row’s downtime end as `未恢复` when `downEnd` is the unrecovered sentinel `9999-12-31 00:00:00` (including ISO strings that start with `9999-12-31`). Other parseable `downEnd` values MUST still display as `yyyy-MM-dd HH:mm:ss` (replacing `T` with a space). The list API MAY still return the sentinel datetime. The end-time cell MUST remain read-only.

#### Scenario: Sentinel displays as 未恢复

- **WHEN** a listed row’s `downEnd` is `9999-12-31T00:00:00`
- **THEN** the 宕机结束 cell MUST show `未恢复` and MUST NOT show `9999`

#### Scenario: Recovered end displays the clock time

- **WHEN** a listed row’s `downEnd` is `2026-09-13 11:23:28`
- **THEN** the 宕机结束 cell MUST show that timestamp
