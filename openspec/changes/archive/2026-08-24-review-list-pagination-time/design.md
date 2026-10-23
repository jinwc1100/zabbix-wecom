## Context

`GET /api/sla/review-events` 现用 `system` / `time` / `owner` 模糊匹配，返回全量数组。`time` 对开始/结束格式化字符串做子串匹配。页面无分页。复盘人要按月填、按区间翻历史。

## Goals / Non-Goals

**Goals:**

- 时间：按月（`period_month`）或按区间（`down_start` 闭区间），互斥。
- 打开与清空后默认 Asia/Shanghai **当月**。
- 分页 `size` ∈ {50, 100}，默认 50；`page` 从 1。
- 系统、责任人模糊 AND。
- Demo 与 `/review/` 一致。

**Non-Goals:**

- 登录、未恢复行特殊 UX、区间按「与宕机时段相交」（避免 9999 哨兵污染）。
- 改保存、日同步、字典接口。

## Decisions

### 1. 按月用 period_month，区间用 down_start

- 月：`month=2026-07` → `period_month = '202607'`。
- 区间：`from`/`to` 为日期；`down_start >= from 00:00:00 AND down_start < to+1day`（含结束日）。
- 不用结束时间相交，避免未恢复哨兵。

### 2. timeMode 互斥

- `timeMode=month`（默认）用 `month`；缺省则服务端按上海当月。
- `timeMode=range` 必须同时有合法 `from`、`to`；`from > to` 返回 400。
- 带了 range 参数但 mode=month 时忽略 range。

### 3. 分页响应

```
{ "total": N, "page": 1, "size": 50, "items": [ ...同现有行字段 ] }
```

- `size` 非 50/100 → 400 或规范到 50（选 **400**，避免静默改条数）。
- `page < 1` → 400；超出末页返回空 `items`、`total` 仍对。
- `LIMIT/OFFSET`；`COUNT(*)` 同一套 WHERE。

### 4. 前端

- 切换「按月 / 按区间」；按月 `type=month`；区间两个 `type=date`。
- 底栏：共 N 条、每页 50|100、上一页/下一页。
- 查询或改 size 时 `page=1`。
- 有未保存行时换页/查询：`confirm` 后丢弃改动（简单可测）。

### 5. 时区

- 当月、日期边界用 `Asia/Shanghai`，与同步一致。

## Risks / Trade-offs

- [BREAKING 裸数组] → 仅本页调用，前后端一起发。
- [OFFSET 深翻] → 复盘数据量可接受。
- [清空变当月] → 按产品约定，不是全量。

## Migration Plan

1. 后端分页 + 新时间参数；单测。
2. 改 Vue；8086 `/review/` 验收当月默认、切区间、50/100。
3. 无数据迁移。

## Open Questions

- 无。
