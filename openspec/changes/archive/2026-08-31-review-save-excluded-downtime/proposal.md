## Why

复盘保存只更新 133 的 `sla_review_event`，Zabbix 看板仍按 `sla_excluded_downtime` 计算排除。需要在保存本行后把排除窗口写回对应库（老版 133 / 新版 132），否则复盘结果不会进入 SLA 统计。

## What Changes

- 复盘 PUT 成功后：用该行 `(system_level, system_name)` 查 133 的 `sla_system_owner.write_jdbc_key`，在 **zabbix6 或 zabbix7** 的现有 `sla_excluded_downtime` 插入一行。
- `period_from` / `period_to` 分别为 `down_start`、`exclude_time` 的 Unix 秒；`name` 为 `故障分类:{fault_category}--故障子类:{fault_subcategory}--故障描述:{fault_desc}`；`slaid` 为 **目标库** `sla` 中 `name = system_name` 的 `slaid`（操作约定：一定命中；未命中则本次保存失败）。
- 主键走 Zabbix `ids` 表，不在 132 建表。不改 `events.html` 请求体（仍只 PUT 复盘字段）。
- Grafana 查询路径不变。不迁历史已保存行。

## Capabilities

### New Capabilities

- `review-excluded-downtime-write`: 保存后按字典选库写入 `sla_excluded_downtime`（含 `ids` 发号与 Unix 时间）。

### Modified Capabilities

- `sla-review-event-web`: 逐行保存成功后必须完成上述排除写入，否则保存视为失败。

## Impact

- `ReviewEventService.update`、zabbix7 `DataSource` 首次业务写入、133/132 的 `sla` / `sla_excluded_downtime` / `ids`。
- 会改变 Grafana SLA 排除区间（「是」为约 1 秒窗口，「否」为整段宕机）。
- 不改看板 SQL、不改字典回填、不改日同步。
