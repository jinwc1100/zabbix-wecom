## 1. Downtime fields

- [x] 1.1 Add `reason-column` (default `排除原因`) on downtime leaf/parent config; load into `DowntimeEvent.exclusionReason`
- [x] 1.2 Implement unreviewed/reviewed helpers and current-month (`Asia/Shanghai`) window on downtime start time

## 2. Notify vs preview

- [x] 2.1 On match/notify assembly, set `monthFullyReviewed` (or equivalent) per system: has current-month events AND all reviewed
- [x] 2.2 `notifyNow` / scheduled path: omit month-fully-reviewed systems from WeCom markdown and @ list; keep systems with no current-month downtime
- [x] 2.3 `preview`: keep all low-SLI items; expose skip-Wecom flag (optional short remark in detail markdown)

## 3. Verify

- [x] 3.1 Rebuild; preview a known fully-reviewed system still listed with skip flag
- [x] 3.2 Confirm notify payload excludes that system; a no-current-month low-SLI system still included
