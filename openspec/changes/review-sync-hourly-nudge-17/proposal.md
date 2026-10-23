## Why

Grafana recovery times and exclusions can change during the day, but `review-event-sync` only runs at 17:00 with the WeCom incomplete-review nudge. Operators need hourly table updates without pinging the group 24 times.

## What Changes

- Scheduled review-event Grafana sync default cron becomes hourly (`0 0 * * * *`, `Asia/Shanghai`) when `review-sync.enabled` is true.
- Incomplete-review WeCom nudge from cron runs only after the **successful 17:00** (Shanghai) hourly sync. Other hours sync without WeCom.
- **BREAKING (ops):** cron no longer implies “sync then always nudge.” Nudge is 17:00-only from schedule.
- Manual `POST /api/sla/review-event-sync` (`notify` default false) unchanged. `sli-snapshot-sync` stays daily 17:00.

## Capabilities

### New Capabilities

- (none)

### Modified Capabilities

- `daily-sla-review-event-sync`: Hourly scheduled sync; cron nudge only at 17:00 after that hour’s successful sync.
- `review-incomplete-wecom-nudge`: Cron trigger is the 17:00 successful sync, not every scheduled sync.

## Impact

- `application.yml` `review-sync.cron`, `ReviewSyncProperties` default
- `ReviewEventSyncScheduler` (+ tests)
- README schedule wording
- Java 8, port 8086
