## Why

`count_sla` 为「是」时，`exclude_time` 目前写成 `min(down_start + 1秒, down_end)`，短于 1 秒的宕机会把排除结束夹到 `down_end`。产品口径要固定为「计入 SLA 时排除窗口结束时刻永远是开始后 1 秒」，不再和结束时间比较。

## What Changes

- 复盘保存成功后：`count_sla` 为「是」时 `exclude_time` 必须等于该行 `down_start` 加 1 秒，即使该时刻晚于 `down_end`。
- 手工 `refreshExcludeTime` 使用同一派生规则，避免保存与整表刷新分叉。
- 「否」仍为 `down_end`；空 / NULL `count_sla` 仍为 `NULL`。不改 `down_start` / `down_end`，不写 Grafana / `sla_excluded_downtime`，页面仍不展示该列。
- **BREAKING**（库内已有「是」且时长 ≤ 1 秒的行）：保存或 `refreshExcludeTime` 后 `exclude_time` 可能从原先的 `down_end` 变为 `down_start + 1秒`。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-review-event-web`: 保存派生 `exclude_time` 时，「是」不再夹到 `down_end`。
- `daily-sla-review-event-sync`: `refreshExcludeTime` 与保存使用同一「是 = start+1s」规则。

## Impact

- SQL：`SlaReviewEventMapper.xml` 的 `updateReviewFields`、`refreshExcludeTime`。
- Java：`ExcludeTimes.derive` 及 `ExcludeTimesTest`；mapper/schema 字符串测试中的 `LEAST` 断言。
- HTTP 路径不变：`PUT /api/sla/review-events/{id}`、`POST /api/sla/review-event-sync?refreshExcludeTime=true`。
- 默认同步插入、cron、`refreshOwner`、总览/列表 UI 不变。
