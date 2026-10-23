## Why

项目负责人需要在页面上补全宕机复盘（分类、描述、解决过程、根本原因、改进措施），并按系统、时间、责任人模糊查找。现有 `sla_review_event` 只有机器同步列和旧的备注列，没有复盘长文列，也没有查询/逐行保存接口。

## What Changes

- **BREAKING（表）**：`zabbix.sla_review_event` 删除 `remark`；新增可空 `TEXT` 列 `resolution_process`（解决过程）、`root_cause`（根本原因）、`improvement`（改进措施）。已有行这三列为 NULL。库约束保持可空，以便日同步只插机器列。
- 启动时对已存在表做幂等 `ALTER`（加列、删 `remark`）；新环境建表语句与之一致。
- 日同步 `INSERT IGNORE` 仍只写机器列；冲突跳过时 **不得** 覆盖三类复盘长文及分类字段。跳过名单去掉 `remark`。
- 后端提供列表（系统名称 / 时间 / 责任人模糊查询）和 **逐行保存**。保存时必填：故障类别、故障子类、故障描述、解决过程、根本原因、改进措施；「是否计算 SLA」由字典带出，不单独手填。不处理未恢复行的特殊展示。
- 前端 Vue 复盘页：主表保留分类与描述；解决过程 / 根本原因 / 改进措施在展开区填写；主表显示复盘 `n/3`；去掉备注。`review-web-demo` 与接 API 的页面交互一致。
- 不改 `POST /api/sla/notify`、责任人规则、周度腾讯文档管道。无登录鉴权（负责人共用）。

## Capabilities

### New Capabilities

- `sla-review-event-web`: 复盘表结构（无备注、三列长文可空）、模糊查询、逐行保存校验、Vue 主表+展开交互。

### Modified Capabilities

- `daily-sla-review-event-sync`: 冲突跳过时不得覆盖新增长文列；不再提及 `remark`。

## Impact

- MySQL `zabbix.sla_review_event`；`SlaSchemaMapper` / `schema-sla-review.sql` / 启动 ALTER。
- `sla-wecom-notifier`：查询与保存 API、故障分类字典读取、日同步跳过字段名单。
- `review-web-demo` 及由 Spring Boot 提供的 Vue 静态页（同源 `/review/`）。
- 测试：建表/ALTER、保存校验、查询模糊匹配、同步不覆盖人填列。
