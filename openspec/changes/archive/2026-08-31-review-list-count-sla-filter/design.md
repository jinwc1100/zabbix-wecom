## Context

复盘列表已有「复盘状态」：`incomplete` 匹配空 `count_sla`，`complete` 匹配「是」或「否」。操作需要在已复盘（或全部）里再按「是否计算SLA」筛「是」或「否」。筛条在 `events.html` / `events-preview.html`；列表走 `GET /api/sla/review-events` 与 MyBatis `reviewEventFilters`。

## Goals / Non-Goals

**Goals:**

- 列表 API 与页面下拉按 `count_sla` 精确匹配「是」或「否」，与现有过滤器 AND。
- Demo 列表本地过滤行为与线上一致。

**Non-Goals:**

- 不改保存、排除回写、总览链接默认参数、字典或 `sla_review_event` 表结构。
- 不下拉第四项「未填写」；空值继续只用「未复盘」。

## Decisions

### 1. 参数名与取值

查询参数 `countSla`：`是` / `否` 精确匹配列 `count_sla`。省略、空或 `all` 不限制。其它值 HTTP 400。不用 `true`/`false`，与库内中文一致。

### 2. 与复盘状态并存

`reviewStatus` 与 `countSla` 独立 AND。未复盘 + 「是」或「否」结果为空集，允许，不在服务端改写组合。

### 3. UI

筛条增加「是否计算SLA」：全部 / 是 / 否。无查询串首次打开与清空均为全部（不传或 `all`）。已知 URL 键增加 `countSla`。总览跳转可不带该键。筛格已满，窄屏已 `1fr` 换行。

### 4. 实现落点

`ReviewEventQuery` + `applyCountSla`（对齐 `applyReviewStatus`）+ mapper `AND count_sla = #{countSla}`。`events.html` 与 `events-preview.html` 同步。单测覆盖校验、AND、页面默认与 URL 水合。

## Risks / Trade-offs

- [未复盘 + 是/否为空] → 预期；文档与操作可接受。
- [筛条更挤] → 沿用现有窄屏换行。

## Migration Plan

部署即生效。回滚去掉参数与下拉；旧书签无 `countSla` 行为不变。

## Open Questions

无。
