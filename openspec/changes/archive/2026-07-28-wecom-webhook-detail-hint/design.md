## Context

通知已支持精简企微文案与责任人 @。用户提供新 webhook，并要求在总结复盘提醒中标明：子系统看「SLA宕机明细」，父系统看「SLA父系统宕机明细」。

## Goals / Non-Goals

**Goals:**

- 切换企微推送地址到新 key。
- preview 与 wecom 两套 Markdown 均在文末增加明细查看指引。

**Non-Goals:**

- 不在文案中硬编码 Grafana 完整 URL（除非后续另提需求）。
- 不改变 SLI 过滤、责任人匹配、近窗逻辑。

## Decisions

### D1. Webhook

直接替换 `wecom.webhook-url` 为新地址；`enabled` 保持 true。

### D2. 指引文案（固定中文）

在 `formatDetail` / `formatWecomSummary` 结尾「请对应责任人…」之后追加：

```text
- 子系统宕机明细请查看：**SLA宕机明细**
- 父系统宕机明细请查看：**SLA父系统宕机明细**
```

可抽成 formatter 私有方法 `appendDetailHints(sb)` 避免重复。

### D3. 配置化（可选，本变更不做）

看板名称暂写死在代码；若需常改再迁到 `application.yml`。

## Risks / Trade-offs

- [Risk] 新 webhook 无权限或 key 错误 → Mitigation：notify 后看返回与群消息；失败会抛企微 errcode。
- [Trade-off] 无 URL：需用户自行在 Grafana 打开同名看板。

## Migration Plan

1. 改配置与文案 → 重启 → preview 看文末 → notify 验证新群。  
2. 回滚：还原旧 webhook 与去掉指引行。

## Open Questions

- 无（按探索结论：两处 Markdown 都加指引，不附 URL）。
