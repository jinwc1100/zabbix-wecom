## Context

总览四张 RankCard（月度父/子、年度父/子）只展示低 SLI 名称与百分比。计入 SLA 的宕机在 Grafana 子/父宕机明细上表现为 `排除状态=部分排除` 且 `排除故障秒数=1`。面板新版/老版 datasource uid 可以不同（子看板新版 `fep7xm9lwil8gd`→132，老版另一 UID）。应用 `spring.datasource`（133）不是看板数据源。`GrafanaClient.queryTable` 已按面板 uid 调 `/api/ds/query`。`DowntimeQueryService` 已打四面板但 `queryAllRows`、无该过滤，且服务企微/Excel，不得把过滤逻辑并进 notify 路径。

用户不变量：排行出现的系统，下探必须有明细；宕机 Grafana 失败则排行也不展示。

## Goals / Non-Goals

**Goals:**

- 四卡每行下探；精确匹配系统名；新老版都查；年度全年、月度按所选月。
- Grafana HTTP only；排除原因展示为「复盘描述」。
- 总览一次加载裁剪排行，下探用响应内数据，不再二次 Grafana。

**Non-Goals:**

- 不改 `sli-sources`、notify、Excel、events.html 复盘保存。
- 不按系统名模糊匹配。
- 不把 133 JDBC 当宕机源。

## Decisions

### 1. 总览同请求采集宕机，前端点行不再打 Grafana

`ReviewOverviewService.getOverview` 在同一 Grafana login 后：年度 SLI、月度 SLI、计入 SLA 宕机（四面板）都跑。宕机失败：`countedDowntimeError`（或复用/同时设置 `grafanaError`+`monthlyGrafanaError`）非空，**四份排行数组皆空**。未复盘 SQL 仍填。

备选：点行再请求。拒绝：会违反「排行有、下探空/失败」。

### 2. 包一层 SQL，用面板自己的 datasource

对 `downtime-leaf` / `downtime-parent` 每个 panel：`resolvePanelMeta` 后

```
SELECT * FROM (rawSql) AS t
WHERE t.`排除状态` = '部分排除' AND t.`排除故障秒数` = 1
```

`queryTable` 已带 `datasource.uid`。禁止改写 yml 去「统一数据源」。

映射：层级=source system-level；名称=name-column；开始=start-column；结束=end-column（子「恢复时间」、父「宕机结束时间」）；复盘描述=reason-column 原文。

去重键：`systemLevel + systemName + start + end`。

### 3. 裁剪排行

- 年度父/子：SLI 低列表 ∩ 名称精确出现在**全年**计入 SLA 宕机（同层级）至少一次。
- 月度某月父/子：该月 SLI 低列表 ∩ 该月（`Asia/Shanghai`，`down_start` 的年=monthlyReportYear 且月=所选月）至少一条同名宕机。

无交集则该卡无排行行（月度仍可用 `ok`/`empty`/`low`：裁完后无行且该月有数字达标 → `ok`；无数字 → `empty`；有裁后行 → `low`）。

### 4. JSON

增加计入 SLA 宕机数组（含 `systemLevel`、`systemName`、`downStart`、`downEnd`、`reviewDesc`）。前端按行过滤：年度用全表精确名；月度再滤月。不单独开 GET，避免第二趟 Grafana。

### 5. 页面

RankCard 每行「计入SLA宕机」展开或抽屉：列 子系统/父系统、系统名称、开始、结束、复盘描述。宕机失败时四张卡走与 SLI 失败相同的 error 展示（不发明排行）。

## Risks / Trade-offs

- [总览多 4 次重 CTE] → 无法在第 4 条不变量下省略；超时沿用 `read-timeout-ms`。
- [SLI 低但无 1 秒部分排除] → 该系统从排行消失（有意）。
- [新老版同事件] → 去重避免双行。

## Migration Plan

1. 发版即可，无 DB。
2. 回滚上一 jar，排行恢复为未按宕机裁剪。

## Open Questions

无。
