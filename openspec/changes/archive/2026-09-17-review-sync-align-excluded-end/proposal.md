## Why

`review-event-sync` already overwrites `down_end` for the same start identity, but collect still drops Grafana rows whose exclusion status is not `未排除` or `-`. After operators mark 部分排除/全部排除, recovery time in Grafana no longer reaches `sla_review_event` (e.g. 父系统开标 `2026-09-13 13:02:18` still 17:00:08 while Grafana is 22:27:49). Those rows are already in the review table and often `count_sla=否`; they still need Grafana’s end and existing `sla_excluded_downtime.period_to`.

## What Changes

- Collect Grafana downtime for review-event sync **without** requiring `未排除`/`-` when the row will only **align** an existing start identity.
- **INSERT** remains only for `未排除` or `-`. Statuses such as `部分排除` / `全部排除` MUST NOT create new `sla_review_event` rows.
- For an existing start identity, parseable Grafana end still overwrites `down_end` (fold duplicates as today). `count_sla=否` still sets `exclude_time` to the new end and **UPDATE**s existing exclusion `period_to` only (no exclusion INSERT).
- Grafana query window stays window-start → trigger. Parent `end-column` name is unchanged.

## Capabilities

### New Capabilities

- (none)

### Modified Capabilities

- `daily-sla-review-event-sync`: Collect may include excluded Grafana rows for overwrite/fold of existing start identities; insert filter stays `未排除`/`-`.
- `review-excluded-downtime-write`: Confirm sync `period_to` UPDATE still runs after that overwrite when `count_sla=否` and an exclusion row exists (including Grafana 部分排除).

## Impact

- `WeeklyReviewDataService` eligibility vs insert vs align split
- `ReviewEventSyncService` Grafana pass: insert only unexcluded; align any collected parseable end with a matching start
- Exclusion write path unchanged in contract (`updatePeriodToAfterSync`)
- Tests for 部分排除 overwrite + no insert; `否` period_to; `mvn test`
- Java 8, port 8086
