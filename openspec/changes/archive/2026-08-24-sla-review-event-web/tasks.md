## 1. Schema

- [x] 1.1 Update `CREATE TABLE` for `sla_review_event`: drop `remark`; add nullable TEXT `resolution_process`, `root_cause`, `improvement`
- [x] 1.2 Add startup idempotent ALTER: add missing long-text columns; drop `remark` if present (`information_schema.COLUMNS`)
- [x] 1.3 Keep daily `INSERT IGNORE` machine-only; skip-list must not update the three long-text columns or category fields

## 2. APIs

- [x] 2.1 `GET /api/sla/fault-categories` returns dictionary rows for cascade
- [x] 2.2 `GET /api/sla/review-events` fuzzy-filters by system, time, owner (AND; empty ignored)
- [x] 2.3 `PUT /api/sla/review-events/{id}` updates human fields only; trim-required six fields; `count_sla` from dictionary; reject unknown category pairs; do not change machine columns

## 3. Vue page

- [x] 3.1 Main table: machine read-only, category cascade, SLA from dict, description, `n/3`, per-row save; no remark
- [x] 3.2 Expand/drawer: resolution process, root cause, improvement; incomplete save blocked with missing-field feedback
- [x] 3.3 Ship the page as `review-web-demo/index.html` and `sla-wecom-notifier` static `/review/` calling same-origin APIs

## 4. Tests

- [x] 4.1 Tests: ALTER/create shape, save validation (missing long text / unknown pair), fuzzy list, sync does not overwrite long text
