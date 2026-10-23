## Context

Collect currently requires exclusion `未排除` or `-`. Grafana 部分排除/全部排除 rows never enter the 38-row working set, so overwrite of `down_end` and `period_to` never runs. Insert/overwrite/exclusion UPDATE already exist; this change only splits **who may insert** vs **who may align**.

Constraints: Java 8, port 8086, no new exclusion INSERT on sync, no parent end-column rename.

## Goals / Non-Goals

**Goals:**

- In-window Grafana rows with any exclusion status participate in start-identity overwrite when a review row already exists.
- Insert still only `未排除` / `-`.
- `否` + existing exclusion still UPDATE `period_to` after that overwrite.

**Non-Goals:**

- Do not INSERT excluded downtimes into `sla_review_event`.
- Do not INSERT `sla_excluded_downtime` when none exists.
- Do not change Grafana query window or subsystem `恢复时间` column.
- Do not change counted-SLA overview filters.

## Decisions

1. **Working set vs insert filter**  
   `isEligible` (or equivalent): parseable start in `[window-start, trigger]`. Drop the unexcluded check from collect.  
   `ReviewEventSyncService`: if exclusion is not `未排除`/`-` and `listByStartIdentity` is empty → skip (no insert). If a start exists → same `alignEnd` as today. Sentinel insert still only for unexcluded.

2. **Keep align + period_to as-is**  
   Reuse `updateAlignedDownEnd` and `updatePeriodToAfterSync`. No second write path.

3. **refreshOwner**  
   Uses the broader collect set; still no insert. Owner refresh on excluded-but-existing starts is allowed.

**Alternative considered:** collect only excluded rows that already exist (query DB first). Rejected: extra round-trip; skip-if-empty on align is enough.

## Risks / Trade-offs

- [Working set larger] → Grafana already returns hundreds of rows; filter is in-process.  
- [period_from ≠ down_start] → existing skip/no-insert; log count 0.  
- [Excluded unrecovered sentinel] → no insert; cannot create new excluded 未恢复 rows (intentional).

## Migration Plan

Deploy JAR; next POST/cron applies. Rollback previous JAR.

## Open Questions

- None.
