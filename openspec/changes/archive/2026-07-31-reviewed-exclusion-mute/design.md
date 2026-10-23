## Context

推送链路已具备双 SLI 源、宕机匹配、企微精简文案与月末定时。`DowntimeEvent` 已加载「排除状态」（复盘 Excel 用）；缺「排除原因」与推送侧复盘过滤。业务确认：未复盘 = 状态为「未排除」或原因为「-」；当月全复盘才不推；无当月宕机仍推；preview 不过滤。

## Goals / Non-Goals

**Goals:**

- 加载排除原因；按自然月判定已复盘。
- notify/定时跳过「当月宕机全已复盘」的低 SLI 系统。
- preview 保留全量，并暴露是否因已复盘而不推送。

**Non-Goals:**

- 不改复盘 Excel「未排除/-」导出规则。
- 不做独立静默名单 API。
- 不改 SLI 阈值、双源、责任人规则。

## Decisions

### 1. 单条判定

```
未复盘 ⟺ trim(排除状态) == "未排除"  OR  trim(排除原因) == "-"
已复盘 ⟺ 非未复盘
```

空/null 原因：不视为「-」，故若状态也不是「未排除」则算已复盘；若状态缺失且原因空——保守当作未复盘（实现时：原因 null/空 **不** 等于 `-`，但状态 null/空 **不** 等于「未排除」→ 会落成已复盘）。为避免误静默，约定：**状态或原因无法解析时视为未复盘**（缺字段则推送）。

### 2. 系统级聚合（当月）

- 「当月」= `Asia/Shanghai` 下宕机 `startTime` ∈ `[本月1日 00:00, 下月1日 00:00)`。
- 与 `recent-window-days` 无关。
- 当月事件集非空且每条已复盘 → `monthFullyReviewed = true` → **notify 剔除**。
- 当月事件集为空 → **仍推送**。
- 当月存在任一条未复盘 → **仍推送**。

### 3. 数据流

```
load downtime (+ exclusionStatus, exclusionReason)
  → match（近窗明细逻辑不变）
  → 同时按自然月评估 monthFullyReviewed
  → preview：全量 items + 标记
  → notify：filter(!monthFullyReviewed) 再发企微/@
```

`DowntimeNotifyItem` 增加如 `monthFullyReviewed` / `skipWecomNotify` 布尔字段。

### 4. 配置

- `exclusion-column` 已有；新增 `reason-column: 排除原因`（leaf/parent）。

### 5. Preview 标注

- detail / wecomMarkdown 中 preview 用的清单可对已复盘系统加简短标记（如「本月已复盘」）；**不从列表删除**。
- 实际 webhook 仅在 notify 路径过滤。

**Alternatives considered:** 本地静默名单 — 与看板不同步，本变更不做。

## Risks / Trade-offs

- **[Risk] 运维把原因留作 `-` 但状态已部分排除** → 按规则仍算未复盘，继续推（符合「有一个条件满足」）。
- **[Risk] 状态 `-` 且原因有文案** → 不算「未排除」、原因≠`-` → 已复盘；若本意未处理可能误静默 → Mitigation：文档说明；缺字段保守未复盘。
- **[Risk] 跨月边界** → 用 zone 自然月，与「下月再推」一致。

## Migration Plan

1. 加 reason 字段与配置。  
2. 实现月度复盘判定与 notify 过滤。  
3. preview 抽查标记；notify 抽查过滤。  
4. 回滚：去掉过滤即可恢复全量推送。

## Open Questions

- （无）业务四点已确认。
