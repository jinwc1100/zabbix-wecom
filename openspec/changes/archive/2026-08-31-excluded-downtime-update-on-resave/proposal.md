## Why

复盘再次保存时，排除表按 `slaid + period_from + period_to` 去重。`down_start` 不变，但「是/否计算SLA」会改 `exclude_time`，于是 `period_to` 变了，当前实现会再 INSERT 一行，旧窗口留在 Grafana 里叠两段排除。需要改成改原行，不要第二条。

## What Changes

- 排除写入按目标库 `slaid + period_from`（`down_start` 的 Unix 秒）定位已有行。
- 已有行：UPDATE `period_to`（新的 `exclude_time`）和 `name`；不得再 INSERT。
- 无行：仍按现逻辑 INSERT（`ids` 发号）。
- 同一 `slaid + period_from` 已有多行（历史叠窗）：保留一行并 UPDATE，删除其余，使该键只剩一条。
- 不改 `events.html` 请求体、字典、132 建表。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `review-excluded-downtime-write`: 再保存时按宕机开始时间更新排除窗口，而不是因 `period_to` 变化再插一行。

## Impact

- `ExcludedDowntimeWriteService` 与单测。复盘 PUT 仍先写 `sla_review_event` 再写排除表。
