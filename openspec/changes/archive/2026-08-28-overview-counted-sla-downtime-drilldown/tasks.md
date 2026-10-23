## 1. Grafana counted-SLA downtime collection

- [x] 1.1 Add Grafana wrap query for downtime panels: `排除状态` = `部分排除` and `排除故障秒数` = `1`; use each panel’s datasource uid via existing `/api/ds/query`; query leaf + parent, panels 4 and 2; do not use JDBC
- [x] 1.2 Map rows to level, name, start, end, reviewDesc; dedupe by level+name+start+end; do not change notify/Excel `queryAllRows` path

## 2. Overview API

- [x] 2.1 In `getOverview`, collect counted-SLA downtime after one login; on failure empty all four rank lists and set downtime error used by all four cards; keep SQL incomplete fields
- [x] 2.2 Restrict annual ranks to exact names with ≥1 all-year counted row; restrict each month’s ranks to exact names with ≥1 row in that calendar month; attach counted rows on the JSON
- [x] 2.3 Tests: downtime failure blanks annual+monthly ranks; name without counted rows omitted; exact match; monthly vs annual window; old+new dedupe; notify never uses counted filter

## 3. Page

- [x] 3.1 RankCard per-row drill-down on all four cards; columns 子系统/父系统, 系统名称, 开始, 结束, 复盘描述; annual all rows / monthly selected month; no extra fetch; downtime error hides ranks

## 4. Verify

- [x] 4.1 `mvn test` in `sla-wecom-notifier`
