## Context

`WeeklyReviewDataService.collect` 已实现窗口 + 未排除过滤 + 责任人。`zabbix.sla_review_event` 已由 MyBatis 建表，唯一键 `(period_month, system_level, system_name, down_start, down_end)`。应用目前无 `@Scheduled`。Grafana 仍是宕机真相源；本能力只把累计未排除行幂等写入 MySQL。

## Goals / Non-Goals

**Goals:**

- 每天 17:00（可关）及手工 POST：collect 后写入主表。
- `period_month` = 宕机开始时间 `yyyyMM`（`Asia/Shanghai`）。
- 已存在则整行跳过；不写、不改分类相关列。
- 未恢复结束时间用哨兵 datetime，保证唯一键生效。

**Non-Goals:**

- 不改 Owner 规则、年度 SLI notify、腾讯文档/企微周度管道。
- 不从库回写 Grafana，不删除库中已不在 Grafana 的行（已复盘排除的历史行保留）。
- 不按年 SLI 过滤。

## Decisions

### 1. 复用 collect，不新写 Grafana 过滤

- **选择**：`ReviewEventSyncService` 调用 `WeeklyReviewDataService.collect(triggerTime)`，`triggerTime` 为当天 17:00 或 `now`（手工时用 now，但窗口上沿仍截到 min(now, 当天 17:00) 还是 now？）
- **手工**：用 `LocalDateTime.now(zone)`，上沿即触发时刻；若在 17:00 前补跑，只会同步到当前时刻，与「至 17 点」略有差别。定时任务在 17:00 触发则对齐需求。
- **备选**：手工也强制上沿为当天 17:00（未来时刻则仍用 now）。采用 **定时 = 当天 17:00；手工 = now**，简单可测。

### 2. INSERT IGNORE，不 UPDATE

- **选择**：MyBatis `INSERT IGNORE` 映射五列业务字段 + period_month + down_end 哨兵。冲突 0 行影响。
- **备选**：先 SELECT 再 INSERT 有竞态；`ON DUPLICATE KEY UPDATE owner_name=...` 会改责任人，违反「跳过整行」。

### 3. period_month 来自开始时间

- 解析 `downtimeStart` 为 LocalDateTime，`yyyyMM`。无法解析则跳过该行并打日志，不插入。

### 4. 未恢复哨兵

- 结束展示为空白 / `未恢复` / 无法解析 → `down_end = 9999-12-31 00:00:00`。
- 读出给 Excel 时再映射回「未恢复」（本 change 不要求读出）。

### 5. 调度

- `@EnableScheduling` + `ReviewEventSyncScheduler`，`cron: 0 0 17 * * *`，`zone: Asia/Shanghai`，`review-sync.enabled` 默认 `false`。
- `POST /api/sla/review-event-sync` 忽略开关。

### 6. 配置

- `review-sync.enabled` / `cron` / `zone`；窗口继续 `weekly-review.window-start`。

## Risks / Trade-offs

- [Grafana 结束时间从「未恢复」变为具体时刻] → 唯一键含 down_end，会再插一行。接受：视为新状态；若以后要合并需另开 change。
- [哨兵 9999 出现在报表] → 文档约定；后续导出再翻译。
- [日任务全量 collect 数百条] → 可接受；INSERT IGNORE 便宜。
- [只读账号无 INSERT] → 失败打日志；需写权限。

## Migration Plan

1. Mapper + Service + 单测（月份、哨兵、ignore）。
2. 调度默认关，手工 POST 验数。
3. 打开 `review-sync.enabled`。回滚：关开关。

## Open Questions

- 无。结束时间变化导致第二行已记录为已知限制。
