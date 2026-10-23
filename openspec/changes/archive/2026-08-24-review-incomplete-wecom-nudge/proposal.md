## Why

Daily sync is off and fires at 17:00, so rows whose `count_sla` is still empty never get a WeCom ping. Move the job to 17:30, turn it on, and after a scheduled sync `@` owners for the whole-table incomplete count—without pinging the group on manual backfill.

## What Changes

- Daily sync cron: 17:00 → **17:30** (`Asia/Shanghai`).
- `review-sync.enabled` default → **true**. When false, skip both scheduled sync and the nudge.
- Scheduled path only: after successful sync, query all `sla_review_event` rows with empty `count_sla` (NULL or blank). WeCom sends **only the total count** and `@` owners of those rows who have a resolvable mobile. No system list.
- `POST /api/sla/review-event-sync` still syncs and **must not** send WeCom.
- No change to `POST /api/sla/notify`, weekly review doc, owner matching rules, or review-page save.

## Capabilities

### New Capabilities

- `review-incomplete-wecom-nudge`: empty `count_sla` = not reviewed; whole-table count; short WeCom + `@`; only after **scheduled** daily sync.

### Modified Capabilities

- `daily-sla-review-event-sync`: job at 17:30; default enabled; after successful scheduled sync MUST run the nudge; manual POST MUST NOT.

## Impact

- `application.yml` `review-sync.cron` / `enabled`.
- `ReviewEventSyncScheduler`: when enabled, sync then nudge.
- New query/service: count empty `count_sla`, map `owner_name` → mobile from `owners.rules`; reuse `WeComWebhookClient`.
- Grafana collect, INSERT IGNORE, list/save APIs unchanged.
