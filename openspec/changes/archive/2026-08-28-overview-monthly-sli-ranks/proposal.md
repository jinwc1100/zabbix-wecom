## Why

复盘总览目前只有年度 SLI < 99.9% 排行。当月低 SLA 更贴近近期故障，打开总览时应先看到月度最差系统，再看年度。

## What Changes

- 总览在「按责任人」与年度排行之间增加两张卡：月度父系统 / 月度子系统，SLA < 同一阈值（默认 99.9%）。
- 交互对齐年度（方案 A）：默认最差 Top 10，超过 10 条提供「更多」展开全量。
- `GET /api/sla/review-overview` 增加月度低 SLI 列表；打开页仍一次请求。
- Grafana 月度看板用独立配置，**不得**写入现有 `grafana.sli-sources`（年度推送/Excel 不变）。
- 月度查询失败时月度卡报错、列表为空；年度排行与未复盘 SQL 仍可成功返回。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `review-overview-hub`: 总览页与总览 API 增加月度父/子低 SLI 排行（Top 10 + 更多），置于年度排行之前。

## Impact

- `static/review/index.html` 布局与 `RankCard` 复用。
- `ReviewOverview` / `ReviewOverviewService` / 测试。
- `application.yml` 新增月度 SLI 源（UID、列名、面板）；`GrafanaProperties`。
- 采集逻辑可复用 `queryLowSliRows` + 列名，但源列表必须与年度分离。
- 不改 `POST /api/sla/notify`、日同步、复盘列表。
