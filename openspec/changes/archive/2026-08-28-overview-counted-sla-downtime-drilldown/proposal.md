## Why

总览低 SLI Top 10 只能看到系统名和百分比，无法看到把 SLA 打下去的宕机。复盘把「计入 SLA」落成 Grafana 上排除状态「部分排除」且排除故障秒数 1 秒；打开总览时应能从排行行下探这些明细，且排行与下探必须同成败、同行必有明细。

## What Changes

- 月度父/子、年度父/子四张排行卡每一行增加「计入 SLA 宕机」入口。
- 明细走 Grafana HTTP（看板 rawSql + 面板自身 datasource uid → `/api/ds/query`），查询子/父宕机明细新版+老版；**不得**用应用 `spring.datasource` 直查 Zabbix。
- 过滤：`排除状态` = `部分排除` 且 `排除故障秒数` = `1`。系统名与排行 **精确相等**。
- 列：子系统/父系统、系统名称、宕机开始时间、宕机结束时间；看板「排除原因」展示为 **复盘描述**。
- 年度下探：该系统全部命中行。月度下探：开始时间落在所选自然月（看板年 + 月份）。
- 新版+老版命中且层级+名称+开始+结束相同则去重为一行。
- **BREAKING（总览排行语义）**：计入 SLA 宕机 Grafana 失败时，四张低 SLI 排行与下探均不可用（报错、不发明排行）。排行只保留「精确同名且至少一条命中宕机」的系统；月度排行再按所选月至少一条。保证「总览有该行 ⇒ 下探必有明细」。
- 未复盘 KPI / 责任人表 / 企微 / Excel 不变。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `review-overview-hub`: 四张低 SLI 卡行内下探计入 SLA 宕机；排行与宕机 Grafana 同成败；名称精确匹配；年度全年 / 月度当选月。

## Impact

- `GET /api/sla/review-overview` 增加计入 SLA 宕机列表（或按系统索引），并在裁剪后的排行上使用。
- `GrafanaClient` / 宕机采集：四面板包一层 WHERE，用面板 datasource。
- `/review/index.html` RankCard 行内入口与明细展示。
- 总览打开多四次 Grafana 查询，耗时上升。
