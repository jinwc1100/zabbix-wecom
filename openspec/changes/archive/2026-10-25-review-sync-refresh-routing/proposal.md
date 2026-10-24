# Proposal

## Why

运维在 `sla_system_owner` 补责任人并调用 `refreshOwner=true` 后，复盘页责任人已更新，但保存仍常因 `write_jdbc_key` 仍为默认 `zabbix6`（与 Grafana 新版监控 / 132 库不一致）而失败。该路由对齐目前只在应用**启动**时 `MonitorEditionBackfillService.backfill()` 执行，导致必须重启进程（或本地开发机启动）才能修复，且与「刷责任人」操作脱节。

## What Changes

- 为 `POST /api/sla/review-event-sync` 增加 query 参数 `refreshRouting`（默认 `false`）。
- 当 `refreshRouting=true` 时，在同一次请求内执行与启动时相同的 Grafana 监控版本 backfill（更新 `sla_system_owner.monitor_edition` 与 `write_jdbc_key`，不改 owner/mobile）。
- 执行顺序：`refreshRouting`（若 true）→ 现有 `refreshExcludeTime` / `refreshOwner` / 默认 sync 逻辑。
- 响应中增加路由回填统计（更新行数等），并在 message 中可读摘要。
- 文档：补字典后推荐 `refreshRouting=true&refreshOwner=true`，再复盘保存。
- 定时 cron 不得使用 `refreshRouting`（仅手工 POST）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `daily-sla-review-event-sync`：`refreshRouting` 参数、组合行为、cron 限制、响应字段。
- `system-owner-dictionary`：允许除启动外通过 review-event-sync 手工触发同一 backfill 规则。

## Impact

- `SlaNotifyController`、`ReviewEventSyncService`、`ReviewEventSyncResult`
- `MonitorEditionBackfillService`（返回结构化结果供 API）
- 测试：Controller/Service 组合参数与 backfill 调用顺序
- `sla-wecom-notifier/README.md` 运维说明
