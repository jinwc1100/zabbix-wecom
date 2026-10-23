## Context

`sla-wecom-notifier` 已能从 Grafana 拉子/父宕机明细（`DowntimeQueryService`），用 `OwnerRuleResolver` 匹配责任人，并用 webhook `@` 手机号。年度 SLI 精简推送（`POST /api/sla/notify`）保持纯手工。周五需要另一条管线：累计未排除宕机 → 填本地模版 → 腾讯在线表格 → 企微带链接。

Grafana `/api/ds/query` 的 `from`/`to` 对现有 raw SQL 基本无效，时间窗必须在应用层按宕机开始时间过滤。`doc/复盘明细.xlsx` 目前不在仓库中，实现前必须入库。

## Goals / Non-Goals

**Goals:**

- 每周五 11:00（可关）或手工 POST：产出 `复盘明细-yyyyMMdd` 在线表并通知责任人。
- 时间窗：开始时间 ∈ `[2026-07-30 00:00, 触发时刻]`（`Asia/Shanghai`）；仅 `未排除` / `-`。
- 模版列写入：父/子系统、系统名称、宕机开始、宕机结束、责任人；不改责任人规则。
- 上传使用腾讯文档开放 API。

**Non-Goals:**

- 不恢复月末最后 7 天的 SLI 自动推送。
- 不改 `POST /api/sla/notify` / preview / 现有 review-excel 双页签。
- 不在进程内调用 Cursor MCP。
- 不自动填写故障类别/子类/是否计算 SLA（留给人工在在线表完成）。
- 不做「本周增量」；窗口为累计。

## Decisions

### 独立编排服务，不塞进 `SlaReviewNotifyService`

`notifyNow()` 绑定低 SLI + 已复盘跳过 + 精简 Markdown。周五管线按宕机行展开、不看年度 SLI。新建例如 `WeeklyReviewDocService.run(Instant triggerTime)`，调度器与 HTTP 都只调它。

### 时间与排除过滤在内存完成

复用现有全量宕机索引；`startTime == null` 的行视为不合格（与无法判定窗口一致）。排除规则与 `ReviewExcelService.isUnexcluded` 对齐。起点做成配置项，默认 `2026-07-30T00:00+08:00`。

### 父子行都保留

子系统看板一行、父系统看板一行分别写入（与既有在线复盘表明细习惯一致），不做父子折叠。

### 本地 xlsx 模版 + 开放 API 导入

相对「在线复制模版再写单元格」：业务指定以 `doc/复盘明细.xlsx` 为准。流程：classpath/文件路径加载 → POI 按表头定位列 → 从首个空数据行追加 → 导入接口上传 → 取可分享 URL。

表头定位按列名（`父/子系统`、`系统名称` 等），避免写死行号。不覆盖「下拉选项」「故障说明」页。

### 腾讯文档凭证与导入

配置 `tencent-docs`（或同等前缀）：应用/企业身份、导入所需 token。密钥走环境变量，不提交真实密钥。导入失败则整次任务失败、不发「假成功」企微（可发失败告警到日志；是否另推运维群列为非目标）。

标题：`复盘明细-` + `yyyyMMdd`（触发日，上海时区）。同日多次运行允许产生同名多份（开放 API 通常允许重名）；不做覆盖更新，避免误改已填复盘。

### 调度与手工入口

- `@EnableScheduling` 仅服务本任务；cron 默认 `0 0 11 ? * FRI`，zone `Asia/Shanghai`，`enabled` 默认 false 直到凭证与模版就绪（上线再开）。
- `POST /api/sla/weekly-review-doc` 忽略开关，始终执行（便于联调）。

### 企微

短 Markdown：窗口说明、行数、在线文档 URL；`mentioned_mobile_list` 为本次命中且解析到手机号的责任人。复用 `WeComWebhookClient.sendMarkdown`，不改 mention 实现。

## Risks / Trade-offs

- [xlsx 导入丢失下拉/数据验证] → 实现期 spike：导入一份样表，不行则改为「创建空 sheet + 写单元格」或官方 copy，但仍以本地模版列布局为准。
- [模版文件缺失] → 任务启动检查路径，缺失则失败并打错误日志。
- [开放 API 权限/分享] → 导入后设置企业内可访问或沿用应用默认；链接打不开则 @ 无意义。
- [累计窗口变长] → Grafana 全表 + 内存过滤，行数增大时观察耗时；必要时再下推 SQL。
- [误开开关但无凭证] → `enabled` 默认 false。

## Migration Plan

1. 将 `复盘明细.xlsx` 放入 `sla-wecom-notifier/doc/`。
2. 配置腾讯文档凭证；`enabled=false` 部署；用 POST 联调。
3. 确认导入表结构与分享链接后，将 `enabled` 设为 true。
4. 回滚：关开关即可停周五任务；POST 仍可用。旧 SLI notify 不受影响。

## Open Questions

- 2026-08-21 已用腾讯文档导入链路实测 `doc/复盘明细.xlsx`：成功生成
  `https://docs.qq.com/sheet/DZmRjUVVtR1ZoSURm`，三张页签（故障录入、故障说明、下拉选项）
  及辅助内容均保留。现有 Open API/MCP 不提供数据验证规则查询，因此下拉联动是否可点击需在
  腾讯文档 UI 人工确认；若联动丢失，回退为导入后通过在线表格 API 重建联动规则。
- 生产客户端采用官方 v2 导入流程：
  `POST /openapi/drive/v2/files/upload` → COS PUT →
  `POST /openapi/drive/v2/files/async-import` →
  `GET /openapi/drive/v2/files/import-progress`。
