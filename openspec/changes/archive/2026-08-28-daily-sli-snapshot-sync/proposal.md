## Why

子系统/父系统 SLA 看板（`afj5c5o5v3rb4f`、`aftk5ryifcfeof`）目前只在总览和推送时现查 Grafana，库里没有一份可对账的日更快照。需要每日把看板全量行落到 MySQL，并支持手工再拉，避免只能依赖 Grafana 当场查询。

## What Changes

- 新建表（如 `zabbix.sla_sli_snapshot`）保存当年宽表：层级、系统名、面板（老/新）、年度 SLI、一月…十二月。
- 策略为 **覆盖最新**：同一 `report_year + 层级 + 系统名 + panel_id` 每天 UPSERT，不按日追加历史行。
- 定时：在现有日任务时段旁路执行 Grafana 拉取并写表；失败不阻断 `sla_review_event` 同步、不发企微。
- 手工：`POST` 接口立即同步，不受定时开关限制。
- 数据只走 Grafana HTTP（`grafana.monthly-sli-sources` 的 UID/面板 4 与 2），不直查看板底层库。
- **非目标**：本变更不改总览读路径（仍打 Grafana）；不改 notify/Excel；不改宕机复盘表。

## Capabilities

### New Capabilities

- `daily-sli-snapshot-sync`: 日更/手工把父子 SLA 看板行 UPSERT 到快照表。

### Modified Capabilities

- （无）总览与 notify 需求不变。

## Impact

- `SlaSchemaMapper` / 启动建表；新 Mapper 与同步服务。
- 调度器：独立 `@Scheduled` 或挂在 17:00 之后且互不影响。
- `POST /api/sla/...` 手工同步；`application.yml` 增加开关/cron（可选复用 `review-sync` 时刻）。
- 写 `zabbix` 库（与现有 `sla_*` 相同数据源）。
