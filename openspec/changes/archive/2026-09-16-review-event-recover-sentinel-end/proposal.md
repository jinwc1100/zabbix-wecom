## Why

复盘同步在 Grafana 结束时间为空/`未恢复`/无法解析时把 `down_end` 写成哨兵 `9999-12-31 00:00:00`，且唯一键包含 `down_end`，后续 Grafana 出现真实恢复时间也不会更新旧行。`events.html` 把哨兵原样显示（列宽下像 `9999-12-3`），与看板恢复时间对不上。需要在同步时把同一次宕机的哨兵结束时间提升为真实恢复时间，并在列表把哨兵显示为「未恢复」。

## What Changes

- 日常 Grafana 同步：同一 `period_month + system_level + system_name + down_start` 上，若库中结束时间为哨兵而采集到可解析结束时间，MUST UPDATE `down_end`（不插第二行）。
- 若真结束行已存在、哨兵行也在：删除哨兵行；人填字段跟有内容的那行走。
- `count_sla` 为 `否` 时，promote 后按保存规则把 `exclude_time` 设为新 `down_end`；`是` 不改 `exclude_time`。本 change **不**重写 `sla_excluded_downtime`。
- `refreshOwner` 按「同一次宕机」（不含结束时间）匹配，避免恢复后刷不到责任人。
- 列表页（`events.html` 与 demo）将哨兵 `down_end` 显示为「未恢复」。
- 未恢复仍写入哨兵；唯一键形状不变。不改通知/周报 Excel Grafana 原文。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `daily-sla-review-event-sync`: 同步时提升哨兵结束时间为 Grafana 真实恢复时间；冲突时折叠两行；责任人刷新按开始时间身份匹配。
- `sla-review-event-web`: 列表把哨兵结束时间显示为「未恢复」。

## Impact

- `ReviewEventSyncService`、`SlaReviewEventMapper`（UPDATE/DELETE by start identity）。
- `events.html`、`events-preview.html` 的 `formatTime`。
- 测试：`ReviewEventSyncServiceTest`、`ReviewEventSyncMapperTest`、页面断言。
- 历史 9999 行随下次同步提升，无单独 DDL/脚本。
