## Why

月末最后 7 天的自动企微推送已不再需要；复盘改为按需手工触发。继续保留日历门控的 cron，会在窗口内重复打扰群，也增加误推风险。

## What Changes

- 移除每天 10:00 / 17:00、且仅在月末最后 7 个自然日触发的 SLA 定时推送（方案 A：拆掉整段自动推送，而不是只放宽窗口）。
- 手工 `POST /api/sla/notify` 与 `GET /api/sla/preview` 保持可用，行为不变。
- **不改**责任人匹配（地区/交易/开评标规则）与企微 `@` 手机号逻辑。
- 删除调度专用配置与日历门控实现（`sla.schedule.*`、`LastDaysOfMonthGate`、调度器）。
- **BREAKING**（运维）：进程启动后不再自动推送；原先依赖月末窗口的值班流程需改为手工调用 API。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-downtime-notify`: 删除「仅在月末最后 7 个自然日自动推送」及与之绑定的日历窗口约束；明确系统不再运行 SLA 定时任务。手工 notify 不受日历窗口限制的语义保留（因已无窗口，改为：手工 notify 始终可用）。责任人匹配与 `@` 要求不变。

## Impact

- 代码：`SlaNotifyScheduler`、`LastDaysOfMonthGate` 及其测试、`SlaScheduleProperties` 的调度字段、`application.yml` 中 `sla.schedule`。
- 应用：可关闭 Spring `@EnableScheduling`（若无其它定时任务）。
- API：`POST /api/sla/notify`、`GET /api/sla/preview` 不变。
- 不动：`OwnerRuleResolver`、`WeComWebhookClient` 的 mention 路径、`SlaMarkdownFormatter` 责任人文案、复盘排除/静音逻辑。
