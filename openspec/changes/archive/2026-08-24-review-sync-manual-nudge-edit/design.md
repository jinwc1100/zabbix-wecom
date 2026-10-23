## Context

`ReviewEventSyncScheduler.runDaily` (enabled) calls `ReviewEventSyncService.runNow()` then `ReviewIncompleteNudgeService.nudgeAfterScheduledSync()`. `POST /api/sla/review-event-sync` only calls `runNow()`. Nudge sends one markdown (count only) plus `@` via `WeComWebhookClient.sendMarkdown`. Incomplete = empty `count_sla`, whole table.

`PUT /api/sla/review-events/{id}` already overwrites the six human fields when all are non-blank. The Vue page disables save unless `_dirty`; after save `_dirty=false` so the control looks dead. Human inputs stay unbound-editable. Two copies: `src/main/resources/static/review/index.html` and `review-web-demo/index.html`.

## Goals / Non-Goals

**Goals:**

- Manual review-event-sync: same sequence as cron after a successful sync (nudge included). `review-sync.enabled` still gates cron only.
- Every nudge (cron or POST): three identical WeCom deliveries; each markdown starts with `重要的内容说3遍`.
- Saved rows (`count_sla` is `是` or `否`): human fields read-only until 编辑; then save via existing PUT; lock again. Keep both HTML copies in sync.

**Non-Goals:**

- No change to Grafana collect, INSERT IGNORE, `POST /api/sla/notify`, owner matching rules.
- No per-system list in WeCom.
- No new save HTTP verb.

## Decisions

### 1. Share one “sync then nudge” path

- **Choice:** After `runNow()` succeeds, controller and scheduler both call the same nudge method. If `runNow()` throws, skip nudge (scheduler already returns). If nudge throws, log; still return the sync JSON on the POST (DB is done).
- **Why:** Product wants manual = cron minus the clock. Isolating WeCom failure keeps backfill usable.
- **Alternative:** Duplicate nudge only in the controller — drifts.

### 2. Three sends, short gap

- **Choice:** Call `sendMarkdown` three times with the same body and mention lists. Prefix the markdown with `重要的内容说3遍` plus a newline, then the existing count line. Sleep ~400ms between calls to reduce webhook throttling. Count 0 → zero calls.
- **Why:** “连续通知3次” means three robot messages, each `@`. One message with “×3” in text would not ping three times.
- **Applies to:** scheduled and manual nudges.

### 3. Edit lock is UI-only

- **Choice:** If `count_sla` is non-blank, start locked (`_editing=false`): category/sub/desc/long text disabled; show 编辑 not 保存. 编辑 sets `_editing=true`. Save still requires six fields; on success clear dirty and lock. Never-saved rows (`count_sla` empty) stay immediately editable. Machine columns stay read-only. Page-change dirty confirm unchanged.
- **Why:** PUT already allows rewrite; locking matches “保存后无法改” without a new API.
- **Demo:** file:// mock save should toggle the same lock.

## Risks / Trade-offs

- [Three `@` per owner per run, noisy] → Requested; N=0 still silent.
- [WeCom 20/min limit] → 400ms gap; three markdown+text pairs still small.
- [POST wait ~1s+ Grafana] → Acceptable for a manual button.
- [Forget demo HTML] → Task both files.

## Migration Plan

1. Wire POST nudge; triple-send + prefix; UI lock/edit; copy demo.
2. Restart 8086. Rollback: POST skip nudge; send once; drop lock (PUT unchanged).

## Open Questions

None. Triple send, shared path, and UI-only edit were agreed.
