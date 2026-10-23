## Context

`sla-wecom-notifier` 已实现低 SLI 关联宕机、近窗过滤与完整 Markdown。推送企微时因含宕机明细而超长分片。业务要求：**群消息只要名单三字段；完整明细仍可通过 preview 查看。**

## Goals / Non-Goals

**Goals:**

- `notify` 推送精简 Markdown：系统名称、SLA值（SLI值）、子系统/父系统。
- `preview` 继续返回完整 `markdown`（含近窗宕机与备注）及完整 `items`。
- 查询/匹配管线不变。

**Non-Goals:**

- 不删除宕机查询能力。
- 不改 Grafana 看板或阈值逻辑。
- 不强制去掉分片逻辑（精简后多半用不到，可保留兜底）。

## Decisions

### D1. 双 Markdown 字段

- `NotifyResult.markdown`：完整版，供 `preview`。
- `NotifyResult.wecomMarkdown`：三字段精简版，供 `notify` → WeCom。
- 备选：preview 也改成精简——**否决**（与「preview 还是保留」冲突）。

### D2. 精简版版式

按父系统/子系统分组，每行：

`1. **系统名称** — SLI值 — 父系统|子系统`

页头保留阈值、系统数、父/子计数；**不出现**宕机起止与历史备注。

### D3. Formatter API

`SlaMarkdownFormatter` 增加：

- `formatDetail(NotifyResult)` → 现有完整文案
- `formatWecomSummary(NotifyResult)` → 三字段文案

`buildReport()` 同时填充两个字段；`notifyNow()` 只 `sendMarkdown(wecomMarkdown)`。

## Risks / Trade-offs

- [Risk] 调用方误把 `preview.markdown` 当推送内容 → Mitigation：README 写明两字段差异；响应里同时返回二者便于对照。
- [Trade-off] 群里看不到宕机时间 → 需复盘时再调 preview / Grafana。

## Migration Plan

1. 实现双 Markdown 后重启，先 `preview` 确认 `markdown` 仍完整、`wecomMarkdown` 仅三字段。
2. 再 `notify` 验证企微单条可读。
3. 回滚：恢复仅推送完整 `markdown`。

## Open Questions

- 无（按用户已确认：推送精简、preview 保留明细）。
