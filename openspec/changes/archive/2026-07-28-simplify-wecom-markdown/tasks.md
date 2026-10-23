## 1. Result model and formatter

- [x] 1.1 Add `wecomMarkdown` to `NotifyResult` (keep existing `markdown` as detailed version)
- [x] 1.2 Add `formatWecomSummary` in `SlaMarkdownFormatter` with only 系统名称 / SLA(SLI)值 / 父系统|子系统
- [x] 1.3 Keep existing detailed `format` (or rename to `formatDetail`) unchanged in content

## 2. Service wiring

- [x] 2.1 In `SlaReviewNotifyService.buildReport`, fill both `markdown` and `wecomMarkdown`
- [x] 2.2 Change `notifyNow` to send `wecomMarkdown` to WeCom instead of detailed `markdown`
- [x] 2.3 Ensure `preview` returns both fields and full `items` (downtime periods/remarks retained)

## 3. Docs and verification

- [x] 3.1 Update README to explain preview detailed vs WeCom summary Markdown
- [x] 3.2 Rebuild, call preview and confirm detailed vs summary length/content
- [x] 3.3 Optionally call notify once to confirm WeCom receives short single/few messages
