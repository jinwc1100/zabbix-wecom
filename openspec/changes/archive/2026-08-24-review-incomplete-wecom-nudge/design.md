## Context

`ReviewEventSyncScheduler.runDaily` uses Spring cron `${review-sync.cron:0 0 17 * * *}` (`Asia/Shanghai`). When `review-sync.enabled` is true it calls `ReviewEventSyncService.runNow()`. Current yml: `enabled: false`, `cron: "0 0 17 * * *"`. Manual `POST /api/sla/review-event-sync` calls `runNow()` and never goes through the scheduler.

`zabbix.sla_review_event.count_sla` is NULL on INSERT IGNORE. Successful review-page save writes `是` or `否` from `sla_fault_category`. The table stores `owner_name`, not mobile. WeCom `@` is `WeComWebhookClient.sendMarkdown(..., names, mobiles)` then `mentioned_mobile_list`. `OwnerRuleResolver.resolve(systemName)` cannot look up by person name. Default unmatched name is `owners.default-owner` = `待指定责任人`.

## Goals / Non-Goals

**Goals:**

- Cron 17:30; ship `review-sync.enabled: true`.
- After a successful *scheduled* sync only: count all rows with empty `count_sla`, send one WeCom message with that count, `@` owners who have a mobile.
- Manual POST stays sync-only.

**Non-Goals:**

- No change to Grafana collect, INSERT IGNORE, human columns, `OwnerRuleResolver` matching, `POST /api/sla/notify`, weekly doc pipeline, review UI.
- No per-system list, no per-owner counts.
- No new HTTP for the nudge.

## Decisions

### 1. Chain inside the existing scheduler

- **Choice:** In `runDaily`, if enabled: `syncService.runNow()` then nudge. One cron, one flag.
- **Why:** Product wants “after sync, all still-empty rows”. Manual POST bypasses the scheduler, so it will not nudge.
- **Alternative:** Second `@Scheduled` at 17:31 — can run before sync finishes.

### 2. Incomplete = empty `count_sla`, whole table

- **Choice:** `count_sla IS NULL OR TRIM(count_sla) = ''`. No `period_month` filter.
- **Why:** Matches “never successfully saved”. Half-filled long text still counts until save writes `count_sla`.
- **N = 0:** do not send WeCom.

### 3. Name → mobile via `owners.rules`, do not change Resolver

- **Choice:** Distinct `owner_name`; first rule with equal `name` and non-blank `mobile`. Do not call `resolve(systemName)`.
- **Why:** Rows already have names; changing Resolver would affect annual notify. Duplicate rules for one name usually share a mobile.
- **`待指定责任人` / no mobile:** included in N, omitted from `@`.

### 4. Reuse webhook client

- **Choice:** Short markdown (count + ask to open the review page); `WeComWebhookClient.sendMarkdown(content, names, mobiles)`. Honor `wecom.enabled`.
- **Alternative:** Change the hardcoded mention sentence — not required.

### 5. Failure isolation

- **Choice:** If sync throws, skip nudge (scheduler already catches). If nudge/WeCom fails, log error; do not roll back inserts.
- **Why:** DB is source of truth; next 17:30 will nag again.

### 6. Defaults

- **Choice:** `review-sync.enabled: true`, `cron: "0 30 17 * * *"` (sec 0, min 30, hour 17). Restart required to pick up yml.

## Risks / Trade-offs

- [Daily `@` of everyone with any historical empty `count_sla` (window from 2026-07-30)] → Accepted; rows drop off after save.
- [Name missing from rules → no `@`] → Count still includes the row; do not edit rules in this change.
- [Process down at 17:30] → Ops; do not attach nudge to manual POST.
- [Mention footer still talks about an SLA list] → Acceptable; markdown leads with the count.

## Migration Plan

1. Change yml + scheduler; add count query + nudge service.
2. Restart port 8086. Disable with `review-sync.enabled: false` (sync and nudge together).
3. Rollback: restore 17:00 / enabled false; remove nudge call.

## Open Questions

None. Count rule, whole table, and manual-no-push were confirmed.
