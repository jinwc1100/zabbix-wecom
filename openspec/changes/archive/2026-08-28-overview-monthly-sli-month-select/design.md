## Context

`overview-monthly-sli-ranks` 已上线：总览在年度卡之上展示月度父/子低 SLA，源为 `grafana.monthly-sli-sources`，错误走 `monthlyGrafanaError`。实现误把 `sli-column` 配成 `月度SLI`，并对 Grafana 原始 SQL 外包 `WHERE \`月度SLI\` < 阈值`。看板 UID 与年度相同（子系统 `afj5c5o5v3rb4f`、父系统 `aftk5ryifcfeof`），最终 SELECT 只有 `年度SLI` 和 `一月`…`十二月`（未来月或无数据为 `-`）。年度 `queryLowSliRows` + `年度SLI` 仍正确；notify/Excel 仍只读 `sli-sources`。

## Goals / Non-Goals

**Goals:**

- 月度排行按约定月份列采集，打开默认当月，父/子共用带年文案的下拉。
- 未来月禁用；仅数字且低于阈值进入排行；达标空态与无数据 `--` 空态分开。
- 老/新版面板与年度一样并列、不去重。
- 去掉月度配置里的 `sli-column`。

**Non-Goals:**

- 不改 notify、Excel、日同步、年度卡。
- 不另做年份选择（看板为 `YEAR(CURDATE())`）。
- 不把月度 UID 并入 `sli-sources`。
- 不按系统名合并老/新版行。

## Decisions

### 1. 月度不再 `queryLowSliRows` + 单列名

对 `monthly-sli-sources` 各面板使用 `queryAllRows`（看板原始 SQL，一次出全年 12 列）。Java 用固定别名 `一月`…`十二月` 解析；忽略配置中的 `sliColumn`（yml 删除该键）。

备选：yml 写死 `八月` 或每次 `WHERE \`八月\``。拒绝：未来月 `'-' < 99.9` 会被当成 0；切月会重复跑重 CTE。

### 2. 一次总览响应带齐 12 月，前端切月

`GET /api/sla/review-overview` 仍一次请求。增加：

- `monthlyReportYear`：与看板年一致，服务端用 `Asia/Shanghai` 的当年（与 `review-sync.zone` 相同）。
- `monthlyCurrentMonth`：1–12，默认选中。
- 每月一份父/子低 SLA 列表（仅数字且 `< grafana.sli-threshold`，升序）。
- 每月父/子状态：`low`（有排行行）、`ok`（至少一条可解析数字且无一低于阈值）、`empty`（该层该月无一可解析数字）。

前端共用一个 `<select>`：选项文案 `{year}年{一月…十二月}`；`month > monthlyCurrentMonth` 禁用。切换只改绑定的列表与空态，不再请求。

保留 `lowMonthlySliParents` / `lowMonthlySliChildren` 作为**当月**快照，便于旧页面/测试；完整按月数据放在显式结构（如 `lowMonthlyByMonth`）以免切月丢数。

### 3. 解析与空态

- 去掉 `%` 后能 `parseDouble` 的才算数字；`-`、空、`--`、非数字 → 不进排行。
- 卡在 `monthlyGrafanaError` 非空时仍只显示错误。
- `low`：Top 10 + 更多（现有 RankCard）。
- `ok`：空态文案表明本月无低于阈值系统（不用 `--` 当 SLI%）。
- `empty`：卡内 `--`（或等价「无月度数据」），表体无排名行。

### 4. 面板规则

每个 `monthly-sli-sources` 的 panels（默认 id 4 / 2）各出一行；`splitByLevel` 与年度相同。同一 `systemName` 可出现两次。

### 5. 配置

`monthly-sli-sources` 保留 `name`、`dashboard-uid`、`system-level`、`name-columns`、`panels`。删除月度 `sli-column`。UID 仍独立于 `sli-sources`。

## Risks / Trade-offs

- [总览包体变大] → 仅低于阈值的行按月存放，不是全量系统×12。
- [MySQL `CURDATE()` 年与上海年不一致] → 文案用应用时区年；若看板年漂移，列仍是该 SQL 的 1–12 月。
- [列名与看板不一致] → 约定写死中文月名；缺列当该月 `empty` 并打日志，不回退 `月度SLI`。

## Migration Plan

1. 发版含 yml 与采集/页面；无需 DB。
2. 回滚上一版 jar；月度卡会再次因 `月度SLI` 报错。

## Open Questions

无。
