## Why

每日复盘同步只在哨兵 `9999-12-31` 时提升 `down_end`；Grafana 已恢复或事后改了结束时间，库里旧结束（含「未恢复」）常不更新。采集还用 Grafana `now-1h`，历史宕机进不了工作集，提升没有输入。需要同一开始时间以 Grafana 最新结束覆盖库行，并同步 `否` 的排除窗口。

## What Changes

- 采集 Grafana 宕机的时间范围改为 `weekly-review.window-start` → 触发时刻（不再默认 `now-1h`）。排除状态仍只要 `未排除` / `-`。
- 同一 `(period_month, system_level, system_name, down_start)`：Grafana 可解析结束时间时 **UPDATE** 已有行的 `down_end`，不靠 INSERT 再插一行。人填字段不改。
- `count_sla` 为 `否` 时 `exclude_time` 跟新 `down_end`；`是` 或空不改 `exclude_time`。
- 若目标库已有该系统该 `period_from` 的 `sla_excluded_downtime`，且本行 `否`，**UPDATE `period_to`**；无排除行则不同步插入。不改排除 `name`、不走完整保存写入口。
- 同开始时间多行（哨兵+真结束，或两个真结束）折叠为对准 Grafana 结束的一行。
- 新宕机（库中无该开始时间）仍 INSERT IGNORE。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `daily-sla-review-event-sync`: 采集窗口；按开始时间覆盖 `down_end`；`否` 的 `exclude_time`；折叠非哨兵重复行。
- `review-excluded-downtime-write`: 同步覆盖结束且 `否` 时，更新已有排除行的 `period_to`。

## Impact

- `GrafanaClient.queryAllRows` / `DowntimeQueryService` 或复盘采集调用处的 from/to。
- `ReviewEventSyncService` 提升/折叠。
- `SlaReviewEventMapper`（按 id 更新非哨兵 `down_end`）。
- 排除库：按 `slaid`+`period_from` 更新 `period_to`（zabbix6/zabbix7）。
- Java 8；132 上排除仅 UPDATE 已有行，不新 INSERT（无行则跳过）。
