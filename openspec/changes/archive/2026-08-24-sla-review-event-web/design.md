## Context

`zabbix.sla_review_event` 已由日同步写入机器列。建表含 `remark`，无人填接口。`review-web-demo/index.html` 是 Vue 3 CDN 假数据预览。负责人共用，无登录。`CREATE TABLE IF NOT EXISTS` 无法给已有表加列。

## Goals / Non-Goals

**Goals:**

- 表：删除 `remark`，新增三列可空 TEXT；启动幂等 ALTER。
- API：按系统名称 / 时间 / 责任人模糊查、读故障分类字典、按 id 逐行保存；保存校验人填必填列；是否计算 SLA 由字典带出。
- 页面：Vue 主表 + 展开写三列长文；主表 `n/3`；无备注；与 Demo 交互一致；经应用端口同源访问。
- 日同步仍 `INSERT IGNORE` 机器列，不覆盖人填列。

**Non-Goals:**

- 登录、按人数据隔离、未恢复行特殊 UX、分页（首版过滤结果数百行可接受）。
- 改 Owner 规则、`POST /api/sla/notify`、周度腾讯文档 / Excel 模板。
- 人填列数据库 NOT NULL（会阻断日同步插入）。

## Decisions

### 1. 人填列可空，必填只在保存

- **选择**：`resolution_process`、`root_cause`、`improvement` 为 `TEXT NULL`。保存 API 对 trim 后空串拒绝。
- **备选**：DB NOT NULL → 新宕机 INSERT 失败。否决。

### 2. 启动 ALTER，而不是只改 CREATE

- **选择**：建表之后：缺三列则 `ADD`；存在 `remark` 则 `DROP`。用 `information_schema.COLUMNS` 判断，可重复启动。
- **备选**：仅手工 DBA 脚本。否决：易漏。新环境 `CREATE` 文本与线上一致（无 remark、有三列）。

### 3. SLA 与分类以服务端字典为准

- **选择**：保存时用 `(fault_category, fault_subcategory)` 查 `sla_fault_category`；不存在则 400；`count_sla` 写入字典值，忽略客户端 SLA。
- **备选**：信任前端。否决：子类名可跨类别重复。

### 4. 查询与保存 API

- **选择**：
  - `GET /api/sla/review-events?system=&time=&owner=`：三条件 AND，空则忽略。系统 / 责任人 `LIKE %x%`；时间对 `DATE_FORMAT` 后的 `down_start` 与 `down_end` 字符串做子串匹配。
  - `GET /api/sla/fault-categories`：字典列表。
  - `PUT /api/sla/review-events/{id}`：只更新人填列（类别、子类、描述、SLA、三长文）及 `updated_at`。不改机器列。
- **备选**：按唯一键更新。按 id 更简单。

### 5. 前端部署

- **选择**：Vue 3 CDN 单页，以 `review-web-demo/index.html` 为交互源，并放入 `sla-wecom-notifier/src/main/resources/static/review/index.html`，访问 `http://<host>:8086/review/`。`fetch` 同源 `/api/sla/...`。
- **备选**：独立 Vite 工程。首版无构建链，CDN 足够。用 `file://` 打开 Demo 调 API 可能被拦；真数据验收以 Boot 静态页为准。

### 6. 方案 B 交互

- 主表：机器只读 + 类别/子类 + SLA 只读 + 描述 + `n/3` + 保存。
- 展开（同行下方或侧栏）：解决过程、根本原因、改进措施。
- 保存：六项必填未满则禁用或 toast 列出缺失；三项长文计入 `n/3`。无备注。

### 7. 日同步跳过名单

- INSERT 列不变。冲突不更新：`fault_category`、`fault_subcategory`、`fault_desc`、`count_sla`、`resolution_process`、`root_cause`、`improvement`、`owner_name`。去掉 `remark`。

## Risks / Trade-offs

- [ALTER DROP remark 不可逆] → 列从未被产品写入；可接受。回滚需再 ADD 空列。
- [一次拉回数百行] → 模糊过滤后仍可能上百行；可接受。变慢再分页。
- [file:// Demo 调 API 被拦] → 用 8086 `/review/` 验收真数据。
- [未恢复哨兵 9999 出现在时间列] → 本 change 不改展示规则。

## Migration Plan

1. 改 CREATE + ALTER，对已有库启动一次确认列。
2. API + 单测（校验、字典 SLA、模糊查、同步不覆盖长文）。
3. 更新 Demo 与 static 页，8086 打开验收保存。
4. 回滚：关页面入口；三列可保留（兼容空值）。勿轻易 DROP 三列（可能已有人填）。

## Open Questions

- 无。分页与登录留待后续 change。
