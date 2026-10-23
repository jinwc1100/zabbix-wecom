## Why

复盘明细页一打开就是表格，看不到「谁最差、还有多少没填」。需要先有一页总览：低 SLI 父/子系统、未复盘规模、按责任人待办，并能跳进明细且直接落在未复盘列表。

## What Changes

- 新增复盘总览前置页：`/review/index.html`；现有填报表迁到 `/review/events.html`。
- 总览：低 SLI 父/子 Top 10 + 更多；父/子未复盘**事件条数**（同一系统不同宕机时间各计 1 条，不去重）；按责任人未复盘条数（同样按事件行、不去重）及跳转。
- KPI / 责任人跳转明细：`reviewStatus=incomplete` + `timeMode=all`（及层级或责任人）。顶栏「去复盘」无 query，明细缺省当月 + 全部状态。
- 明细页启动时读取 URL 查询参数。
- 企微催办默认「打开复盘页」落到总览 `.../review/index.html`。
- 总览数据 API：Grafana 年度低 SLI + `sla_review_event` 未复盘按行聚合（与催办同一空 `count_sla`）。

## Capabilities

### New Capabilities

- `review-overview-hub`: 总览页、总览 API、Top10/更多、未复盘计数与责任人跳转。

### Modified Capabilities

- `sla-review-event-web`: 明细页从 URL 应用 `reviewStatus`、`owner` 等筛选；提供从总览进入明细的路径。
- `review-incomplete-wecom-nudge`: 默认 `page-url` 改为总览 `/review/index.html`。

## Impact

- 新静态页 + 可能调整 `/review/` 默认文档
- `AnnualSliService` / Grafana 查询（总览加载可能较慢）
- `SlaReviewEventMapper` 未复盘按层级、按责任人聚合
- 明细 `index`/`events` 读 query string
- 企微 `review-sync.page-url` 默认总览 URL
