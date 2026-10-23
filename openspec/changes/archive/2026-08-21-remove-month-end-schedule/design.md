## Context

`sla-wecom-notifier` 通过 `SlaNotifyScheduler` 在 `Asia/Shanghai` 每天 10:00 / 17:00 触发；`LastDaysOfMonthGate` 仅在当月最后 7 个自然日调用 `SlaReviewNotifyService.notifyNow()`。同一推送管线也可由 `POST /api/sla/notify` 随时触发。责任人解析与企微 `@` 发生在 `notifyNow()` 内部，不依赖调度器。

业务已确认方案 A：去掉整段自动推送，只保留手工入口。

## Goals / Non-Goals

**Goals:**

- 进程运行期间不再因 cron / 日历窗口自动调用 notify 管线或打企微 webhook。
- 手工 notify / preview 行为与责任人匹配、`@` 逻辑保持不变。
- 删除调度专用配置与代码，避免留下「关了但还在跑」的死开关。

**Non-Goals:**

- 不改为「每天都推」或换 cron（那是方案 B）。
- 不改 `OwnerRuleResolver`、`WeComWebhookClient` mention、Markdown 责任人文案。
- 不改 Grafana 拉取、宕机匹配、本月已复盘跳过推送。
- 不新增替代调度（企业微信定时、外部 cron）作为本变更的一部分。

## Decisions

### 删除调度器与日历门控，而不是 `enabled: false`

配置关闭仍会留下 `@Scheduled` 与门控代码，下次改配置就会复活自动推送。方案 A 要求「没有这段任务」，因此删除：

- `SlaNotifyScheduler`
- `LastDaysOfMonthGate` 及单元测试
- `SlaScheduleProperties` 整类（目前仅服务调度）
- `application.yml` 的 `sla.schedule`
- `@EnableScheduling`（仓库内仅此一处 `@Scheduled`）
- 启动类上对 `SlaScheduleProperties` 的 `@EnableConfigurationProperties` 注册

`notifyNow()` 仍由 HTTP 控制器调用，调度器只是其中一个调用方。

### 不抽「共用触发接口」给外部调度

若以后用系统 crontab 打 HTTP，现有 `POST /api/sla/notify` 已足够。本变更不引入新的内部 job SPI。

### 测试范围

删除 `LastDaysOfMonthGateTest`。不为「某时刻不触发」写脆弱的时钟测试；靠代码审查确认无 `@Scheduled`。保留现有 `OwnerRuleResolverTest` 等责任人测试，本变更不改它们。

## Risks / Trade-offs

- [遗忘月末提醒] → 运维/值班改为需要时调 `POST /api/sla/notify`；文档与 spec 写明无自动推送。
- [部署后仍自动推] → 确认已发布的包不含调度器；回滚即恢复旧包（见迁移）。
- [误改责任人路径] → 实现时只动 `schedule` / `SlaScheduleProperties` / 启动注解 / yml；PR 审查禁止改 `OwnerRuleResolver` 与 webhook mention。

## Migration Plan

1. 发布含删除调度的版本；旧进程停掉后月末窗口不再推。
2. 需要推送时调用 `POST /api/sla/notify`（与现网手工补发相同）。
3. 环境变量/配置里残留的 `sla.schedule.*` 可忽略（类已删除，无绑定则无效果）。
4. 回滚：重新部署上一版本即可恢复 10:00/17:00 + 最后 7 天门控。

## Open Questions

- 无。方案 A 与「不改匹配/@」已在 explore 中确认。
