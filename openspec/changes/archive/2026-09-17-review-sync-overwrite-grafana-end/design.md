## Context

复盘同步 `insertIgnore` 唯一键含 `down_end`；仅哨兵 `9999-12-31` 可 UPDATE。Grafana 改结束时间不会覆盖已有行；`queryAllRows` 默认 `now-1h`，历史宕机常不在采集集。`writeAfterSave` 在复盘保存时写排除表，同步 promote 当前不写排除库。

约束：Java 8、8086、人填字段不覆盖、无排除行时同步不 INSERT 排除表、132 上已有排除行只 UPDATE。

## Goals / Non-Goals

**Goals:**

- 复盘采集 Grafana from = `weekly-review.window-start` 的 epoch millis，to = 触发时刻。
- 同一开始时间：Grafana 可解析结束覆盖库中 `down_end`（含哨兵与已有真结束）。
- `否` 更新 `exclude_time` 与已有 `sla_excluded_downtime.period_to`。
- 同开始多行折叠为一条对准 Grafana 结束的行。

**Non-Goals:**

- 不放宽排除状态过滤（仍 `未排除` / `-`）。
- 不改子系统 `end-column: 恢复时间`（看板列名另案）。
- 不同步插入新的排除行；不改排除 `name`。
- 不改总览计入 SLA 明细口径。

## Decisions

1. **采集窗口**  
   `DowntimeQueryService.loadIndex` 增加 from/to；复盘 `WeeklyReviewDataService.collect` 传入 window-start millis 与 trigger millis。其它仍走 `queryAllRows` 默认 `now-1h` 的调用保持不变（若仅 review 路径改 overload）。

2. **对齐顺序**  
   对每条采集：先 `fromRow`；结束仍是哨兵则只 INSERT IGNORE（新未恢复）。结束可解析则 `listByStartIdentity`：  
   - 空：INSERT IGNORE。  
   - 存在结束=T 的行：将其余同开始行折叠进该行后删除。  
   - 否则：选保留行（优先已有非空 `count_sla`，否则最小 `id`），UPDATE `down_end`（及 `否` 的 `exclude_time`），再折叠删除其它行。  
   禁止对已有开始时间再 INSERT 新结束。

3. **排除 period_to**  
   仅当保留行 `count_sla=否` 且 `exclude_time` 已更新。用字典 `write_jdbc_key` 找到 `slaid`，按 `period_from=down_start` 更新已有排除行的 `period_to`；0 行则跳过。失败记日志，**不**让整次同步失败（避免缺 SLA 挡住其它系统）。

4. **日志**  
   计数：更新结束、折叠、排除 period_to 更新。

## Risks / Trade-offs

- [Grafana `$__timeFilter` 仍吃 from] → 窗口拉到 window-start 才能采到历史恢复。  
- [已排除行仍采不到] → 本变更不扩过滤；未恢复且已排除的仍不更新。  
- [排除 UPDATE 失败] → 主表已改结束，排除库可能旧；日志可手工补。  
- [多行折叠删行] → 允许 DELETE 同开始的重复复盘行（本变更范围内）。

## Migration Plan

发版后下一次 17:00 或手工 POST 即覆盖。回滚上一版 JAR。无需改表结构。

## Open Questions

- 无。排除过滤与结束列名保持现状。
