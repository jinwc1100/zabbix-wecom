## 1. Collect window

- [x] 1.1 Review-event Grafana collect uses `from` = window-start epoch millis and `to` = trigger instant (not `now-1h`)

## 2. Align down_end

- [x] 2.1 For parseable Grafana end: UPDATE kept row by start identity; do not INSERT another end; sentinel and recovered both overwrite
- [x] 2.2 `count_sla=否` sets `exclude_time` to new `down_end`; 是/空 unchanged
- [x] 2.3 Fold extra rows with the same start (copy blank human fields, delete duplicates); keep row matching collected end or smallest-id / reviewed
- [x] 2.4 When 否 and exclusion exists: UPDATE `period_to` only; no insert; exclusion failure does not fail whole sync; log updated/folded/period_to counts

## 3. Tests

- [x] 3.1 Collect query from is window-start millis not `now-1h`
- [x] 3.2 Sentinel and recovered ends overwrite; no second insert; 否 exclude_time; two recovered rows fold
- [x] 3.3 Existing exclusion `period_to` updates; missing exclusion does not insert
- [x] 3.4 `mvn test`
