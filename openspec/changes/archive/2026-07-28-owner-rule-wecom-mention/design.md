## Context

`doc/list.txt` 定义了按地区与「交易/开评标」划分的责任人及手机号。用户确认匹配顺序：先地区，再类型；规则未写类型则以地区为主。额外约定：雄安开评标→刘彩霞，雄安交易→王华聪。企微 markdown 无法可靠 @，需用 text + `mentioned_mobile_list`。

## Goals / Non-Goals

**Goals:**

- 将名单规则化为可配置规则并匹配每个低 SLI 系统。
- notify 时 @ 所有命中责任人手机（去重）。
- preview 能看到匹配结果，不发送 @。

**Non-Goals:**

- 不改 Grafana 查询与近窗过滤逻辑。
- 不实现责任人 Web 维护界面。
- 不依赖企微通讯录 API（仅用群机器人 webhook）。

## Decisions

### D1. 规则模型

每条规则：`regions[]`、`types[]`（空=不限）、`name`、`mobile`、可选 `priority`（特指更高）。

从 `list.txt` 映射，并显式增加/调整：

- 雄安 + 开评标 → 刘彩霞  
- 雄安 + 交易 → 王华聪（可与「雄安工程交易」特指合并为「雄安+交易」）

### D2. 系统名解析

- **地区**：规则 `regions` 中任一为系统名子串即命中（长词优先，如「西藏一张网」优于「西藏」若同时存在）。
- **类型**：
  - 交易：名称含 `交易系统`/`交易平台`/`交易`（且非仅「开评标」语义冲突时按更具体词）
  - 开评标：含 `开评标`/`开标`/`评标`/`编标`
  - 否则：`NONE`（只能匹配 `types` 为空的规则）

### D3. 匹配顺序

1. `priority` 高的特指规则（如石家庄工程开评标）。  
2. 地区命中且类型匹配（系统有类型时）。  
3. 地区命中且规则不限类型（系统类型为 NONE，或以地区为主的兜底）。  
4. 多规则同优先级：全部纳入 @ 集合（去重手机）；展示名取第一个或拼接（实现取第一个为 primary，全部进 @ 列表）。  
5. 无匹配：`ownerName=待指定责任人`，不加入 @ 列表。

### D4. 企微推送

```
sendMarkdown(wecomMarkdown)
sendMentionText(文案含责任人姓名, mentioned_mobile_list=去重手机)
```

关闭静态 `wecom.mentioned-mobile-list` 作为主路径；可保留为额外兜底 @（默认空）。

### D5. 配置落点

`application.yml`（或 `owner-rules.yml`）维护规则；`doc/list.txt` 保留作业务原文说明。

## Risks / Trade-offs

- [Risk] 手机号未绑定企微账号 → @ 无效 → Mitigation：preview 打印匹配结果供人工核对；文档说明需绑定。
- [Risk] 系统名地区别名不一致（如「海口G3」vs「海口三平台」）→ Mitigation：regions 配置同义词（海口、海口G3、海南）。
- [Risk] 「门户/web/监督」无类型 → 仅能打到不限类型规则；雄安门户可能无匹配 → 可接受或后续加规则。

## Migration Plan

1. 落地规则配置与匹配器，preview 核对 41 系统归属。  
2. 再开 notify 验证群内 @。  
3. 回滚：去掉动态 @，恢复仅 markdown。

## Open Questions

- （已决）雄安开评标→刘彩霞；雄安交易→王华聪。  
- 海口G3 / 海南省交易系统：实现时将「海口」「海南」纳入张宇交易规则同义词（与名单「海口三平台/海南三平台」对齐）。
