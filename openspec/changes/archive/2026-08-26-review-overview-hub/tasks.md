## 1. List API: all-time and system level

- [x] 1.1 Extend `ReviewEventQuery` / controller to accept `timeMode=all` and `systemLevel` (`父系统`|`子系统`)
- [x] 1.2 Update `SlaReviewEventMapper.xml` list/count: skip time predicates for all; exact `system_level` when set; 400 on invalid values
- [x] 1.3 Add mapper/service tests for all-time + incomplete spanning months, parent level filter, invalid systemLevel

## 2. Overview API

- [x] 2.1 Add SQL aggregations: `COUNT(*)` incomplete rows by `system_level` (no distinct system_name); incomplete counts grouped by `owner_name`
- [x] 2.2 Add `GET /api/sla/review-overview` using those queries plus `AnnualSliService.collectLowSliItems`, split by `sourceLevel`
- [x] 2.3 On Grafana failure, return SQL fields, empty low-SLI lists, non-blank `grafanaError`
- [x] 2.4 Tests: same system two downtimes → parent/child count 2; owner sort by count desc

## 3. Static pages

- [x] 3.1 Move current `/review/index.html` content to `/review/events.html`; add 总览 link; hydrate query (`reviewStatus`, `owner`, `system`, `timeMode`, dates, `systemLevel`); add 全部时间 and 父/子系统筛选
- [x] 3.2 Add overview `/review/index.html`: KPI 条数, owner table + 去复盘（incomplete+all）, Top 10 + 更多, 顶栏去复盘（无 query）
- [x] 3.3 Keep `review-web-demo` list HTML in sync with `events.html`; add overview demo or stub as tests require
- [x] 3.4 Update `ReviewPageEditModeTest` (and similar) to compare `events.html` pairs instead of overview `index.html`

## 4. WeCom default URL

- [x] 4.1 Change shipped `review-sync.page-url` to `http://10.32.201.201:8086/review/index.html`
- [x] 4.2 Update nudge tests that assert the default markdown link

## 5. Docs and package

- [x] 5.1 README: `/review/` 总览 vs `/review/events.html` 明细
- [x] 5.2 `mvn test` in `sla-wecom-notifier`
