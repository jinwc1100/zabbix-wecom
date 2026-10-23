## Why

手工 `POST /api/sla/review-event-sync` 只写库不催人，漏跑 17:30 时补同步无法提醒责任人。催办只发一次，群里容易被刷掉。复盘页保存后保存按钮变灰，责任人误以为不能改已填内容。

## What Changes

- **BREAKING**（催办行为）：`POST /api/sla/review-event-sync` 在同步**成功后**必须走与 17:30 相同的未复盘企微催办。开关仍只约束定时，不约束该 POST。同步失败则不催。
- 未复盘催办（定时与手工）对同一文案**连续发送 3 次**；每条正文以「重要的内容说3遍」开头，仍只汇总条数并 `@`，不列系统。条数为 0 仍不发。
- 复盘页：已保存行（`count_sla` 非空）人填字段只读，提供「编辑」解锁后再保存；`PUT` 仍允许覆盖六字段。机器列只读。Demo 与 `/review/` 两页同步。
- 不改 `POST /api/sla/notify`、Grafana 采集、INSERT IGNORE、责任人规则。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `daily-sla-review-event-sync`: 手工 POST 成功后必须触发未复盘催办；失败不催。
- `review-incomplete-wecom-nudge`: 催办发送 3 次；首句固定「重要的内容说3遍」；手工与定时共用。
- `sla-review-event-web`: 已保存行只读 + 编辑后再保存；仍逐行 PUT。

## Impact

- `SlaNotifyController` 的 review-event-sync：sync 成功后调用催办。
- `ReviewIncompleteNudgeService` / `WeComWebhookClient`：三连发与文案前缀。
- `static/review/index.html` 与 `review-web-demo/index.html`：编辑态。
- 单测覆盖手工路径催办、三连发、N=0、已保存行编辑。
