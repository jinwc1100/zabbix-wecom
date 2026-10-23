## Why

责任人花名册（`doc/list.txt`）已更新（陕西、成都、平潭、西藏工程交易等），且王华聪对雄安交易的匹配需收窄为必须含「工程交易」；同时企微复盘精简清单缺少「监控系统」（新版/老版）字段，不便对照 Grafana 面板。需要同步规则、补推送字段，并用重导复盘 Excel 验收原「待指定」系统是否能挂上人。

## What Changes

- 按 `doc/list.txt` 与已确认拍板更新 `owners.rules`（补陕西/成都/平潭等；西藏工程交易→雷志；王华聪雄安交易须含「工程交易」；雄安门户/监督等非开评标仍可通过地区兜底到王华聪）。
- 企微精简 Markdown（`wecomMarkdown`）每条增加字段「监控系统」，取值为该系统所属年度 SLI 面板名：`新版监控` 或 `老版监控`。
- **不**改 preview 明细结构、**不**改复盘 Excel 列（仍用现有六列）；验收时在规则生效后重导 `GET /api/sla/review-excel`，对照原 `doc/SLA复盘-2026-07.xlsx` 中「待指定责任人」系统是否已匹配。
- 同步修正 `owner-rule-mention` 中与「雄安任意交易→王华聪」不符的既有要求。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `owner-rule-mention`: 更新地区/类型规则覆盖范围；收窄雄安王华聪交易匹配；明确西藏工程交易归属雷志；保留雄安非开评标地区兜底到王华聪。
- `sla-downtime-notify`: 企微精简清单增加「监控系统」字段（来自 SLI 面板名）。

## Impact

- 配置：`application.yml` → `owners.rules`；可能微调 `OwnerRuleResolver`（若「雄安+工程交易」无法仅靠 region 关键词表达）。
- 代码：`DowntimeNotifyItem` / `DowntimeMatchService` 透传 `panelName`；`SlaMarkdownFormatter.formatWecomSummary` 输出监控系统；detail/Excel 不变。
- 验收：重启服务后重导复盘 Excel，核对成都/陕西/平潭/西藏工程交易等原待指定系统。
- 无新依赖；无 API 路径变更。
