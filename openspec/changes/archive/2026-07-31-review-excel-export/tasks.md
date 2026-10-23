## 1. Dependencies and data plumbing

- [x] 1.1 Add Excel library (e.g. Apache POI ooxml) to `pom.xml`
- [x] 1.2 Confirm parent downtime exclusion-status column; add configurable `exclusion-column` (default `排除状态`) on downtime sources
- [x] 1.3 Extend `DowntimeEvent` / `DowntimeQueryService` to load exclusion status
- [x] 1.4 Add annual inventory collection for **all** systems (not only SLI &lt; threshold), keeping `sourceLevel` and annual SLI

## 2. Review Excel service and API

- [x] 2.1 Build rows: match inventory to downtime by source level; keep only exclusion status `未排除` or `-`; fill owner via `OwnerRuleResolver`; leave interrupt reason empty
- [x] 2.2 Generate workbook with sheets `{month}-SLA小于99.9%待复盘` and `{month}-所有待复盘` (month label only; no start-time month filter)
- [x] 2.3 Expose `GET /api/sla/review-excel` returning xlsx download (`month` optional)

## 3. Docs and verify

- [x] 3.1 Document the API and sheet rules in README
- [x] 3.2 Rebuild; download once and spot-check sheet1 ⊆ low-SLI systems, sheet2 may include high-SLI, exclusion filter, empty reason, owner names
