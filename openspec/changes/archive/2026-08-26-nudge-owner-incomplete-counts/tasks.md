## 1. Markdown with per-owner counts

- [x] 1.1 Load `countIncompleteByOwner` in `ReviewIncompleteNudgeService`; skip groups whose name is null or blank
- [x] 1.2 Append `- {name}：{count} 条` after the two fixed lines, then the page-url line; keep three identical sends and existing @ rules
- [x] 1.3 Update `ReviewIncompleteNudgeServiceTest`: named owners’ counts in body; blank owner absent; `待指定责任人` listed not mentioned; still no `count_sla`; still three sends

## 2. Verify

- [x] 2.1 `mvn test` in `sla-wecom-notifier`
