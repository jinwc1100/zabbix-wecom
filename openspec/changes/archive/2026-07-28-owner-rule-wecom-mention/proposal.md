## Why

低 SLI 复盘推送需要触达真实责任人。现有 `owners.entries` 只能整名精确匹配，无法覆盖 `doc/list.txt` 中「地区 + 交易/开评标」的责任划分；企微侧也未按系统动态 `@`。需要按名单规则解析责任人，并在推送时用手机号 `@`。

## What Changes

- 引入基于 `doc/list.txt` 的**规则匹配**（地区优先，再匹配交易/开评标；规则未写类型则以地区命中）。
- **特指/细化约定**：
  - 雄安**开评标**类系统 → 刘彩霞（15221477921）
  - 雄安**交易**类系统 → 王华聪（13521207069）
  - 其他规则与名单一致（含石家庄工程开评标、各地区交易/开评标拆分等）。
- `notify` 推送精简 Markdown 后，追加一条 text，`mentioned_mobile_list` 为本次命中责任人手机号（去重）。
- `preview` 可展示解析到的责任人/手机，便于核对（不发送 `@`）。
- 未匹配规则的系统：仍出现在清单中，不强制 `@`（可用默认责任人展示文案）。

## Capabilities

### New Capabilities

- `owner-rule-mention`: 按地区+业务类型规则解析责任人，并在企微推送中 `@` 对应手机号。

### Modified Capabilities

- `sla-downtime-notify`: 通知结果携带责任人信息；企微 notify 在摘要 Markdown 之外增加按匹配结果的 `@` 提醒。

## Impact

- 代码：规则配置/解析器、匹配服务、`NotifyResult`/items 扩展、`WeComWebhookClient` 动态 `mentioned_mobile_list`。
- 配置：将 `list.txt` 落地为 YAML 规则（或启动时加载）；弱化整名 entries 的主导地位。
- 企微：每次 notify 可能多一条 text @ 消息；依赖群成员手机号与企微账号绑定。
