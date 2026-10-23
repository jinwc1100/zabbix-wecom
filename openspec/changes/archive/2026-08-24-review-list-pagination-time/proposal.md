## Why

复盘列表一次拉全表，时间又是字符串模糊匹配，负责人很难按「这个月」或一段日期翻看。数据量上来后页面会卡，也无法按月填报。

## What Changes

- **BREAKING**：`GET /api/sla/review-events` 去掉 `time` 模糊参数；不再返回裸数组。
- 时间二选一：**按月**（`period_month`）或 **按区间**（`down_start` 落在起止日期内，结束日含当天）。两种条件互斥。
- 页面打开默认 **Asia/Shanghai 当月**；清空筛选后仍回到当月，不是全历史。
- 服务端分页：`page` 从 1 计；`size` 仅 **50 或 100**，默认 50。响应 `{ total, page, size, items }`。
- 系统名称、责任人模糊查询保持 AND。改查询或改每页条数回到第 1 页。
- Demo 与 `/review/` 静态页交互一致。不改保存接口、日同步、登录。

## Capabilities

### New Capabilities

- （无）列表能力仍归复盘页。

### Modified Capabilities

- `sla-review-event-web`: 列表从全量模糊时间改为按月/按区间 + 可选 50/100 分页；默认当月。

## Impact

- `ReviewEventController` / `ReviewEventService` / `SlaReviewEventMapper.xml` 列表查询。
- `review-web-demo/index.html` 与 `static/review/index.html`。
- 测试：当月默认、按月、按区间、非法 size、分页边界。
