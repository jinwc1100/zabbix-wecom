## Context

`sla_review_event.exclude_time` 由保存 PUT 和 `refreshExcludeTime` 按 `count_sla` 派生。当前「是」使用 `LEAST(DATE_ADD(down_start, INTERVAL 1 SECOND), down_end)`，与产品要求的固定「开始 +1 秒」不一致。Java `ExcludeTimes` 与 SQL 需保持同一规则。本应用仍不把该列写入 Grafana 排除表。

## Goals / Non-Goals

**Goals:**

- 「是」：`exclude_time = down_start + 1秒`，不与 `down_end` 比较。
- 「否」/ 空：行为不变。
- 保存与整表刷新共用同一公式。

**Non-Goals:**

- 不回写 `sla_excluded_downtime` 或 Grafana。
- 不改同步插入、cron、`refreshOwner`、页面展示。
- 不改 `down_start` / `down_end`。
- 不批量改历史行，除非走保存或 `refreshExcludeTime`。

## Decisions

1. **SQL 去掉 `LEAST`，Java 去掉 clamp**  
   「是」一律 `DATE_ADD(down_start, INTERVAL 1 SECOND)`；`ExcludeTimes.derive` 在 `是` 且 `downStart != null` 时返回 `downStart.plusSeconds(1)`。  
   备选：仅改 Java、SQL 仍 `LEAST` —— 否决，落库以 SQL 为准会分叉。

2. **时长 ≤ 1 秒允许 `exclude_time` > `down_end`**  
   产品明确不要夹结束。起止相同或不足 1 秒时排除结束可晚于宕机结束。  
   备选：保留 clamp —— 否决，与本次需求相反。

3. **`down_start` 为 NULL 时「是」**  
   保持现有 Java：退回 `downEnd`；SQL `DATE_ADD(NULL, …)` 为 NULL。正常行由 Grafana 同步写入开始时间，不为此新增校验。

## Risks / Trade-offs

- [超短故障排除结束晚于宕机结束] → 接受；文档化。下游若按闭区间 `[start, exclude_time]` 解释，可能多 1 秒；本变更不接 Grafana。
- [已有「是」且已夹过的行仍是旧值] → 部署后需再保存或 `POST ...?refreshExcludeTime=true` 才会改写。
- [SQL 与 Java 再次分叉] → 测试同时覆盖 mapper 字符串与 `ExcludeTimes`。

## Migration Plan

1. 发版后默认不扫表。
2. 若要对已复盘「是」行对齐新公式：调用 `refreshExcludeTime=true`。
3. 回滚：恢复 `LEAST` / clamp 后再跑一次 refresh。

## Open Questions

- 无。`否` 与未复盘保持原规则。
