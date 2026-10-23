## Context

复盘 PUT 只写 133 `sla_review_event` 并推导 `exclude_time`。Zabbix `sla` / `sla_excluded_downtime` 在 133 与 132 结构相同；排除主键无 AUTO_INCREMENT，靠 `ids`。字典 `write_jdbc_key` 已标明目标库。操作约定：`sla.name` 等于 `system_name`，目标库一定有唯一一行。

## Goals / Non-Goals

**Goals:**

- 保存成功后向目标库 `sla_excluded_downtime` 插入排除窗口，供 Grafana SLA 使用。
- 选库、查 `slaid`、Unix 时间、`ids` 发号、`name` 拼接。

**Non-Goals:**

- 132 建表、改 Grafana SQL、改 `events.html` 请求体、回填历史已保存行、按服务 tag 一对多插多条 SLA。

## Decisions

### 1. 选库与 slaid

133 用 `(system_level, system_name)` 读 `write_jdbc_key`。`zabbix6` → 主库；`zabbix7` → `zabbix7DataSource`。在 **该库** `SELECT slaid FROM sla WHERE name = system_name`。零行或字典缺失：保存失败（HTTP 错误）。不使用 133 的 `slaid` 去写 132。

### 2. 时间

`period_from` / `period_to` 为 INT Unix 秒，由已保存行的 `down_start`、`exclude_time` 按 `Asia/Shanghai` 转换。`exclude_time` 为空则失败（保存后「是/否」不应为空）。

### 3. name

`故障分类:` + category + `--故障子类:` + subcategory + `--故障描述:` + desc。超过 255 字符则截断。不写 IP、不写 jdbc key。

### 4. 发号

目标库 `ids`：`table_name=sla_excluded_downtime` 且 `field_name=sla_excluded_downtimeid`。下一主键 = `nextid + 1`，写入行后把 `nextid` 更新为该值。禁止假定 AUTO_INCREMENT。

### 5. 顺序与重复保存

先按现逻辑 UPDATE 复盘（得到 `exclude_time`），再插排除表。两库无法同事务；排除失败则接口失败，客户端可重试。同一 `slaid + period_from + period_to` 已存在则 **不再插入**（避免重复点保存叠两段排除）。`name` 变化不另开窗口。

### 6. 接线

zabbix6 用现有 MyBatis/`JdbcTemplate`。zabbix7 用已有 `@Bean zabbix7DataSource` 的 `JdbcTemplate`，不引入 `AbstractRoutingDataSource`。不对 zabbix7 跑 DDL。

## Risks / Trade-offs

- [排除失败时复盘已提交] → 接口仍报错；重试 UPDATE 幂等，排除靠时间窗去重。
- [写入会改 Grafana SLI] → 预期；「是」约 1 秒窗口对齐部分排除。
- [ids 并发] → 单实例应用可接受；多实例需后续锁。本系统单进程。

## Migration Plan

部署后仅对新的保存生效。回滚：停排除写入代码；已插入行留在 Zabbix。

## Open Questions

无（`sla.name` 必中已确认）。
