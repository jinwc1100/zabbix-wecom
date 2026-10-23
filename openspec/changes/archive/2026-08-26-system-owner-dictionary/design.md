## Context

`OwnerRuleResolver` 用 `owners.rules`（地区 contains、TRADE/BID、priority、交易兜底）给 Grafana 系统名分责任人。低 SLI 通知、周报 Excel、复盘日同步、`refreshOwner`、未复盘催办 `@` 都依赖它。核对后的名单在 `sla-wecom-notifier/doc/responsible_persons.xlsx`（780 行：子系统/父系统、系统名称、责任人、手机），系统名当前全局不重复。应用已能在启动时 `CREATE TABLE` 并种子 `sla_fault_category`。

## Goals / Non-Goals

**Goals:**

- 字典表唯一键 `(system_level, system_name)`，Excel 种子导入。
- 运行时精确查找姓名与手机，替换 yml 规则。
- 复盘同步、低 SLI 通知、周报/复盘 Excel、催办共用同一查找。
- `refreshOwner=true` 仍只更新已有 `sla_review_event.owner_name`。

**Non-Goals:**

- 查不到字典行时的默认人、yml 兜底、告警或自动从 Grafana 补行。
- 责任人维护 UI；缺行由运维手工 SQL。
- 拆人员表（人名与手机 1:1，手机冗余存在系统行上即可）。
- 改 `refreshOwner` / `notify` 的 HTTP 契约。

## Decisions

### 表 `zabbix.sla_system_owner`

列：`system_level`、`system_name`、`owner_name`、`mobile`，`UNIQUE (system_level, system_name)`。启动 `CREATE TABLE IF NOT EXISTS`，与复盘表同一套 initializer。

备选：只按 `system_name` 唯一。否决：复盘事件已按层级区分，防止日后同名跨层级。

### 种子：Excel `INSERT IGNORE`

将 `responsible_persons.xlsx` 放入资源（或沿用 `doc/` 并由启动逻辑读取），启动解析后 `INSERT IGNORE`。已有行不覆盖，便于手工改人后重启不被 Excel 冲掉。首次部署空表即灌满 780 行。

备选：把 780 行写进 Java 常量。否决：难维护。备选：仅提供一次性 SQL、应用不灌数。否决：和故障分类种子不一致，易漏导。

### 解析：按键查表，批量可预载

`resolve(systemLevel, systemName)`：trim 后相等匹配，返回该行 `owner_name`/`mobile`。一次采集/通知可先 `SELECT` 全表进 `Map<(level,name), row>`，避免逐行 SQL。

催办 `@`：未复盘行的 distinct `owner_name` → 字典中该姓名任意一行非空 `mobile`（当前每人一号）。不再读 `owners.rules`。

### 删除 yml 规则

去掉 `owners.rules` 及 `OwnerRuleProperties` 规则列表。`OwnerRuleResolver` 改为读 mapper（或改名 `SystemOwnerResolver`）。调用点传入 Grafana 行上的 `systemLevel`，不能只传系统名。

### `refreshOwner`

`ReviewEventSyncService` 采集与 `updateOwnerByIdentity` 不变；行上 `owner_name` 在 `WeeklyReviewDataService` 已用字典填好即可。

## Risks / Trade-offs

- [Grafana 改名/新系统字典无行] → 产品确认运维手工 INSERT；应用不补。
- [启动读 xlsx 失败或账号无 CREATE] → 与现有建表相同；失败应打日志，避免静默无字典。
- [催办按姓名取号、一人多号] → 当前 Excel 无此情况；取该姓名第一条非空手机。

## Migration Plan

1. 发布含建表 + 种子的版本，启动一次写入字典。
2. 部署后对已有复盘行执行 `POST /api/sla/review-event-sync?refreshOwner=true` 回写 `owner_name`（与字典一致时也应幂等）。
3. 回滚：恢复旧 jar/yml；字典表可留（只读无害）。勿在回滚时 DROP，除非确认无外部依赖。

## Open Questions

无。缺行维护与是否带 `system_level` 已在探索中确认。
