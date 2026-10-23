## 1. Configuration

- [x] 1.1 Add `pageUrl` to `ReviewSyncProperties` with getter/setter
- [x] 1.2 Set `review-sync.page-url` in `application.yml` to `http://10.32.201.201:8086/review/index.html`

## 2. Nudge markdown

- [x] 2.1 Inject `ReviewSyncProperties` into `ReviewIncompleteNudgeService`
- [x] 2.2 Build markdown as two fixed lines; if `page-url` is non-blank after trim, append `[打开复盘页](<url>)`; never include incomplete count or `count_sla` in the body
- [x] 2.3 Keep three sends, mention lists, and skip-when-count-zero unchanged

## 3. Tests

- [x] 3.1 Assert default URL produces the exact link line; blank `page-url` omits `[打开复盘页]`
- [x] 3.2 Assert body contains the new second line and does not contain `条未保存` or `count_sla`; still three sends when N>0
