## Why

`doc/list.txt` 已改：北京不再整市归王立新。区县开评标、区县交易、市级两个专名、音视频系统要对齐配置，否则 notify / 复盘同步仍按旧规则 @ 错人。

## What Changes

- 王立新只匹配「北京综合交易」「北京工程建设」，去掉 `北京` / `北京市级` / `亦庄` 大网。
- 刘彩霞 BID 增加密云、门头沟；胡一奇 BID 增加亦庄。
- 王华聪 TRADE 覆盖亦庄、门头沟、密云，以及其它含「北京」的交易系统（优先级低于王立新专名）。
- 新增张洪涛：系统名含「音视频」则命中。
- 不改 `OwnerRuleResolver` 算法；不回写已存在的 `sla_review_event.owner_name`。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `owner-rule-mention`: 责任人规则与最新 `doc/list.txt` 对齐，尤其是北京拆分与音视频系统。

## Impact

- `application.yml` → `owners.rules`
- `OwnerRuleResolverTest`（及如有 README 北京示例）
- 年度 notify / preview / 新插入的复盘行责任人
- 已入库复盘行责任人不变
