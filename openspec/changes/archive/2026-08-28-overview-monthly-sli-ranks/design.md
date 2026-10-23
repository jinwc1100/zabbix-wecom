## Context

总览 `GET /api/sla/review-overview` 用 `AnnualSliService.collectLowSliItems` 打 `grafana.sli-sources`（年度看板、列 `年度SLI`），再拆成 `lowSliParents` / `lowSliChildren`。该源同时服务企微年度推送。页面 `RankCard` 已是 Top 10 + 更多。

## Goals / Non-Goals

**Goals:**

- 责任人表与年度卡之间增加月度父/子两卡，方案 A 交互。
- 独立 Grafana 源；阈值仍用 `grafana.sli-threshold`。
- 月度失败不影响年度列表与 SQL KPI。

**Non-Goals:**

- 不改 notify / Excel / 日同步。
- 不在本服务传 Grafana from/to（用看板默认时间窗）。
- 月度排行不跳转复盘列表（与年度卡一致，仅展示）。

## Decisions

### 1. 独立配置 `grafana.monthly-sli-sources`

形状与 `sli-sources` 相同：`name`、`dashboard-uid`、`system-level`、`sli-column`（默认 `月度SLI`）、`name-columns`、`panels`（老版 id 4 / 新版 id 2，与年度对齐，实现时按实际看板改）。

**不**把月度 UID 写进 `sli-sources`。UID 上线前填进 yml；缺 uid 的源跳过并记日志。

### 2. 一次总览请求、两次采集、错误隔离

同一 Grafana `login` 后：先（或后）采集年度 `sli-sources`，再采集 `monthly-sli-sources`。各自 try/catch：

- `grafanaError`：仅年度失败
- `monthlyGrafanaError`：仅月度失败

登录失败则两个 error 都可以非空、四份列表皆空。SQL 未复盘字段始终先填。

JSON 增加 `lowMonthlySliParents` / `lowMonthlySliChildren`（结构同年度项：系统名 + SLI 数值）。排序：月度 SLI 升序。

### 3. 页面

复用 `RankCard`，标题区分「月度…」与「年度…」。月度 grid 在年度 grid **之上**。月度卡绑 `monthlyGrafanaError`。

### 4. 采集代码

从 `AnnualSliService` 抽出按 `List<SliSourceConfig>` + `sliColumn` 拉低 SLI 的方法，年度/月度各调一次。禁止月度走 `collectLowSliItems()`（那会读年度源）。

## Risks / Trade-offs

- [总览 Grafana 耗时加倍] → 错误隔离；后续可拆接口（本变更不做）。
- [UID/列名与看板不符] → yml 可改；列名走 `queryLowSliRows(..., sliColumn)`。
- [看板时间窗不是自然月] → 产品文案写「月度」，以 Grafana 为准。

## Migration Plan

1. 填月度 UID 后发版。
2. 回滚：去掉月度配置与页面两卡即可。

## Open Questions

无（方案 A、独立源、错误隔离已定）。UID 在实现任务中写入 yml。
