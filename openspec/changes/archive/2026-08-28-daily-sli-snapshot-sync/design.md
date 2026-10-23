## Context

父子 SLA 看板 UID 已在 `grafana.sli-sources` 与 `grafana.monthly-sli-sources`（`afj5c5o5v3rb4f` / `aftk5ryifcfeof`，面板 4 与 2）。拉取走 `GrafanaClient.queryAllRows`。写库模式对齐 `sla_review_event`：启动 `CREATE TABLE IF NOT EXISTS`，定时 + 手工 POST。总览仍现查 Grafana。

## Goals / Non-Goals

**Goals:**

- 日更与手工把看板全量行 UPSERT 到快照表。
- Grafana HTTP only；新老版分行；非数字月格为 NULL。
- 同步失败不影响复盘日同步与催办。

**Non-Goals:**

- 不按 `sync_date` 追加历史。
- 不改总览/notify/Excel 读路径。
- 不直查 Grafana 底层 MySQL。

## Decisions

### 1. 表 `zabbix.sla_sli_snapshot`，覆盖最新

唯一键：`(report_year, system_level, system_name, panel_id)`。

列：`sli_annual DECIMAL` 可空；`sli_m01`…`sli_m12` 可空；`panel_name`；`synced_at`。`report_year` 取同步时刻 `Asia/Shanghai` 的年（与看板「当年」宽表一致）。

备选：每日一行。拒绝：表膨胀且看板本身是当年覆盖语义。

### 2. 采集复用 `monthly-sli-sources`

同一 UID/面板含年度列 `年度SLI` 与 `一月`…`十二月`。名称列沿用 source `name-columns`。空名跳过。解析：去 `%`，非数字 → NULL，不当 0。

Notify 仍只用 `sli-sources`，不读快照表。

### 3. 调度独立于复盘

`sli-snapshot-sync.enabled` / `cron` / `zone`，默认 cron `0 0 17 * * *` Asia/Shanghai。独立 `@Scheduled`，try/catch 隔离。手工 `POST /api/sla/sli-snapshot-sync` 忽略 enabled。

备选：塞进 `ReviewEventSyncScheduler`。拒绝：Grafana 失败会搅在一起。

### 4. UPSERT

`INSERT ... ON DUPLICATE KEY UPDATE` 刷新 SLI 列与 `synced_at`。Grafana 当天已消失的系统 **不删行**（保留最后一次快照）。

## Risks / Trade-offs

- [Grafana 超时] → 沿用 `read-timeout-ms`；失败记日志，表保持上次成功值。
- [账号只读] → 与现有 `sla_*` 相同数据源；INSERT 失败即暴露授权问题。
- [系统改名] → 旧名行残留；可接受。

## Migration Plan

1. 发版后启动建表，等 17:00 或调手工接口。
2. 回滚：停调度/接口；表可留。

## Open Questions

无（覆盖策略与总览仍走 Grafana 已定）。
