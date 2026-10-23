## Why

下游需要根据复盘结果计算排除结束时刻：计入 SLA 时只排除约 1 秒，不计入时排除整段宕机。`sla_review_event` 目前没有这列，已复盘存量也无法批刷。

## What Changes

- 表增加可空列 `exclude_time`（`DATETIME`）。启动时对已有库 `ALTER`；新建表写进 `CREATE TABLE`。
- 复盘保存成功后按 `count_sla` 写入：`是` 为 `down_start + 1秒`（若晚于或等于 `down_end` 则用 `down_end`）；`否` 为 `down_end`。未复盘保持 `NULL`。
- 日同步插入仍不写这列。Cron 不刷这列。
- `POST /api/sla/review-event-sync?refreshExcludeTime=true` 按同一公式扫表重算已复盘行，不采集 Grafana、不插入。
- 复盘总览与列表页不展示、不编辑该列。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-review-event-web`: 表结构含 `exclude_time`；保存时派生写入；页面不展示。
- `daily-sla-review-event-sync`: 手工 POST 增加 `refreshExcludeTime`；默认同步与 Cron 不写/不刷该列。

## Impact

- Schema：`SlaSchemaMapper` / `schema-sla-review.sql`、启动迁移。
- 保存：`ReviewEventService.update`、`updateReviewFields`。
- 同步：`SlaNotifyController`、`ReviewEventSyncService`（新模式，勿套 Grafana `refreshOwner` 采集）。
- 测试：schema/save/sync/controller；列表 HTML 断言不出现该列展示。
- 无新外部依赖。下游按列名 `exclude_time` 读取。
