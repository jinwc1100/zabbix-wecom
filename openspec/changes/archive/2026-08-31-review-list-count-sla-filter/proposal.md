## Why

复盘列表已有「复盘状态」（空 / 已填 `count_sla`），无法在已复盘里再按「是否计算SLA」筛「是」或「否」。操作需要下拉过滤该列。

## What Changes

- 列表 API 增加可选 `countSla`：`是` 或 `否` 为精确匹配；省略、空或 `all` 不限制。其它值客户端错误。与现有系统/责任人/层级/复盘状态/时间过滤器 AND。
- `/review/events.html` 与 demo 列表增加下拉：全部 / 是 / 否；默认与清空为全部。URL 已知键包含 `countSla`。
- 不改保存、排除回写、总览跳转默认（可不带该参数）。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-review-event-web`: 列表查询与 Vue 筛条支持按是否计算 SLA 过滤。

## Impact

- `ReviewEventController` / `ReviewEventQuery` / mapper 过滤、`events.html`、`events-preview.html`、相关单测。
