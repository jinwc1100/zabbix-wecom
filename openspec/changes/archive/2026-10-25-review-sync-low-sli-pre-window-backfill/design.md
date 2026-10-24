# Design

## Context

See `proposal.md` — Why. Today `ReviewEventSyncService` uses `WeeklyReviewDataService` for a single Pass A collect bounded by `weekly-review.window-start`. `ReviewExcelService` already loads full-panel leaf/parent indexes via `DowntimeQueryService.loadLeafIndex(session)` / `loadParentIndex(session)` (no from/to), filters `未排除`/`-` via `isUnexcluded`, and scopes systems by annual SLI. Those pre-window rows never reach `sla_review_event` under the current spec scenario “Start before window is omitted.”

## Goals / Non-Goals

**Goals:**

- Implement Pass B before Pass A on the existing default sync path (hourly cron + manual POST default).
- Reuse Excel-aligned matching and exclusion rules; insert-only with existing mapper/insert + end-align logic unchanged for Pass A.
- Expose Pass B insert stats on `ReviewEventSyncResult`.

**Non-Goals:**

- Changing “待复盘” definition (`count_sla` empty), nudge copy, or 17:00-only nudge trigger.
- Deleting or hiding rows when SLI recovers.
- End overwrite/fold for pre-window rows from Pass B collect (Pass A still owns in-window end alignment; pre-window rows already in DB get end updates only when Pass A later sees them in-window — they won’t, by design; ops accept stale `down_end` until manual edit or future enhancement).
- New HTTP flags; `refreshOwner` / `refreshExcludeTime` / routing-only paths unchanged.

## Decisions

1. **Pass order: B then A**  
   Run Pass B first so new historical rows exist before Pass A processes the window. Overlap at exactly window-start is owned by Pass A (`[window-start, now]` inclusive).  
   *Alternative:* A then B — rejected; no benefit and harder to reason about duplicate skip counts.

2. **Shared matching logic**  
   Extract a small helper (e.g. `ReviewLowSliDowntimeCollector` or static util used by `ReviewExcelService` and sync) that, given session + low-SLI item list + leaf/parent indexes, yields `WeeklyReviewRow` or internal DTOs with level, name, start, end, exclusion, owner. Reuse `ReviewExcelService.isUnexcluded` (move to util if needed to avoid Excel → sync dependency cycle).  
   *Alternative:* Duplicate `expandRows` loop in sync — rejected; drift risk vs Excel.

3. **Low-SLI list source**  
   Use `AnnualSliService.collectAllInventoryItems` + threshold from `GrafanaProperties.getSliThreshold()` (same as Excel), not `collectLowSliItems` alone, if the latter omits fields needed for matching — verify in implementation; behavior must match Excel sheet 1 system set.

4. **Grafana session**  
   One login per sync run: Pass B loads full indexes + annual SLI; Pass A continues using `WeeklyReviewDataService` (separate queries with window bounds). Accept duplicate Grafana traffic vs refactoring WeeklyReviewDataService in this change.  
   *Alternative:* Single combined query — larger refactor, out of scope.

5. **Insert path**  
   Map Pass B candidates through existing `ReviewEventSyncMapper` + `insertIgnore` / skip-existing counters; do **not** call end-overwrite/fold pipeline for Pass B rows. Increment new result fields e.g. `preWindowCollected`, `preWindowInserted`, `preWindowSkippedExisting`.

6. **Pass B scope guard**  
   Only invoke when `applyGrafanaPass(triggerTime, false, result)` runs (same branch as today’s default insert sync). Skip when `refreshOwner`, when `refreshExcludeTime`-only short-circuit, or routing-only return.

## Risks / Trade-offs

- **[Risk] Hourly full-panel downtime query adds Grafana load** → Same cost class as manual Excel export; monitor logs; no extra cron.
- **[Risk] Pre-window rows never get Pass A end updates** → Acceptable per product decision; end times come from Grafana at insert time; unrecovered sentinel rules still apply on insert.
- **[Risk] Excel and sync drift if helpers diverge** → Single shared collector + tests comparing candidate keys to `ReviewExcelService` fixtures.
- **[Risk] Large first-run insert burst** → Document one-time `POST /api/sla/review-event-sync` after deploy; INSERT IGNORE idempotent on reruns.

## Migration Plan

1. Deploy with `review-sync.enabled` true; optional manual POST once to backfill.
2. No DB migration; existing rows untouched.
3. Rollback: revert code; inserted rows remain (by spec); acceptable.

## Open Questions

(none — user decisions locked for exclusion filter, query range, insert-only, and cron coupling)
