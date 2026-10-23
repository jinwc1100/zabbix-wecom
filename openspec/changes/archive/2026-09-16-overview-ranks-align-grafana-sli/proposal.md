## Why

总览四张低 SLI 排行卡（月度父/子、年度父/子）在阈值之外还要求 Grafana 宕机为「部分排除 + 排除故障秒数 = 1」才上榜。Grafana SLA 表上年度、当月都低于 99.9% 的系统（大量仍为「未排除」）因此从 index 消失，卡片还会误显示「无低于阈值系统」。需要让排行对齐 Grafana SLI 表，明细对齐真正计入 SLA 的宕机，并按是否已复盘展示复盘描述。

## What Changes

- 四张卡仅按 Grafana SLI 数字低于 `grafana.sli-threshold`（99.9）展示，**不再**用计入 SLA 宕机裁剪排行。
- 「查看宕机明细」收录会拉低 SLI 的行：排除状态为 `未排除` 或 `-`，**或** `部分排除` 且 `排除故障秒数 = 1`。整段排除（复盘「否」）不进明细。
- 复盘描述：未复盘（排除状态 `未排除` 或排除原因为空/`-`）展示空白；已复盘展示 Grafana 排除原因原文（保存写入的故障分类拼接串）。明细页不得把空描述显示成 `-`。
- 宕机 Grafana 失败时四张 SLI 排行仍可展示；明细页报错。上榜不再保证明细至少一行。
- 不改复盘保存、排除表写入、企微通知、周报 Excel、字典与 132 DDL。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `review-overview-hub`: 排行对齐 Grafana SLI 阈值；明细改为计入 SLA 的未排除 ∪ 1 秒部分排除；复盘描述按未复盘/已复盘；宕机失败不再清空排行。

## Impact

- 后端：`ReviewOverviewService`（去掉 `restrictRanksToCountedDowntime` 对榜的裁剪；宕机失败不清空榜）、`CountedSlaDowntimeService`（WHERE 与复盘描述映射）。
- 前端：`/review/index.html` 空态语义保持阈值本身；`/review/downtime.html` 空复盘描述不显示 `-`。
- API：`GET /api/sla/review-overview` 的 `lowSli*` / `lowMonthly*` 变宽；`countedSlaDowntimes` 集合变宽；`countedDowntimeError` 不再迫使四榜为空。
- 测试：`ReviewOverviewServiceTest`、`CountedSlaDowntimeServiceTest`、页面断言。
- `GrafanaClient.queryTable` 的 `from=now-1h`：若宕机面板 SQL 使用 `$__timeFilter`，总览这次查询须把时间窗扩到报表年；验证后再改，不顺带改通知路径除非同样缺数。
