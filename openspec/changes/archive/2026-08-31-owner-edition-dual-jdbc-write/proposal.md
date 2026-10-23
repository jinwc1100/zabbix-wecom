## Why

新版监控与老版监控分属 132（Zabbix 7）与 133（Zabbix 6）。责任人字典没有版本/写库标记，后期无法按规则往 132 插数。本阶段只在 **133 主库** 的字典上记下目标库，并预留 132 连接；**不在 132 建任何表，也不把现有复盘/快照写入改到 132**。

## What Changes

- 在 133 的 `zabbix.sla_system_owner` 增加 `monitor_edition`（`老版监控` | `新版监控`）与 `write_jdbc_key`（`zabbix6` | `zabbix7`）。唯一键仍为 `(system_level, system_name)`。
- 启动时用看板 `afj5c5o5v3rb4f`、`aftk5ryifcfeof` 的 panel 4/2 回填这两列；Excel 种子仍只插层级/名称/责任人/手机。
- 将 `sla.datasources.zabbix7` 接到第二套 Hikari，供 **后期** 向 132 插入数据。`zabbix6` 即 `spring.datasource`（133），仍是唯一主库。
- **本 change 不得在 132 上 CREATE/ALTER 任何表。** 应用启动 DDL、日同步、快照、复盘页读写、总览未复盘、催办全部仍只打 133。
- Grafana SLI/宕机查询仍走 `/api/ds/query`。

## Capabilities

### New Capabilities

- `monitor-edition-jdbc-routing`: 第二套 JDBC 仅预留；禁止 132 DDL；本阶段全部业务读写走 zabbix6。

### Modified Capabilities

- `system-owner-dictionary`: 字典增加版本与写库键；Grafana 回填；查找仍按层级+名称；表只在 133。

## Impact

- `sla-wecom-notifier`：`sla.datasources.zabbix7` 接线、`SlaSchemaInitializer` 字典列与回填。
- MySQL：仅 133 ALTER `sla_system_owner`。132 无 schema 变更。
- 不改 Grafana 看板、xlsx 列、复盘/快照同步目标库、复盘 API 契约。
