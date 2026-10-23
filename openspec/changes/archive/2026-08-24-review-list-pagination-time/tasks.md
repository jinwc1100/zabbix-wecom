## 1. API

- [x] 1.1 Replace list query: drop `time` substring; add month (`period_month`) and range (`down_start` inclusive dates); default Shanghai current month
- [x] 1.2 Paginate with `page` (1-based) and `size` 50 or 100 only; return `{ total, page, size, items }`; reject inverted range and invalid size
- [x] 1.3 Keep system/owner fuzzy AND; sort `down_start DESC, id DESC`

## 2. Vue

- [x] 2.1 Filter bar: month vs range toggle; month picker; from/to dates; default current month; clear returns to current month
- [x] 2.2 Pager: total, size 50/100, prev/next; filter or size change resets to page 1; confirm if dirty rows
- [x] 2.3 Sync `review-web-demo/index.html` and `static/review/index.html`

## 3. Tests

- [x] 3.1 Tests: default month, month param, inclusive range, invalid size/range, page slice vs total
