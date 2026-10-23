## Why

Grafana 未排除宕机目前只用于填 Excel / 腾讯文档，`zabbix.sla_review_event` 主表仍为空。需要每日把父/子系统宕机累计窗口同步进库，供复盘落库与后续按月查询，且同一宕机不得重复、不得覆盖人填分类。

## What Changes

- 每日 **17:00**（`Asia/Shanghai`）拉取宕机开始时间 ∈ `[window-start, 当天 17:00]`（默认 `2026-07-30 00:00:00`）、排除状态为 `未排除` 或 `-` 的子系统 + 父系统宕机。
- 写入 `sla_review_event`：父/子系统、系统名称、宕机开始/结束、责任人（现有 `OwnerRuleResolver`，不改规则）；`period_month` 为 **宕机开始时间的 `yyyyMM`**。
- **同一宕机只一行**：唯一键命中则 **跳过整行**（含责任人）；**不更新** 故障类别、故障子类、故障描述、是否计算 SLA、备注。
- 未恢复的结束时间写入固定哨兵 `9999-12-31 00:00:00`，避免 MySQL NULL 无法参与唯一约束。
- 恢复 `@EnableScheduling`；开关默认关闭；提供手工 POST 补跑。不推企微、不上传腾讯文档。不改 `POST /api/sla/notify`。

## Capabilities

### New Capabilities

- `daily-sla-review-event-sync`: 每日 17:00（及手工入口）将未排除宕机幂等同步到 `sla_review_event`。

### Modified Capabilities

- （无）周度腾讯文档管道保持独立；本能力只写 MySQL。

## Impact

- `sla-wecom-notifier`：调度、复用 `WeeklyReviewDataService.collect`、MyBatis `INSERT IGNORE`、配置 `review-sync.*` 或扩展 `weekly-review`。
- 表 `zabbix.sla_review_event` 已存在；无需改字典表。启动时仍由现有 `SlaSchemaMapper` 保证表在。
