## 1. Counted-SLA downtime collection

- [x] 1.1 Widen `CountedSlaDowntimeService` WHERE to `排除状态` IN (`未排除`, `-`) OR (`部分排除` AND `排除故障秒数` = 1); still wrap panel rawSql; do not change `DowntimeQueryService`
- [x] 1.2 Map `reviewDesc`: unreviewed (status `未排除` or reason blank/`-`) → empty; else trimmed 排除原因
- [x] 1.3 If downtime panel SQL uses `$__timeFilter` and historical 未排除 rows are truncated, widen only the overview `queryTable` from/to to report-year start through now (`Asia/Shanghai`); leave notify path unchanged unless it is also truncated

## 2. Overview ranks

- [x] 2.1 Stop `restrictRanksToCountedDowntime` from filtering `lowSli*` / `lowMonthly*`; keep month `ok`/`empty`/`low` based on numeric cells vs below-threshold lists only
- [x] 2.2 On counted-downtime collect failure: set `countedDowntimeError`, empty downtime array, keep SLI ranks when their Grafana queries succeeded; login failure still empties all Grafana fields

## 3. Drill-down UI

- [x] 3.1 `downtime.html`: render empty `reviewDesc` as blank (no `-`); keep empty-table copy when no rows; still honor `countedDowntimeError`

## 4. Tests

- [x] 4.1 Update `CountedSlaDowntimeServiceTest` for the new WHERE and reviewDesc mapping (unreviewed blank, reviewed reason, unexcluded kept, full-exclude dropped)
- [x] 4.2 Update `ReviewOverviewServiceTest`: low SLI without downtime stays on ranks; downtime failure keeps ranks; replace `restrictsByExactNameAndSelectedMonth` rank-filter assertions
- [x] 4.3 Page/test coverage that downtime HTML does not coerce blank 复盘描述 to `-`; `mvn test`
