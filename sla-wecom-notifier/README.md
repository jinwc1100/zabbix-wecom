# SLA 企业微信通知服务

轻量 Spring Boot 2.7（Java 8）应用：过滤年度 SLI &lt; 99.9 的系统，关联宕机明细，企微推送精简三字段，并按 Zabbix 字典表 `sla_system_owner` 匹配责任人手机号进行 `@`。

## SLI 双源（方案 B）

配置：`grafana.sli-sources`。

| 源 | Dashboard UID | 层级标签 |
|----|---------------|----------|
| 子系统 SLA | `afj5c5o5v3rb4f` | 子系统 |
| 父系统 SLA | `aftk5ryifcfeof` | 父系统 |

两源各自过滤 `年度SLI < sli-threshold` 后合并进推送名单。展示用的「父系统 / 子系统」**以 SLI 来源为准**；宕机明细只查对应层级的宕机看板。无宕机时层级仍保留，不会改成「未知」。

## 责任人匹配

责任人与手机号来自 `zabbix.sla_system_owner`，唯一键为 `(system_level, system_name)`（子系统/父系统 + 系统名称精确匹配）。

- 启动时建表，并用 `doc/responsible_persons.xlsx`（同时打进 classpath `db/responsible_persons.xlsx`）做 `INSERT IGNORE` 种子；已有行不会被 Excel 覆盖。
- 新 Grafana 系统需运维向该表手工 INSERT。
- 低 SLI 企微、周报 Excel、复盘日同步（含 `refreshOwner`）、未复盘催办 `@` 共用此表。
- `POST /api/sla/review-event-sync?refreshOwner=true` 仍只按复盘表唯一键更新已有行的 `owner_name`，不插入、不改复盘人填字段；姓名取字典当前值。

`notify`：先发精简 Markdown，再发 text，`mentioned_mobile_list` 为本次命中手机（去重）。  
`preview`：返回 `ownerName` / `ownerMobile`，不推送、不 `@`。  
本月宕机若全部已复盘（排除状态≠「未排除」且排除原因≠「-」），`preview` 仍展示并标「本月已复盘」；`notify` 会跳过这些系统。无当月宕机的低 SLI 仍推送。

文末固定指引：

- 子系统宕机明细请查看：**SLA宕机明细**
- 父系统宕机明细请查看：**SLA父系统宕机明细**

企微地址配置：`wecom.webhook-url`（改后需重启）。

## 推送方式

- 年度低 SLI 清单**不会**定时自动推送；需要时调用 `POST /api/sla/notify`。
- 周度复盘在线文档**没有**定时任务；需要时调用 `POST /api/sla/weekly-review-doc`。
- 未排除宕机可同步到 `zabbix.sla_review_event`：`POST /api/sla/review-event-sync`（不受开关影响）。query `notify` 默认 `false` 只写库；`notify=true` 且成功后才发未复盘企微催办。query `refreshOwner=true` 只按唯一键更新 `owner_name`，不插入新行、不改复盘人填字段。每小时整点由 `review-sync.enabled` 控制同步；仅上海时间 17:00 那次成功同步后催办。已存在开始时间不重复插入。
- 周度任务累计查询宕机开始时间从 `2026-07-30 00:00:00` 到触发时刻且排除状态为
  `未排除` / `-` 的父、子系统明细，填写 `doc/复盘明细.xlsx`。
- 默认 `tencent-docs.mode: mcp`：服务只落盘 xlsx（`target/weekly-review/`），由 Cursor 腾讯文档 MCP 导入后，再调用
  `POST /api/sla/weekly-review-wecom?url=文档地址` 推企微。
- `tencent-docs.mode: openapi` 时由进程调用腾讯文档开放 API（需环境变量凭证）。

## 复盘网页

- `/review/`、`/review/index.html`：复盘总览。显示父/子系统未复盘事件条数、按责任人未复盘条数，以及年度 SLI 低于阈值的父/子系统排行。
- `/review/events.html`：复盘明细与逐行填报。无 query 时默认东八区当月、全部状态。
- 总览 KPI 与责任人链接使用 `reviewStatus=incomplete&timeMode=all`，确保整表待办数字与明细一致；顶栏「去复盘」不带 query。
- `GET /api/sla/review-overview`：总览数据。Grafana 年度 SLI 失败时，MySQL 未复盘统计仍返回。
- 企微催办默认「打开复盘页」指向总览 `/review/index.html`。

## 双 Markdown

| 字段 | 用途 |
|------|------|
| `markdown` | 完整明细（含近窗宕机、备注、责任人） |
| `wecomMarkdown` | 企微精简：系统名称 / SLI / 父\|子系统 |

## 启动

```bash
cd sla-wecom-notifier
mvn -DskipTests package
java -jar target/sla-wecom-notifier-1.0.0-SNAPSHOT.jar
```

端口：`8086`

```bash
curl http://localhost:8086/api/sla/preview
curl -X POST http://localhost:8086/api/sla/notify
curl -X POST http://localhost:8086/api/sla/review-event-sync
curl -X POST "http://localhost:8086/api/sla/review-event-sync?notify=true"
curl -X POST "http://localhost:8086/api/sla/review-event-sync?refreshOwner=true"
curl -X POST http://localhost:8086/api/sla/weekly-review-doc
curl -X POST "http://localhost:8086/api/sla/weekly-review-wecom?url=https://docs.qq.com/sheet/xxxx"
```

## 复盘 Excel 导出

`GET /api/sla/review-excel?month=yyyy-MM`（`month` 可选，缺省为东八区当前年月）。

下载 `.xlsx`，含两个页签（页签名中的月份**仅作命名**，**不按**宕机开始时间过滤自然月）：

| 页签 | 系统范围 | 宕机行 |
|------|----------|--------|
| `{month}-SLA小于99.9%待复盘` | 年度清单中 SLI &lt; 99.9% | 对应层级宕机看板中排除状态为 `未排除` 或 `-` |
| `{month}-所有待复盘` | 年度清单全量 | 同上 |

列：系统名称、系统类别（父系统/子系统，来自 SLI 源）、中断原因（空，手工填）、宕机开始/结束时间、责任人（`sla_system_owner` 按层级+名称）。

```bash
curl -OJ "http://localhost:8086/api/sla/review-excel?month=2026-07"
```
