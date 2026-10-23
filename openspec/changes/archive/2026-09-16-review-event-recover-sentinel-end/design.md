## Context

`ReviewEventSyncMapper.UNRECOVERED_END` 为 `9999-12-31 00:00:00`。INSERT IGNORE 唯一键含 `down_end`，Grafana 从「未恢复」变为 `2026-09-13 11:23:28` 后旧行不更新。列表 `formatTime` 只替换 `T`。约束：不改唯一键列、不覆盖人填字段、不在本 change 写 `sla_excluded_downtime`。

## Goals / Non-Goals

**Goals:**

- 同一次宕机（level + name + start + period_month）哨兵结束时间提升为 Grafana 可解析结束时间。
- 哨兵与真结束两行并存时折叠为一行。
- 列表把哨兵显示为「未恢复」。
- `count_sla=否` 时 promote 后 `exclude_time` 跟新 `down_end`。

**Non-Goals:**

- 不改唯一键定义、通知、周报 Excel。
- 不在 promote 时写排除表。
- 不把 API JSON 里的 `downEnd` 改成「未恢复」字符串（仅 UI）。

## Decisions

### 1. 身份不含结束时间

同一次宕机 = `period_month, system_level, system_name, down_start`。结束时间只是状态。

备选：改唯一键去掉 `down_end`。拒绝：NULL/哨兵与历史行迁移面大。

### 2. 同步顺序：INSERT IGNORE，再 promote

对每条采集行：

1. `insertIgnore`（未恢复仍写哨兵）。
2. 若映射后的 `down_end` **不是**哨兵：查找同身份且 `down_end = UNRECOVERED_END` 的行。
   - 仅哨兵：`UPDATE down_end` 为采集值；若 `count_sla` 为 `否`，`exclude_time = down_end`；`是` 或空不改 `exclude_time`。人填字段不动。
   - 哨兵 + 已有相同真 `down_end`：把哨兵上非空人填字段拷到真结束行（真结束行已有值则保留），然后 DELETE 哨兵。
   - 无哨兵：结束。

Cron 与默认 POST 都走这套。`refreshExcludeTime` 单独模式不采集 Grafana，不 promote。

### 3. refreshOwner 按开始时间匹配

`updateOwnerByIdentity` 去掉 `down_end` 条件（同身份可能多行时全部更新责任人）。不 insert。

### 4. 展示只在前端

`formatTime`：解析后日期为 `9999-12-31`（前缀匹配 `9999-12-31`）→ `未恢复`。API 仍返回 DATETIME。`events.html` 与 `events-preview.html` 一致。

## Risks / Trade-offs

- [UPDATE 撞唯一键] → 先查真结束行再决定 UPDATE 还是合并删除。
- [已复盘否 + 排除表仍按旧 exclude_time] → 下次保存会按 `period_from` 更新；本 change 不自动写 132/133 排除表。
- [同 start 两次真实宕机] → Grafana 视为同一开始；不拆行。

## Migration Plan

1. 发版后下一次同步提升历史哨兵。
2. 回滚上一 jar：列表再显示 9999；已 UPDATE 的 `down_end` 留在库里。

## Open Questions

无。
