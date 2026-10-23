## Why

责任人目前靠 `application.yml` 里地区关键字 + TRADE/BID + 优先级猜测系统名，和已核对的逐系统名单脱节，改人要改配置并重启。已有 `doc/responsible_persons.xlsx`（层级、系统名、责任人、手机）应对齐进 Zabbix 字典表，运行时按精确键查找。

## What Changes

- 在 `zabbix` 新建系统责任人字典表，用 `responsible_persons.xlsx` 导入种子数据。
- **BREAKING**（匹配语义）：废弃 `owners.rules` 地区/类型规则；责任人与手机一律按 `(system_level, system_name)` 查字典。
- 复盘日同步、低 SLI 企微、周报/复盘 Excel、未复盘催办 `@` 共用该字典。
- `POST /api/sla/review-event-sync?refreshOwner=true` 的触发与「只更新已有行 `owner_name`、不插入、不改复盘字段」不变；`owner_name` 改为字典当前值。
- 字典缺行（新 Grafana 系统）由运维手工 INSERT，本变更不设计自动补全、yml 兜底或「待指定责任人」流程。

## Capabilities

### New Capabilities

- `system-owner-dictionary`: 字典表结构、唯一键 `(system_level, system_name)`、从 Excel 导入种子、按该键解析责任人姓名与手机。

### Modified Capabilities

- `owner-rule-mention`: 解析从地区/类型规则改为字典精确匹配；企微 `@` 仍用解析到的手机去重。
- `daily-sla-review-event-sync`: 写入/刷新的 `owner_name` 来自字典；`refreshOwner` HTTP 行为不变。
- `review-incomplete-wecom-nudge`: `owner_name` → 手机改为查字典，不再读 `owners.rules`。
- `sla-review-excel`: 责任人列改为字典查找。
- `weekly-review-tencent-doc`: 模版责任人列与 `@` 改为同一字典。

## Impact

- `sla-wecom-notifier`：`OwnerRuleResolver` / `OwnerRuleProperties`、`application.yml` `owners.rules`、`SlaSchemaInitializer`、复盘同步、notify、Excel、催办。
- 数据库：`zabbix` 新表；启动建表权限与现有 `sla_review_event` 相同。
- API 路径与 query（含 `refreshOwner`）不变。
- 测试：`OwnerRuleResolverTest` 及依赖 yml 规则的催办/同步用例改为字典夹具。
