## Context

保存 PUT 已调用 `ExcludedDowntimeWriteService.writeAfterSave`。历史已复盘行未写排除表时，Grafana 宕机看板（子系统 `cfn36wzatcpa8d`、父系统 `cfsre5mthdr7kc`）仍显示排除状态「未排除」、排除原因 `-`。看板 SQL 的未排除 = 该 SLA 排除窗与宕机无交集：`period_to > down_start` 且 `period_from < down_end`（Unix 秒，`Asia/Shanghai`）。

补写不打 Grafana HTTP（避免 `now-1h` 截断），用目标库 `sla` + `sla_excluded_downtime` 算同一交集。选库仍看 133 字典 `write_jdbc_key`。

## Goals / Non-Goals

**Goals:**

- 手工 POST，每次调用完整扫一轮已复盘行后结束。
- `dryRun` 默认 true 只返回分类清单；false 才对「未排除且能解析 slaid」的行调用现有写入。
- 无 `sla.name` 匹配：跳过并出现在结果里，不使整批失败。

**Non-Goals:**

- cron、改 Grafana SQL、为石家庄等补 SLA 名、改 `events.html`、132 建表。

## Decisions

### 1. 接口

`POST /api/sla/excluded-downtime-backfill`，query `dryRun` 默认 `true`。无请求体。放在现有 `/api/sla` 手工任务旁。不鉴权（与 `review-event-sync` 相同）。

### 2. 候选与未排除

读 133 `sla_review_event` 且 `count_sla` 为「是」或「否」。对每行：字典 `write_jdbc_key` → zabbix6 主库或 zabbix7 池；`SELECT slaid FROM sla WHERE name = system_name`。无 slaid → 记入 `skippedNoSla`。有 slaid 且已有排除行满足 Grafana 交集 → `skippedAlreadyExcluded`。否则视为未排除（等价排除原因为空/`-`）。

### 3. 写入

`dryRun=false` 时对未排除且有 slaid 的行调用现有 `writeAfterSave`（含 `ids`、按开始时刻 upsert）。单行失败记入 `failed`，继续后续行。`dryRun=true` 不调用写入。

### 4. 响应

JSON：`dryRun`、`reviewed`、`wouldWrite`/`written` 条数与明细（id、层级、系统名、开始、结束、countSla、写库）、`skippedAlreadyExcluded` 计数、`skippedNoSla` 列表、`failed` 列表。明细足以对照此前人工核对的约 35 条分类。

## Risks / Trade-offs

- [0 秒宕机写了仍未排除] → 看板公式使然；仍按规则写入。
- [Grafana 用 service_sla_map 对上、name 对不上] → 与保存路径一致，记 skippedNoSla。
- [裸 POST 会改 SLI] → 默认 dry-run；文档写明 false 才写。

## Migration Plan

部署后即可调用。先 dry-run 核对，再 `dryRun=false`。回滚：去掉接口；已写入行留在排除表。

## Open Questions

无。
