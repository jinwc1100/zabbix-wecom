## Why

催办群消息只说「以下责任人尚有未复盘」，被 @ 的人看不到自己还剩几条。总览已有按人事件条数，催办正文应对齐，避免打开页面才知道工作量。

## What Changes

- **BREAKING**（企微正文契约）：未复盘催办 markdown 在固定两行之后增加按责任人的「姓名：N 条」列表；仍连续发送 3 次；仍 @ 有手机号的责任人。
- 条数口径与总览一致：整表空 `count_sla` 的事件行 `COUNT(*)`，不去重系统名。
- 列表不含空/`NULL`/仅空白的 `owner_name`。`待指定责任人` 有姓名，仍列出条数；无手机则仍不 @。
- 仍禁止正文出现 `count_sla` 字样。打开复盘页链接与发送时机不变。

## Capabilities

### New Capabilities

- （无）仅扩展现有催办。

### Modified Capabilities

- `review-incomplete-wecom-nudge`: 催办正文必须包含非空姓名的未复盘条数列表；取消「正文不得含条数」的禁令（仍禁止 `count_sla`）。

## Impact

- `ReviewIncompleteNudgeService.buildMarkdown`
- 复用 `countIncompleteByOwner`（或等价查询）
- `ReviewIncompleteNudgeServiceTest` 及任何断言固定两行正文的测试
