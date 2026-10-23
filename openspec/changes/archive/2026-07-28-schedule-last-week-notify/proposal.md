## Why

SLA 复盘推送目前依赖手工调用 API，或依赖已关闭的「每天 9:30」定时任务，无法按业务约定在月末集中提醒。需要在每月最后一周每天自动推送到企微群，平时不打扰。

## What Changes

- 将定时推送策略改为：**每月自然日最后 7 天**（方案 A），每天 **17:00**（东八区）执行一次企微推送。
- 非该窗口内的定时触发一律跳过（打日志即可），**平时不做推送**。
- 保留并沿用现有 `SlaNotifyScheduler` + `notifyNow()` 链路（含责任人 @、精简 markdown、看板指引）。
- 手工 `POST /api/sla/notify` **不受**月末窗口限制，便于联调与补发。
- 配置：`sla.schedule.enabled` 上线默认开启；`cron` 改为每日 17:00；增加「是否处于月末最后 7 天」的门控逻辑（及可选配置项）。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-downtime-notify`: 增加月末最后 7 天每日 17:00 自动推送要求；明确手工 notify 不受该日历门控约束。

## Impact

- 配置：`application.yml` → `sla.schedule.*`
- 代码：`SlaScheduleProperties`、`SlaNotifyScheduler`（及可能的日历门控工具类）
- 运维：进程需持续运行；时区需为 `Asia/Shanghai`（或显式配置）；改配置后重启生效
- API：preview / notify 行为不变（notify 仍可随时手工触发）
