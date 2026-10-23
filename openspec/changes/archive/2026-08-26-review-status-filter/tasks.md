## 1. List API

- [x] 1.1 Add `reviewStatus` to `ReviewEventQuery` / list endpoint: omit/blank/`all` skip `count_sla`; `incomplete` reuse `incompleteCountSla`; `complete` = `是`/`否`; other values 400
- [x] 1.2 Apply the filter in `reviewEventFilters` so `count` and `list` share it; AND with system, owner, time

## 2. Review page

- [x] 2.1 Add 全部 / 未复盘 / 已复盘 control; default 全部; query sends `reviewStatus` only when not all; clear restores 全部 + current month
- [x] 2.2 Mirror the same filter in `review-web-demo/index.html` (and `file://` MOCK path)

## 3. Tests

- [x] 3.1 Mapper/service/controller tests: incomplete, complete+other filters, omit all, invalid value
