## Why

企微群机器人地址已更换，需同步配置以免推送到旧群。复盘精简清单未说明宕机明细应去哪查看，责任人收到 @ 后仍可能找不到「SLA宕机明细 / SLA父系统宕机明细」看板。

## What Changes

- 将 `wecom.webhook-url` 更新为新地址：`https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=3b8b6e5a-4d3f-43b1-94a9-7565f9db6ad7`。
- 在复盘提醒 Markdown 文末补充指引：
  - 子系统宕机明细查看「SLA宕机明细」
  - 父系统宕机明细查看「SLA父系统宕机明细」
- 上述指引同时出现在 **preview 完整 `markdown`** 与 **企微精简 `wecomMarkdown`**，保持口径一致。
- 本变更不附带 Grafana URL（仅看板中文名）；若后续需要可配置化链接。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `sla-downtime-notify`: 更新企微 webhook；复盘 Markdown 增加父/子系统宕机明细查看指引。

## Impact

- 配置：`application.yml` → `wecom.webhook-url`
- 代码：`SlaMarkdownFormatter` 文末追加指引（detail + summary）
- 运维：改配置后需重启服务；用 `POST /api/sla/notify` 验证新群收到消息
