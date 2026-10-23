## Why

月末 SLI 自动推送已去掉；值班仍需要一份可协作的「待复盘宕机清单」，并 @ 责任人。手工从 Grafana 抄到腾讯文档易漏、难复用。需要每周五固定产出在线复盘表，窗口从 2026-07-30 起累计到触发时刻。

## What Changes

- 新增独立管线（与现有年度 SLI `POST /api/sla/notify` 分开）：查询子/父宕机明细 → 匹配责任人 → 填本地 xlsx 模版 → 腾讯文档开放 API 导入 → 企微通知含文档地址并 @ 责任人。
- 定时：每周五 11:00（`Asia/Shanghai`），带开关；关闭后不跑定时，不影响手工补跑。
- 手工：新增独立 `POST`（不复用 `/api/sla/notify`）便于联调。
- 数据：宕机开始 ∈ `[2026-07-30 00:00, 触发时刻]`；排除状态仅 `未排除` 或 `-`（与 review-excel 相同）；责任人用现有 `OwnerRuleResolver`，**不改**匹配规则。
- 模版：`sla-wecom-notifier/doc/复盘明细.xlsx`；写入「父/子系统、系统名称、宕机开始时间、宕机结束时间、责任人」。
- 在线文件名：`复盘明细-yyyyMMdd`（日期无横线，如 `复盘明细-20260821`）。
- 上传走腾讯文档**开放 API**（进程内），不依赖 Cursor MCP。

## Capabilities

### New Capabilities

- `weekly-review-tencent-doc`: 周五（或手工 POST）按时间窗拉子/父未排除宕机、填模版、导入腾讯在线表格、企微推送文档链接并 @ 责任人。

### Modified Capabilities

- `sla-downtime-notify`: 将「无定时 SLA 企微推送」收窄为**仅约束年度 SLI notify 管线**；允许本变更的周五复盘文档管线定时/手工发企微。`POST /api/sla/notify` 行为不变。

## Impact

- 代码：`sla-wecom-notifier` 新调度器、查询过滤、xlsx 填模版、腾讯文档客户端、新 HTTP 入口、企微文案；恢复 `@EnableScheduling`（仅服务本任务）。
- 配置：任务开关/cron、时间窗起点、腾讯文档开放 API 凭证、模版路径。
- 依赖：腾讯文档开放 API；Apache POI（已有）。
- 资源：需将 `doc/复盘明细.xlsx` 纳入仓库（当前缺失）。
- 不动：`OwnerRuleResolver` 规则、`/api/sla/notify` 精简清单与已复盘跳过逻辑、review-excel 现有导出格式。
