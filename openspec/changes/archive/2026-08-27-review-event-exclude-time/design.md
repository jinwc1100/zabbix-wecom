## Context

`sla_review_event` 已有 `down_start` / `down_end` / `count_sla`。复盘 PUT 从故障分类字典写入 `count_sla`，日同步 `INSERT IGNORE` 不碰人填列。`refreshOwner` 走 Grafana 采集再按唯一键改责任人。`exclude_time` 只依赖本表三列，不能套那条采集链。

## Goals / Non-Goals

**Goals:**

- 可空列 `exclude_time DATETIME`；启动补列；新建表带上。
- 保存时按约定派生；短于（含等于）1 秒的 `是` 用 `down_end`。
- `refreshExcludeTime=true` 扫表重算已复盘行。
- 复盘总览、列表、demo 不展示该列。

**Non-Goals:**

- 不回写 Grafana 排除状态。
- 不改催办、总览 KPI、未复盘判定（仍看空 `count_sla`）。
- Cron / 默认同步不刷该列。
- 列名不使用 `Exclude_time`。

## Decisions

### 1. 公式（含短宕机）

- `count_sla = 是`：`candidate = down_start + 1 second`；若 `candidate` 晚于或等于 `down_end`，则 `exclude_time = down_end`，否则 `candidate`。
- `count_sla = 否`：`exclude_time = down_end`（含未恢复哨兵 `9999-12-31 00:00:00`）。
- 空 / NULL `count_sla`：`exclude_time` 为 NULL。

保存用本行已有 `down_start`/`down_end`（SQL `CASE`/`LEAST` 或先读后写均可）。禁止改 `down_start`/`down_end`。

### 2. 存量刷新扫表，不走 Grafana

`POST /api/sla/review-event-sync?refreshExcludeTime=true`：`UPDATE` 已复盘行（`count_sla` 为 `是`/`否`）；空 `count_sla` 置 `exclude_time` NULL。不 `collect`、不插入。

与 `refreshOwner` 独立：可同时为 true（先/后均可）。仅 `refreshExcludeTime` 时不访问 Grafana。Cron 只走插入模式。

Query 名：`refreshExcludeTime`（默认 false）。

### 3. 迁移

对齐 `resolution_process`：`information_schema` 无列则 `ALTER ADD exclude_time DATETIME NULL`。`CREATE TABLE` 与文档 schema 同步。列表 SELECT 可不返回该列，页面自然看不见；若 JSON 带上，前端仍不得渲染。

### 4. 不改唯一键

仍是 `(period_month, system_level, system_name, down_start, down_end)`。

## Risks / Trade-offs

- [存量一次全表 UPDATE] → 行数有限；只更新已复盘 + 清空未复盘脏值。
- [只刷 Grafana 仍在未排除集里的行会漏] → 已选扫表，避免该风险。
- [列表 API 多一个字段] → 可不 SELECT；页面禁止展示。

## Migration Plan

1. 发布含启动 `ALTER` 的版本。
2. 运维调用 `POST /api/sla/review-event-sync?refreshExcludeTime=true` 刷存量。
3. 之后只靠保存写入。回滚：停用新逻辑后列可留（可空）。

## Open Questions

无（列名、短宕机、页面、刷新路径已锁定）。
