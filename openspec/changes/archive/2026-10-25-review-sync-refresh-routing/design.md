# Design

## Context

见 `proposal.md`。启动时 `SlaSchemaInitializer` 已调用 `MonitorEditionBackfillService.backfill()`；`POST /api/sla/review-event-sync` 的 `refreshOwner` 仅更新 `sla_review_event.owner_name`。复盘保存依赖 `sla_system_owner.write_jdbc_key` 指向正确 Zabbix 库。

## Goals / Non-Goals

**Goals:**

- 手工 POST 可选触发与启动相同的字典路由 backfill。
- 与 `refreshOwner` / `refreshExcludeTime` / 默认 sync 可组合；固定执行顺序。
- 响应携带路由更新统计；README 给出运维 SOP。

**Non-Goals:**

- 不改 `refreshOwner` 语义；不写入 `sla_review_event` 路由字段。
- 不做「仅 backfill 本次采集系统」的增量 Grafana 扫描（第一版全表 backfill）。
- 不在此变更中实现保存 API 中文错误（另案）。
- cron 定时任务不新增 `refreshRouting` 参数（无法带 query）。

## Decisions

### 1. 新参数 `refreshRouting`（默认 false）

与 `refreshExcludeTime` 并列，仅作用于 `POST /api/sla/review-event-sync`。为 true 时调用现有 `MonitorEditionBackfillService.backfill()`，不复制逻辑。

### 2. 请求内执行顺序

```
if refreshRouting -> backfill()
if refreshExcludeTime -> refreshExcludeTime (existing)
else if refreshOwner -> owner pass only
else if !refreshExcludeTime only path -> default sync
```

当 `refreshRouting=true` 且 `refreshExcludeTime=true` 时：先 backfill，再 refreshExcludeTime（不采宕机），与现 spec 一致。

当 `refreshRouting=true` 且 `refreshOwner=true` 且二者均非 exclude-only：先 backfill，再 refreshOwner。

当仅 `refreshRouting=true`（owner/exclude 均 false）：只 backfill，返回 message，不采 Grafana 宕机。

### 3. Backfill 返回值

将 `backfill()` 改为返回小型结果（如 `rowsUpdated`、`collisionCount`、`grafanaDiscoveryOk`），写入 `ReviewEventSyncResult` 新字段，并拼入 `message`。

### 4. 复用启动 backfill 规则

Grafana 失败时仍回落老版/zabbix6；冲突面板跳过更新。与 `system-owner-dictionary` 主 spec 一致，避免双实现。

**备选（未采用）**：独立 `POST /system-owner-routing-backfill` — 运维多记一个 URL；B1 与 refreshOwner 同入口更顺。

## Risks / Trade-offs

- [Risk] 生产上手工 backfill 在 Grafana 失败时把行打回 zabbix6 → Mitigation：响应/message 标明发现失败；日志保留现有 warn。
- [Risk] 全表 backfill 请求耗时、Grafana 压力 → Mitigation：仅运维手工调用；与启动同量级。
- [Risk] 开发机误调生产 → Mitigation：文档强调在生产执行；长期可考虑鉴权（非本变更）。

## Migration Plan

1. 部署新版本 jar。
2. 运维 SOP：补 `sla_system_owner` 后调用 `POST ...?refreshRouting=true&refreshOwner=true`，再复盘保存。
3. 回滚：旧 jar 无参数；字典已被 backfill 的数据保留（无 schema 变更）。

## Open Questions

无。
