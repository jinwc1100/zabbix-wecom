## 1. Config and monthly collection

- [x] 1.1 Remove `sli-column` from `grafana.monthly-sli-sources` in `application.yml`; keep UID, panels, `system-level`, `name-columns`
- [x] 1.2 Collect monthly data via `queryAllRows` on monthly sources only; parse `一月`…`十二月`; skip non-numeric (`-`, blank); filter `< sli-threshold`; do not call `queryLowSliRows` / `月度SLI`; keep notify/Excel on `sli-sources` only

## 2. Overview API

- [x] 2.1 Extend `ReviewOverview` with `monthlyReportYear`, `monthlyCurrentMonth` (`Asia/Shanghai`), per-month parent/child low lists, and per-month `low`/`ok`/`empty` status; keep `lowMonthlySliParents`/`lowMonthlySliChildren` as the current-month snapshot; preserve annual vs monthly error isolation
- [x] 2.2 Tests: dash/`-` not ranked and not treated as 0; `ok` vs `empty`; current-month snapshot; old+new panel rows both kept; monthly failure keeps annual; notify never reads monthly sources; no `月度SLI` in monthly SQL wrap

## 3. Page

- [x] 3.1 Shared month `<select>` above monthly cards: labels `{year}年{月名}`, default current month, future months disabled; bind both RankCards to selected month; `ok` empty copy vs `--` for `empty`; never show `--` as SLI%; keep Top 10 + 更多 and `monthlyGrafanaError`

## 4. Verify

- [x] 4.1 `mvn test` in `sla-wecom-notifier`
