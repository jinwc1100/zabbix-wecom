## Context

主库是 `spring.datasource` → `10.32.201.133`（zabbix6）：全部 `sla_*` DDL 与复盘/快照/字典读写都在这里。`sla.datasources.zabbix7` 已在 yml 指向 `10.32.201.132`，尚未建池。字典无监控版本列。操作约定：同一系统名不会同时出现在新/老面板。132 现有 Zabbix 7 表，**没有** 应用侧 `sla_review_event` / `sla_sli_snapshot`；本阶段 **不去建**。

## Goals / Non-Goals

**Goals:**

- 133 字典增加 `monitor_edition` 与 `write_jdbc_key`，并用 SLA 看板 panel 4/2 回填（供后期选库插入）。
- 接线 zabbix7 连接池，启动可连 132，但本 change 不向 132 写业务行、不在 132 建表。
- 主库行为保持：同步、快照、复盘、总览、催办仍只使用 133。

**Non-Goals:**

- 在 132 CREATE/ALTER 任何表。
- 按面板把 `sla_review_event` / `sla_sli_snapshot` 写入 132。
- 复盘列表双库合并、`writeJdbcKey` API、双库未复盘统计。
- 改 Grafana 看板、xlsx 列、字典唯一键。

## Decisions

### 1. 133 是唯一主库

所有 MyBatis 默认与本阶段全部 mapper 调用走 zabbix6。`write_jdbc_key` 只是字典元数据，**本 change 不根据它切换写路径**。

### 2. 写库键是逻辑名

`zabbix6` = 133；`zabbix7` = 132。`老版监控`↔`zabbix6`，`新版监控`↔`zabbix7`。不存 IP。

### 3. 预留第二池，延迟插入

建 zabbix7 Hikari，供后续 change 向 132 **已有或另行准备的表** INSERT。本 change 的 `SlaSchemaInitializer` **禁止** 在 zabbix7 上执行 CREATE/ALTER。

备选：本阶段只改 yml 不建 Bean。拒绝：后期插入需要可注入的 DataSource；Bean 可以存在但零业务写入。

### 4. 回填仍以 Grafana 面板为准

panel 4 → 老版/zabbix6；panel 2 → 新版/zabbix7。撞名记错误并跳过。未命中看板 → 默认老版/zabbix6。不改 owner/mobile。

### 5. 不做 Routing 写分流（本阶段）

可以注册第二 DataSource Bean 而不接 `AbstractRoutingDataSource`。若接 Routing，默认键必须是 zabbix6，且没有任何调用设置 zabbix7。推荐：**独立 `@Bean("zabbix7DataSource")`，主 DataSource 仍自动配置**，避免误切库。

## Risks / Trade-offs

- [启动会连 132，132 宕则应用起不来] → Hikari 可设较短失败日志；或文档要求 132 可达。不在本 change 做降级只读 133，除非启动探测失败仍继续（可选：zabbix7 连接失败只 warn）。**决定：zabbix7 池初始化失败 MUST 只记日志，MUST NOT 阻止 133 主库启动。**
- [字典标了 zabbix7 但 132 无应用表] → 预期；后期插入前由运维/下一 change 准备表。
- [回填依赖 Grafana] → 失败则默认 zabbix6。

## Migration Plan

1. 部署：133 ALTER 字典列并回填；注册 zabbix7 池；业务读写不变。
2. 回滚：去掉字典列非必须；主库数据未迁。

## Open Questions

无。
