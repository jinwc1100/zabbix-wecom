## Why

企微群机器人 Markdown 有长度限制，当前推送含近窗宕机明细时需拆成多段，阅读成本高。复盘通知在群里优先只需知道「哪些系统不达标、SLI 多少、父/子系统」，宕机时间明细留在预览接口即可。

## What Changes

- 企微推送（`/api/sla/notify`）使用的 Markdown **仅包含三字段**：系统名称、SLA值（SLI值）、子系统/父系统。
- `/api/sla/preview` **仍保留完整明细**：含近窗宕机起止、历史备注、以及现有结构化 `items`（不因推送精简而删减）。
- 预览与推送可使用两套 Markdown（或同一结果对象上区分 `markdown` / `wecomMarkdown`），避免互相覆盖。
- 不改变低 SLI 过滤、父/子匹配、近窗过滤等查询逻辑。

## Capabilities

### New Capabilities

- （无新能力域；本变更为现有通知展示裁剪）

### Modified Capabilities

- `sla-downtime-notify`: 企微推送 Markdown 改为三字段精简版；预览接口继续输出完整明细 Markdown/JSON。

## Impact

- 代码：`SlaMarkdownFormatter`、`SlaReviewNotifyService`、`NotifyResult`（可能新增 `wecomMarkdown` 字段）。
- 企微：消息显著变短，通常单条即可发送。
- API：`preview` 行为对调用方保持完整信息；`notify` 推送内容变短。
