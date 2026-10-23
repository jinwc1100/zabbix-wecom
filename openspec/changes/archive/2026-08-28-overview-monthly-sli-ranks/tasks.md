## 1. Config and collection

- [x] 1.1 Add `grafana.monthly-sli-sources` (parent + child, `sli-column` default `月度SLI`, panels aligned with annual unless the board differs); do not add these UIDs to `sli-sources`. Fill real Grafana `dashboard-uid` values
- [x] 1.2 Collect monthly low SLI from that list only (reuse `queryLowSliRows` + column); skip sources with blank uid. Notify/Excel must still use only `sli-sources`

## 2. Overview API

- [x] 2.1 Extend `ReviewOverview` with `lowMonthlySliParents`, `lowMonthlySliChildren`, `monthlyGrafanaError`; isolate annual vs monthly Grafana failures after one login
- [x] 2.2 Tests: monthly split/sort; monthly failure keeps annual; login failure sets both errors and empty lists; SQL incomplete fields still filled; notify path never reads monthly sources

## 3. Page

- [x] 3.1 On `/review/index.html`, reuse RankCard: monthly parent/child grid **above** annual; scheme A Top 10 + 更多; bind `monthlyGrafanaError`

## 4. Verify

- [x] 4.1 `mvn test` in `sla-wecom-notifier`
