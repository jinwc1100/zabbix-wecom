## Context

`sla-wecom-notifier` 已具备 `SlaNotifyScheduler` + `sla.schedule.enabled/cron`，当前默认 cron 为每天 9:30 且 `enabled: false`。推送链路（Grafana → 匹配 → 责任人 @ → 企微）已可用。业务约定改为：**每月自然日最后 7 天、每天 17:00** 自动推，平时定时不推；手工 API 仍随时可用。

## Goals / Non-Goals

**Goals:**

- 以方案 A（月末自然日最后 7 天）实现日历门控。
- 每日 17:00（`Asia/Shanghai`）触发；窗口外仅跳过、不推送。
- 复用现有 `notifyNow()`，不改变消息内容与 @ 逻辑。
- 配置可开关；手工 `POST /api/sla/notify` 不受门控。

**Non-Goals:**

- 不实现日历周（方案 B）或「仅工作日」。
- 不引入分布式锁 / 多实例去重。
- 不改 Grafana 查询、markdown 格式、责任人规则。
- 不做失败自动重试队列（失败打日志即可；可手工补发）。

## Decisions

### 1. Cron 每天跑 + 代码门控（而非复杂 cron 表达式）

- **选择**：`cron: "0 0 17 * * ?"`，在 `scheduledNotify()` 内判断 `isInLastSevenDaysOfMonth(today)`。
- **理由**：Spring 6 字段 cron 无法干净表达「每月最后 7 天」；门控可读、可单测。
- **备选**：Quartz `L` 语法 — 引入成本高，且仍难表达「最后 7 天」。

### 2. 最后 7 天定义

```
dayOfMonth >= lengthOfMonth(year, month) - 6
```

例：31 天月 → 25–31；30 天月 → 24–30；2 月非闰 → 22–28。含周末。

### 3. 时区

- **选择**：调度与门控统一用 `Asia/Shanghai`（可通过 `sla.schedule.zone` 配置；未配则默认该值，或依赖 JVM 默认时区并在 README 要求东八区）。
- **理由**：业务明确「下午 17 点」为中国时间。

### 4. 配置形态

```yaml
sla:
  schedule:
    enabled: true
    cron: "0 0 17 * * ?"
    zone: Asia/Shanghai          # 可选
    last-days-of-month: 7        # 可选，默认 7，便于以后调窗口
```

- `SlaScheduleProperties` 增加 `lastDaysOfMonth`（及可选 `zone`）。
- `@Scheduled` 若需 zone：Spring 支持 `zone = "${sla.schedule.zone:Asia/Shanghai}"`。

### 5. 门控放置位置

- **选择**：放在 `SlaNotifyScheduler`（或抽出 `LastDaysOfMonthGate` 工具供 scheduler 调用），**不要**放进 `SlaReviewNotifyService.notifyNow()`。
- **理由**：保证手工 notify 不受影响。

## Risks / Trade-offs

- **[Risk] JVM 时区非东八区导致 17:00 偏移** → Mitigation：`@Scheduled(zone=...)` + README 说明。
- **[Risk] 进程在 17:00 未运行则漏推** → Mitigation：运维保障常驻；漏推用手工 API。
- **[Risk] 多实例重复推** → Mitigation：当前单机部署；文档注明。
- **[Trade-off] 每月固定约 7 次（含周末）可能偏吵** → 已按业务「每天」确认；若以后要工作日再改门控。

## Migration Plan

1. 改配置与门控代码，打包重启。
2. 用手工 `POST /api/sla/notify` 验证消息内容仍正常。
3. 可用临时把 `last-days-of-month` 调大或单测覆盖边界日；或临时改系统日期（仅测试环境）。
4. 回滚：`sla.schedule.enabled: false` 即可停止自动推。

## Open Questions

- （无）方案 A、含周末、17:00、手工不受限已确认。
