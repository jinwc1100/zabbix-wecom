## Context

未复盘催办在日同步成功后（及 `notify=true` 的手工同步）连续发 3 次同一 markdown，@ 字典里有手机的责任人。正文现为固定两行 + 可选链接，禁止写条数。总览已有 `countIncompleteByOwner`：整表空 `count_sla`、按姓名 `COUNT(*)`、空姓名归一组。

## Goals / Non-Goals

**Goals:**

- 同一条群消息（仍 ×3）列出每个**非空**责任人的未复盘事件条数。
- 口径与总览 KPI/责任人表一致（事件行，不去重系统名）。
- 空姓名不出现在列表；`待指定责任人` 有字面姓名，列出但不 @（无手机规则不变）。

**Non-Goals:**

- 不改为每人一条消息；不改发送次数；不改 page-url；不改 `POST /api/sla/notify`。
- 不给每人不同的明细深链（共用总览链接）。

## Decisions

### 1. 群内点名列表，不是按人拆消息

仍一次 Webhook、同一 markdown、@ 所有有手机的人。比按人发送少刷屏，且保留「说 3 遍」。

### 2. Markdown 形状

```
重要的内容说3遍
SLA 复盘催办：以下责任人尚有宕机明细未复盘。请打开复盘页完成填写并保存。
- 卓松林：7 条
- 王华聪：4 条
[打开复盘页](<page-url>)
```

- 前两行与现网完全一致。
- 姓名行：`- {ownerName}：{count} 条`（全角冒号），按条数降序、同条数按姓名升序（与 `countIncompleteByOwner` 一致）。
- 链接仍在名单之后；page-url 空白则无链接行。
- 禁止 `count_sla`；允许「N 条」。

### 3. 数据

调用已有 `countIncompleteByOwner`，丢掉 `ownerName` 为 null 或 trim 后空的组。若过滤后名单为空但仍有未复盘行（全是空姓名），仍发送：仅前两行 + 链接（与「有未复盘就要催」一致，只是列表为空）。

`@` 逻辑保持：遍历未复盘姓名，跳过空和 `待指定责任人`，有手机才进 `mentioned_mobile_list`。不必与列表完全相同（列表可含待指定，@ 列表不含）。

### 4. 是否发

仍用整表 `countIncompleteReviewEvents() > 0`，不因名单被滤空而跳过。

## Risks / Trade-offs

- [名单很长撑爆企微] → 责任人数量通常有限；若超过再截断需另开需求。
- [点名公开] → 产品已选 A。
- [待指定出现在正文但不 @] → 运维能看见缺口；被催的人不会误以为自己是待指定。

## Migration Plan

发版即改文案。回滚还原 `buildMarkdown` 与测试。

## Open Questions

无。
