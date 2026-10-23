## Context

现有 `OwnerRuleResolver`（地区 + TRADE/BID + 交易地区兜底）与企微精简三字段已上线。`doc/list.txt` 已更新；复盘 Excel（`doc/SLA复盘-2026-07.xlsx`）中 16 个「待指定责任人」系统集中在成都开评标、陕西本级、平潭交易/开评标、西藏工程交易——与规则缺口一一对应。`SlaLowSliItem.panelName` 已是「新版监控/老版监控」，但未透传到企微文案。

## Goals / Non-Goals

**Goals:**

- 配置对齐 list.txt + 拍板：陕西/成都/平潭补齐；西藏工程交易→雷志；王华聪雄安交易须含「工程交易」；雄安门户/监督等非开评标仍兜底王华聪。
- 企微精简清单增加「监控系统」字段。
- 重导复盘 Excel 验收原待指定系统。

**Non-Goals:**

- 不改 preview 明细 Markdown 字段集。
- 不改复盘 Excel 列结构 / API。
- 不改定时窗、双 SLI 源、宕机匹配逻辑（除透传 panelName）。

## Decisions

### 1. 责任人配置增量（优先纯配置）

| 调整 | 做法 |
|------|------|
| 卓松林 TRADE | regions 增加 `陕西` / `陕西省本级`（覆盖「陕西省本级工程交易系统」） |
| 刘彩霞 BID | regions 增加 `陕西`/`陕西省本级`、`成都`；保留/统一 `雄安` BID |
| 张宇 TRADE | regions 增加 `平潭` |
| 雷志 | regions 增加 `平潭`（开评标靠 BID 类型）及 `西藏工程交易`；保留广州/江门/西藏一张网 |
| 王华聪雄安交易 | 去掉宽 `雄安` + TRADE；改为 region 关键词 **`雄安工程交易`**（须名称同时含该子串） |
| 雄安门户/监督兜底 | 增加/保留 **types 为空**、region=`雄安`、owner=王华聪（仅 NONE 类型走 region-only / 兜底池），priority 低于刘彩霞 BID |
| 石家庄工程开评标 | 保持王华聪高优先级 BID 规则 |

**Alternatives considered:** 扩展 resolver 支持 `requiredTokens: [工程交易]` —— 仅当 region 关键词无法表达时再改代码；当前用 `雄安工程交易` 子串即可。

### 2. 「监控系统」数据流

```
SlaLowSliItem.panelName
  → DowntimeMatchService.baseItem 写入 DowntimeNotifyItem.monitorSystem（或复用 panelName）
  → SlaMarkdownFormatter.formatWecomSummary 追加「 — {监控系统}」
```

Detail markdown / Excel **不读**该字段。取值必须为配置面板名 `新版监控` / `老版监控`（与 `grafana.*.panels[].name` 一致）。

### 3. 验收方式

1. 更新配置（必要时微小代码）并重启。  
2. `GET /api/sla/review-excel?month=2026-07` 覆盖或另存验收文件。  
3. 对照原 16 系统：成都→刘彩霞、陕西开评标→刘彩霞、陕西交易→卓松林、平潭交易→张宇、平潭开评标→雷志、西藏工程交易→雷志。  
4. `GET /api/sla/preview` 查看 `wecomMarkdown` 是否含监控系统。

## Risks / Trade-offs

- **[Risk] 雄安宽 TRADE 删除后，名称含「雄安+交易」但不含「工程交易」的系统可能变待指定** → Mitigation：按 list 接受；若现网有此类名再补规则。
- **[Risk] region=`西藏` 过宽会与机器管抢雷志** → Mitigation：用更长关键词 `西藏工程交易` / `西藏一张网`，机器管规则保持更高或同等可区分。
- **[Risk] 平潭 types 拆分依赖 detectType** → Mitigation：开标/开评标→BID→雷志；交易→TRADE→张宇；无类型字样再看兜底。
- **[Trade-off] Excel 不展示监控系统** → 按产品确认，仅企微需要。

## Migration Plan

1. 改 `owners.rules` + 透传/格式化代码。  
2. 重建 jar、重启 8086。  
3. 重导 Excel + preview 抽查。  
4. 回滚：还原 yml 规则与 formatter 即可。

## Open Questions

- （无）拍板已收口。
