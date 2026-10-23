## Context

复盘 PUT 成功后 `ExcludedDowntimeWriteService` 向目标库 `sla_excluded_downtime` INSERT。去重键是 `slaid + period_from + period_to`。`period_from` 来自不变的 `down_start`；`period_to` 来自 `exclude_time`（「是」= 开始+1秒，「否」= `down_end`）。编辑把是改成否（或反过来）会换 `period_to`，当前会再插一行，旧窗仍在。

`review-save-excluded-downtime` 尚未归档；本变更改同一写入服务。

## Goals / Non-Goals

**Goals:**

- 同一宕机（同一 SLA、同一开始时刻）在排除表只保留一行；是/否变化时改 `period_to` 和 `name`。
- 首次保存仍 INSERT。

**Non-Goals:**

- 不改 `events.html`、复盘表、`ids` 发号规则（仅新行）、132 DDL、历史全量清洗任务（仅在再次保存该行时收敛重复窗）。

## Decisions

### 1. 定位键改为 slaid + period_from

`down_start` 保存时不改，适合作为「这一次宕机」的稳定键。不再用完整三元组判断「是否已存在」。

查目标库：`SELECT sla_excluded_downtimeid FROM sla_excluded_downtime WHERE slaid = ? AND period_from = ? ORDER BY sla_excluded_downtimeid`。

### 2. 有则 UPDATE，无则 INSERT

零行：现有 INSERT（`ids` `nextid+1`）。  
至少一行：对 **最小** `sla_excluded_downtimeid` 执行 `UPDATE name, period_to`（`period_from` 不变）；其余同键行 DELETE。禁止为换窗再 INSERT。

同窗再保存也会 UPDATE（`period_to` 相同则写相同值；`name` 若分类/描述变了会写回去）。实现简单，且避免只改文案时排除表 name 陈旧。

### 3. 不读保存前的 exclude_time

复盘 UPDATE 先提交新 `exclude_time`。用 `period_from` 定位，不必在 UPDATE 前快照旧 `period_to`。

## Risks / Trade-offs

- [两段真实宕机同一 `sla.name` 且 `down_start` 精确到同一秒] → 会合成一行。复盘主表按开始+结束区分，实际同步几乎不会同秒两条。
- [已叠两行的历史] → 该条再保存时收敛为一条；未再保存的仍可能重复，不另做全库脚本。

## Migration Plan

部署后对新的保存/再保存生效。回滚：恢复「三元组存在则跳过、否则 INSERT」。已 UPDATE 的行保留最后一次窗口。

## Open Questions

无。
