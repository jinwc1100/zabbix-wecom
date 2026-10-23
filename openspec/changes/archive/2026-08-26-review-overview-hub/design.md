## Context

现有 `/review/index.html` 是复盘填报明细：当月默认、模糊系统/责任人、`reviewStatus`、分页保存。企微催办默认打开该页。低 SLI 名单来自 Grafana（`AnnualSliService.collectLowSliItems`，按年度 SLI 升序、阈值 `grafana.sli-threshold` 默认 99.9）。未复盘与催办一致：整表 `count_sla` 为空（NULL 或 trim 后空），**不按月份截断**。

明细列表 **不读 URL query**，无法从总览带入「未复盘 / 责任人」。列表 API 默认当月，若总览按整表计数、跳转却用当月，会出现「总览有数、明细为空」。

## Goals / Non-Goals

**Goals:**

- 总览前置页：低 SLI 父/子 Top 10 + 更多；父/子未复盘与责任人未复盘均为**事件条数**（同一系统不同 `down_start` 各计 1，不去重系统名）；KPI / 责任人链接进入明细且筛选项与结果一致。
- 明细页支持 URL 水合；待办跳转使用整表未复盘（`timeMode=all`），与 KPI 数字对齐。
- 企微「打开复盘页」落到**总览** `/review/index.html`；顶栏「去复盘」落到明细缺省（当月 + 全部状态）。

**Non-Goals:**

- 不改 Grafana 看板、不改催办文案三行模板、不改保存校验。
- 不缓存 Grafana（首版实时拉；失败时仍返回 SQL 块）。
- 低 SLI 行不要求直接跳 Grafana；可链到明细按系统名模糊（可选，非必须）。

## Page layout（视觉稿）

总览与现有明细共用色板（`--bg / --card / --brand / --danger`），桌面单栏 max-width ~1200px，窄屏 KPI 与排行改为单列。

```
┌─────────────────────────────────────────────────────────────────┐
│  SLA 复盘总览                          [去复盘]                 │
│  年度 SLI < 99.9% · 未复盘与催办同一口径（整表空 count_sla）      │
├──────────────────────────────┬──────────────────────────────────┤
│  KPI                         │  KPI                             │
│  父系统未复盘                │  子系统未复盘                    │
│  ┌──────────┐                │  ┌──────────┐                    │
│  │    12    │ 条             │  │    28    │ 条                 │
│  └──────────┘                │  └──────────┘                    │
│  [查看未复盘明细]            │  [查看未复盘明细]                │
├──────────────────────────────┴──────────────────────────────────┤
│  按责任人未复盘（事件条数，整表）                                │
│  ┌────────────┬────────┬────────────┐                           │
│  │ 责任人     │ 条数   │ 操作       │                           │
│  ├────────────┼────────┼────────────┤                           │
│  │ 卓松林     │ 7      │ 去复盘 →   │                           │
│  │ 王华聪     │ 4      │ 去复盘 →   │                           │
│  │ （空姓名） │ 1      │ 去复盘 →   │                           │
│  └────────────┴────────┴────────────┘                           │
│  空态：全部已复盘                                                │
├──────────────────────────────┬──────────────────────────────────┤
│  父系统 · 年度 SLI < 阈值    │  子系统 · 年度 SLI < 阈值        │
│  最差 Top 10                 │  最差 Top 10                     │
│  #  系统名            SLI%   │  #  系统名            SLI%       │
│  1  xxx               98.12  │  1  yyy               97.01      │
│  … 最多 10 行                │  …                               │
│  [更多]  （展开/弹层完整）   │  [更多]                          │
│  Grafana 失败：本卡片错误提示 │                                │
└──────────────────────────────┴──────────────────────────────────┘
```

**交互：**

- 顶栏「去复盘」→ `events.html` **无 query**（缺省：当月 + 复盘状态全部，与今天打开明细一致）。
- 父/子 KPI「查看未复盘明细」→ `events.html?reviewStatus=incomplete&timeMode=all&systemLevel=父系统|子系统`。
- 责任人行「去复盘」→ `events.html?reviewStatus=incomplete&timeMode=all&owner=<姓名>`。空姓名用 `owner=__empty__`，明细只列空白责任人（不得省略 `owner`，否则会变成全部未复盘）。
- 「更多」：同卡片内展开全部低 SLI 行（滚动），或模态；默认折叠 Top 10。条数 ≤10 时隐藏「更多」。
- 加载：整页 skeleton；Grafana 慢时 KPI/责任人可先出、排行单独 loading（同一接口则整页等，见决策）。

