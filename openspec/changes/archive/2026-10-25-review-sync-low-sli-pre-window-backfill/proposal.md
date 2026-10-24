# Proposal

## Why

Low annual-SLI systems can have unreviewed downtimes that started before `weekly-review.window-start` (2026-07-30). Those rows appear on the review Excel low-SLI sheet and Grafana downtime views but never enter `sla_review_event`, so the web “待复盘” list and 17:00 WeCom nudge miss them. Operators need the same historical rows in the table without changing how “待复盘” is defined (`count_sla` empty).

## What Changes

- Add a **Pass B** step on each review-event sync (same hourly cron and manual POST as today): for systems whose annual SLI is below the configured threshold, load downtime with the **same full-panel Grafana index** as review Excel (no window-start on the query), keep rows with `down_start` **before** window-start and exclusion `未排除` or `-`, and **INSERT IGNORE** into `sla_review_event` only (no delete when SLI recovers).
- **Pass A** (existing `[window-start, now]` collect, insert, end overwrite/fold) is unchanged in scope; Pass B runs in the same job before Pass A on default sync.
- Insert eligibility for Pass B matches the Excel low-SLI sheet (not `ReviewStatusUtil` “未复盘”); KPI, list filters, and nudge still use empty `count_sla`.
- Sync result messaging SHOULD expose Pass B insert counts separately or in the summary line for ops visibility.

## Capabilities

### New Capabilities

- (none)

### Modified Capabilities

- `daily-sla-review-event-sync`: Exception to “start before window MUST NOT insert” for low-SLI Pass B; new pass using Excel-aligned data source and filters; insert-only, no rollback on SLI recovery; same hourly schedule.
- `sla-review-excel`: Clarify that review-event Pass B uses the same low-SLI system set, downtime index, and `未排除`/`-` filter as sheet `{month}-SLA小于99.9%待复盘` (plus the pre-window-start time filter for inserts only).

## Impact

- `ReviewEventSyncService` (+ tests), likely shared helper with `ReviewExcelService` for low-SLI matching / unexcluded filter
- `AnnualSliService`, `DowntimeQueryService` (no from/to overload for Pass B), `ReviewEventSyncResult` / log message
- `openspec/specs/daily-sla-review-event-sync`, `openspec/specs/sla-review-excel`
- README ops note for first manual sync after deploy; no new HTTP parameters; cron unchanged (`0 0 * * * *`)
