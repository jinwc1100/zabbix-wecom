## Context

`OwnerRuleResolver` 先地区、再 TRADE/BID；带 `types` 的规则在类型为 NONE 时直接跳过。雄安门户/监督等无「交易」「开评标」字样 → NONE → 无人匹配 → 不 @。业务要求：开评标仍走现规；非开评标或匹配失败时按地区兜底到交易责任人（雄安 → 王华聪）。

## Goals / Non-Goals

**Goals:**

- 增加交易/非开评标地区兜底，且兜底不要求名称含「交易」。
- 已成功匹配的 BID 结果不被兜底覆盖。
- `雄安门户网站`、`雄安监督平台` → 王华聪。
- 尽量复用现有 `owners.rules`，少加新配置结构。

**Non-Goals:**

- 不改 Grafana / 双 SLI 源 / markdown / 定时。
- 不按责任人拆分多群推送。
- 不重写整份 list.txt 匹配引擎。

## Decisions

### 1. 解析顺序

```
1) detectType(name)
2) primaryResolve（现有 typedHits → regionOnlyHits → 最高 priority）
3) if (type == BID && matched) return primary          // 开评标成功则结束
4) if (matched && type != BID) return primary          // 非开评标已命中（如显式 TRADE）也结束
5) if (!matched || type != BID) tradeRegionFallback() // 未匹配，或（可选）NONE 强化兜底
6) else unmatched
```

更贴需求的简化版：

```
primary = existing resolve
if primary.matched:
  if type == BID: return primary
  if type == TRADE or region-only hit: return primary
fallback = best rule among:
  - types contains TRADE, region hit（忽略系统实际 type）
  - types empty, region hit（北京/费方亮/雷志等地区兜底）
if fallback: return fallback
if primary.matched: return primary
return unmatched
```

**推荐落地（清晰）：**

1. 跑现有 primary。  
2. 若 `type == BID` 且 primary 命中 → 返回 primary（刘彩霞等）。  
3. 若 primary 命中且非「仅因误伤」→ 对 TRADE/region-only 命中直接返回。  
4. 若 primary **未命中**，或 `type == NONE`：执行 `fallbackByRegionToTradeOwner`（在 TRADE 规则 + `types` 为空规则中按 region contains + priority 选最优）。  
5. 仍无 → unmatched。

这样 `雄安门户`（NONE、primary 失败）→ 命中王华聪 `regions:[雄安] types:[TRADE]` 兜底。

### 2. 兜底候选池

- 包含：`types` 含 `TRADE` 的规则；`types` 为空的规则。  
- 排除：仅 `types: [BID]` 的规则（避免兜底成刘彩霞）。  
- 按 `priority` 降序；同 priority 保持配置顺序稳定。

### 3. 配置

- 雄安交易兜底继续用现有王华聪规则 `regions: ["雄安"]` + `TRADE`（不必改成必须「工程交易」才能兜底）。  
- README / list.txt 注明：「非开评标或匹配失败时，按地区交易责任人兜底」。

### 4. 单测

- `雄安门户网站` → 王华聪  
- `雄安监督平台` → 王华聪  
- `雄安某某开评标` → 刘彩霞  
- 无地区命中 → unmatched  

## Risks / Trade-offs

- **[Risk] 多地区关键词同时命中（如名称含海南又含别的）** → Mitigation：沿用 priority；必要时加更长关键词优先（现有已靠 priority）。  
- **[Risk] BID 未匹配（地区不在 BID 规则）却被 TRADE 兜底** → 符合「匹配不到则交易兜底」；若业务不要，需另议。当前按用户第 2 条允许。  
- **[Trade-off] types 为空规则进入兜底池** → 北京非开评标等更合理；开评标专用不会进入（它们带 BID）。

## Migration Plan

1. 改 `OwnerRuleResolver` + 单测。  
2. 更新 README。  
3. preview 抽查雄安门户/监督 owner。  
4. 回滚：去掉 fallback 分支即可。

## Open Questions

- （无）验收用例已确认。
