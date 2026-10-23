## 1. Schema

- [x] 1.1 Add `exclude_time DATETIME NULL` to `CREATE TABLE` (`SlaSchemaMapper`, `schema-sla-review.sql`) and startup `ALTER` when the column is missing
- [x] 1.2 Keep `insertIgnore` without `exclude_time`; duplicate skip must not update it

## 2. Save derivation

- [x] 2.1 On successful review PUT, persist `exclude_time` from `count_sla` + `down_start`/`down_end` (是: start+1s, clamp to `down_end`; 否: `down_end`); do not change start/end
- [x] 2.2 Tests: yes longer-than-1s; yes clamp; no uses `down_end`; failed validation does not write

## 3. Table refresh API

- [x] 3.1 `POST /api/sla/review-event-sync?refreshExcludeTime=true`: scan table, same formula; empty `count_sla` → NULL; no Grafana collect; no insert; cron unused
- [x] 3.2 Independent of `refreshOwner`; tests for yes/no/incomplete and no Grafana when only this flag

## 4. UI

- [x] 4.1 Confirm `/review/events.html`, `/review/index.html`, and demo HTML have no `exclude_time` column/input (list SELECT may omit the column)

## 5. Verify

- [x] 5.1 `mvn test` in `sla-wecom-notifier`
