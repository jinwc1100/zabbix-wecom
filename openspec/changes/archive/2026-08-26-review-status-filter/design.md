## Context

复盘列表 `GET /api/sla/review-events` 已支持系统/责任人模糊、按月或按区间、50/100 分页。`count_sla` 空表示未保存过（未复盘），`是`/`否` 表示已保存。催办 SQL 片段 `incompleteCountSla` 已存在。Vue 筛选栏尚无复盘状态。分页在服务端，过滤必须进 SQL。

## Goals / Non-Goals

**Goals:**

- 三档：全部 / 未复盘 / 已复盘；与其它筛选项 AND。
- 打开页与清空后缺省全部（不滤 `count_sla`）。
- 页与 Demo 行为一致。

**Non-Goals:**

- 不改催办范围（催办仍扫整表空 `count_sla`，不跟页筛选）。
- 不按长文 3/3 定义复盘状态。
- 不改保存语义。

## Decisions

### Query `reviewStatus`

取值 `all` | `incomplete` | `complete`。省略、空白或 `all`：不加 `count_sla` 条件。`incomplete`：复用 `incompleteCountSla`。`complete`：`count_sla` 为 `是` 或 `否`。其它值：客户端错误（与非法 `size` 同类）。

备选：布尔 `incompleteOnly`。否决：无法表达已复盘，且缺省全部时语义别扭。

### 默认全部而非未复盘

省略参数 = 现网行为，旧客户端无需改。页面下拉默认「全部」。

### 清空

`resetQuery` 回到当月 + 全部 + 原系统/责任人清空逻辑。

## Risks / Trade-offs

- [页上「复盘 n/3」与筛选「已复盘」不一致] → 文档/实现都以 `count_sla` 为准，与催办一致。
- [非法 reviewStatus] → 400，不静默当全部。

## Migration Plan

发版即可。旧 URL 无 `reviewStatus` 仍列当月全量。回滚去掉参数与下拉。

## Open Questions

无。
