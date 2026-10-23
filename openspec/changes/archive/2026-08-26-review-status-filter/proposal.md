## Why

复盘页只能按系统、责任人、月份/区间筛，已填和未填混在一起，无法单独看待办或已办。催办已用 `count_sla` 区分未复盘，列表查询应对齐同一语义并支持三档切换。

## What Changes

- `GET /api/sla/review-events` 增加复盘状态过滤：全部 / 未复盘 / 已复盘；与现有系统、责任人、时间条件 **AND**。
- 省略该参数或值为全部时不按 `count_sla` 过滤（与现网列表兼容）。
- 复盘 Vue 页增加下拉；打开与清空后缺省为 **全部**。
- `file://` Demo（及如有的 `review-web-demo`）用同一规则过滤。

## Capabilities

### New Capabilities

- （无）过滤挂在现有复盘列表上，不单独成能力。

### Modified Capabilities

- `sla-review-event-web`: 列表 API 与 Vue 页增加三档复盘状态筛选；缺省全部。

## Impact

- `ReviewEventController` / `ReviewEventQuery` / `SlaReviewEventMapper` 的 `reviewEventFilters`
- `static/review/index.html`（及 demo 副本）
- 列表相关单测
- 催办查询不改
