## 1. List UI

- [x] 1.1 Add per-row checkbox, page select-all, 设为样板, 套用样板, 保存勾选 on `events.html` and `events-preview.html`
- [x] 1.4 Explicit template row: pale-blue full-row wash (cells + select/textarea + open panel), no brand left bar; 当前样板 outline button not btn-primary; default first-complete not pre-highlighted; distinct from dirty yellow
- [x] 1.2 Apply copies six fields + dictionary `count_sla` from template (explicit or first complete on page) onto checked non-template rows; confirm if overwriting non-blank; no save API; mark dirty and editable
- [x] 1.3 Batch save serializes existing per-row PUT for checked complete rows; skip incomplete; continue after failure; toast success/skip/fail counts

## 2. Tests

- [x] 2.1 Page tests: apply first complete row onto next checked blanks; apply does not mention exclusion write; no template leaves fields unchanged
- [x] 2.2 Page tests: batch save invokes save path only for complete checked rows; incomplete skipped; later row still saved after earlier failure
- [x] 2.3 `mvn test`
