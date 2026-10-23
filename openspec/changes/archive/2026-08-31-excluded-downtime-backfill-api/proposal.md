## Why

复盘保存才会写 `sla_excluded_downtime`。更早已复盘的行在 Grafana 宕机看板上仍是「未排除」（排除原因为 `-`），需要一次手工补写，而不是改定时任务。

## What Changes

- 新增手工 POST：对 `sla_review_event` 中 `count_sla` 为「是」或「否」的行，在目标库按 Grafana 同款时间交集判断是否未排除；未排除则复用现有 `writeAfterSave`。
- 每次调用只跑一轮，不注册 cron。
- 目标库没有 `sla.name = system_name` 的行跳过并列入失败/跳过清单，不中断整批。
- `dryRun` 默认 true：只返回清单（写入/将跳过/无 sla 名），不改排除表；`dryRun=false` 才真正写入。
- 不按 Grafana `now-1h` 截断；时间范围覆盖全部已复盘行。不改保存路径、不改 132 DDL。

## Capabilities

### New Capabilities

- `review-excluded-downtime-backfill`: 手工 dry-run/执行补写未排除的已复盘行。

### Modified Capabilities

- （无；写入规则仍由 `review-excluded-downtime-write` 承担）

## Impact

- 新 Controller 方法、补写服务、单测。会改变 Grafana SLI（仅 `dryRun=false`）。