明细页顶栏增加「← 总览」，其余表格行为不变。

## Decisions

### 1. URL：总览占 `index.html`，明细迁到 `events.html`

- `/review/` 与 `/review/index.html` = 总览。
- 现有填报页内容迁到 `/review/events.html`（及 demo 同步）。
- **企微默认** `review-sync.page-url` 为 `http://10.32.201.201:8086/review/index.html`（总览）。催办文案三行模板不变；责任人从总览再点进明细。
- 备选（弃用）：总览用 `overview.html`、明细仍占 `index`。

### 2. 计数口径

| 区块 | 口径 |
|------|------|
| 父/子系统未复盘 | `COUNT(*)` 未复盘事件行，`system_level` 分别为 `父系统`/`子系统`，`count_sla` 空（与催办同一谓词），**无时间过滤、不去重系统名**。同一系统两次宕机（不同 `down_start`）计 2 条 |
| 责任人 | 同样 `COUNT(*)` 未复盘行，按 `owner_name` 分组（NULL/空串单独一组），条数降序；同一责任人多条宕机各计 1 |

KPI 与责任人表均为事件条数，单位展示「条」不是「个系统」。

### 3. 明细 API 扩展（保证跳转能筛到）

现有列表默认当月。总览链接必须能：

- `timeMode=all`：不加 `period_month` / `down_start` 条件。
- `systemLevel`：精确等于 `父系统` 或 `子系统`；省略则不限层级。非法值 400。
- 启动时从 query 写入 `q` 并立即查询：`reviewStatus`、`owner`、`system`、`timeMode`、`month`/`from`/`to`、`systemLevel`。

责任人跳转用 **精确** owner：列表现为模糊 `LOCATE`。精确姓名作为子串仍能命中；若存在「张三」与「张三丰」会多命中。可接受；若要精确再加 `ownerExact`（非本版必须）。

### 4. 总览 API

`GET /api/sla/review-overview`（无需登录，与现网 review 一致）：

```json
{
  "sliThreshold": 99.9,
  "incompleteParentCount": 12,
  "incompleteChildCount": 28,
  "incompleteByOwner": [{ "ownerName": "卓松林", "count": 7 }],
  "lowSliParents": [{ "systemName": "...", "annualSli": 98.12, "sourceLevel": "父系统" }],
  "lowSliChildren": [ ... ],
  "grafanaError": null
}
```

服务端：SQL 聚合 + `GrafanaClient.login` + `collectLowSliItems`，按 `sourceLevel` 拆成父/子（与推送「层级以 SLI 源为准」一致）。Grafana 失败：`grafanaError` 非空，两个 lowSli 数组为空，SQL 字段仍填。

**同步一次调用**（实现简单）。若超时过长，后续可拆 `/review-overview/stats` 与 `/review-overview/low-sli`；首版不拆。

前端 Top 10：对已排序数组 `slice(0,10)`；更多用全量数组。

### 5. Demo 与测试

`review-web-demo` 增加总览假数据页或总览 `index` + 明细 `events`，与 classpath 静态资源策略对齐。`ReviewPageEditModeTest` 等「两份 index 字节相同」需改为 **events.html 成对相同**，总览另测或不强制 demo 字节相同。

## Risks / Trade-offs

- [Grafana 慢/失败] → 总览排行空 + 错误文案；KPI 仍可用。
- [书签仍打开旧 index 当明细] → `index` 变为总览；企微改总览 URL。文档/README 写明细 `events.html`。
- [当月 vs 整表] → KPI / 责任人待办一律整表未复盘；仅顶栏「去复盘」当月全部。
- [system_level 取值漂移] → 与表内存的 `父系统`/`子系统` 字面量一致；Grafana `sourceLevel` 同。

## Migration Plan

1. 发布 jar：新 `index.html`、`events.html`、overview API、列表 `timeMode=all` + `systemLevel`。
2. 改 yml 默认 `page-url`；已覆盖 yml 的环境需手工改 URL。
3. 回滚：还原静态文件与 API；企微链接改回旧 index。

## Open Questions

无阻塞项。空责任人展示「（未填写责任人）」；跳转用 `owner=__empty__`。低 SLI 行是否带「去复盘」（缺省明细、不带系统名）实现时与预览一致即可，非必须。
