## Context

匹配仍是：地区 `contains` + TRADE/BID/`types` 空；同池取 `priority` 最高。当前王立新 `regions` 含 `北京`，会吞掉所有带「北京」的区县系统。`list.txt` 已把北京拆开，并增加张洪涛。

## Goals / Non-Goals

**Goals:**

- 配置与 `doc/list.txt` 一致。
- 含「北京」的交易系统默认王华聪；「北京综合交易」「北京工程建设」仍王立新。
- 密云/门头沟开评标 → 刘彩霞；亦庄开评标 → 胡一奇。
- 音视频 → 张洪涛。

**Non-Goals:**

- 不改解析算法。
- 不批量更新 `sla_review_event.owner_name`。
- 不改催办文案或其它能力。

## Decisions

1. **王立新必须带 `types: ["TRADE"]`，且 priority 高于「北京」交易规则**  
   若王立新是 `types: []`，则 `北京综合交易平台` 会先走 typedHits 里的王华聪（`北京`+TRADE），王立新进不了 typedHits。专名 TRADE + 更高 priority 才能压过区县交易。

2. **王华聪「北京」只配 TRADE，禁止 `types: []`**  
   否则门户/音视频/工程建设无类型名也会被「北京」兜底错配。区县无「北京」二字的交易名，另加 `亦庄`、`门头沟`、`密云` 的 TRADE 关键字。

3. **开评标用现有 BID 规则加地区词**  
   刘彩霞 BID：`密云`、`门头沟`。胡一奇 BID：`亦庄`。typedHits 优先于王华聪 TRADE。

4. **张洪涛**  
   `regions: ["音视频"]`，`types: []`，priority 约 60，避免被更宽的 TRADE 兜底抢走（名称同时含交易和音视频时再按 priority 取舍；默认音视频专名更高）。

5. **建议 priority**  
   王立新专名 TRADE 70；张洪涛 60；王华聪 `北京`/区县 TRADE 50（与现有 TRADE 档一致）；BID 刘/胡维持 50，雄安等更高规则不动。

## Risks / Trade-offs

- [名称含「北京」但不是区县交易] → 王立新只认两个专名；其它带北京的交易都归王华聪（已确认）。
- [「北京工程建设开评标」] → 无 BID 专名时走 BID typedHits 可能未命中，再 region-only/兜底；王立新是 TRADE 不会在 BID typedHits 中。若真实无此名可忽略。
- [旧复盘行仍是王立新] → 催办按库内姓名；需改名另做。

## Migration Plan

改 yml 后重启。回滚还原 `owners.rules`。

## Open Questions

无。
