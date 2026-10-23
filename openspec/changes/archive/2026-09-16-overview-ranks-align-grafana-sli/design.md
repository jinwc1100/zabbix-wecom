## Context

总览四张 RankCard 当前先按 SLI &lt; `grafana.sli-threshold` 收集，再用 `CountedSlaDowntimeService` 的「`排除状态` = 部分排除 AND `排除故障秒数` = 1」做交集。Grafana SLA 表上低于 99.9% 但排除状态仍为「未排除」的系统（如山西省一批子系统）因此不上榜；裁完后若该月仍有数字格子，卡片显示「无低于阈值系统」。宕机 Grafana 失败还会把四榜清空。明细页把空 `reviewDesc` 显示成 `-`。

约束：宕机仍只走 Grafana HTTP（子 `cfn36wzatcpa8d`、父 `cfsre5mthdr7kc`，面板 4/2 各自 datasource uid），禁止用 133 JDBC 当宕机源。通知 / Excel / `DowntimeQueryService` 不得并入这套 WHERE。复盘保存与 `sla_excluded_downtime` 写入不动。

## Goals / Non-Goals

**Goals:**

- 四张卡与 Grafana 年/月 SLI 表对齐：能解析且低于阈值即上榜。
- 明细收录计入 SLA 的宕机：未排除（含状态 `-`）∪ 部分排除且排除故障秒数 = 1。
- 复盘描述：未复盘空白；已复盘展示排除原因原文。
- 宕机查询失败不影响 SLI 排行。

**Non-Goals:**

- 不改 `sli-sources`、notify、Excel、events 保存、排除表。
- 不从 `sla_review_event` 拼解决过程/根因到明细。
- 不为名称做模糊匹配。
- 不把 132/133 JDBC 当宕机明细源。

## Decisions

### 1. 排行与宕机解耦

`ReviewOverviewService.getOverview` 在成功收集年/月 SLI 后 **不再** 调用 `restrictRanksToCountedDowntime`。月度 `ok`/`empty`/`low` 只根据该层该月是否有可解析数字、以及低于阈值的列表是否为空（裁剪前即最终榜）。

备选：放宽宕机 WHERE 但仍用交集裁榜。拒绝：未排除把 SLI 拉低时 Grafana 表有行、交集仍可能因名称/月份对不上而丢榜；用户要求对齐 Grafana SLI 表本身。

### 2. 宕机失败不再清空四榜

`counted.collect` 抛错时：设 `countedDowntimeError`，`countedSlaDowntimes` 为空，**保留**已填的 `lowSli*` / `lowMonthly*`。登录失败仍三路皆错、榜空（无任何 Grafana 数据）。

备选：宕机失败四榜也错。拒绝：与「对齐 Grafana SLI」冲突。

### 3. 明细 WHERE（方案 B）

对每个宕机面板 rawSql 外包：

```sql
SELECT * FROM (rawSql) AS t
WHERE t.`排除状态` IN ('未排除', '-')
   OR (t.`排除状态` = '部分排除' AND t.`排除故障秒数` = 1)
```

整段排除不进列表。去重键仍是 level + name + start + end。`DowntimeQueryService.queryAllRows` 不改。

### 4. 复盘描述映射

与 `ReviewStatusUtil.isUnreviewed` 一致：状态为 `未排除`，或原因为空/`-`（含状态缺省）→ `reviewDesc` 为空串或 JSON null。否则 `reviewDesc` = 排除原因 trim 后原文（保存写入的 `故障分类:…--故障子类:…--故障描述:…`）。

`downtime.html` 使用 `reviewDesc` 原样，不得 `|| "-"`。无匹配行时仍可显示「暂无…」。

### 5. `now-1h`

实现时先确认宕机面板 SQL 是否含 `$__timeFilter`。若总览明细缺历史未排除行，仅为 **总览这次** `queryTable` 把 `from`/`to` 扩到报表年 1 月 1 日 00:00 至 now（`Asia/Shanghai`）；默认不改通知路径。

### 6. 上榜不保证有明细

名称不一致或仅有整段排除时，明细可空。前端继续从总览 JSON 客户端过滤，不再二次 Grafana。

## Risks / Trade-offs

- [榜变长] → 已有 Top 10 + 更多；可接受。
- [明细仍空但 SLI 低] → 名称或面板口径差异；不因此再裁榜。
- [`$__timeFilter` + `now-1h` 截断未排除历史] → Decision 5；用山西未排除行做验收。
- [部分排除但秒数不是 1] → 不收录；与现行「计算 SLA = 是」写入一致。

## Migration Plan

1. 发版即可，无 DDL。
2. 回滚上一 jar 即恢复「1 秒部分排除才上榜」。

## Open Questions

无。面板是否使用 `$__timeFilter` 在实现任务中验证，不阻塞规格。
