## 1. Mapper

- [x] 1.1 Add SQL to find rows by start identity (`period_month, system_level, system_name, down_start`); UPDATE sentinel `down_end` (and `exclude_time` when `count_sla` is `否`); DELETE sentinel by id
- [x] 1.2 Change `updateOwnerByIdentity` to match without `down_end`

## 2. Sync promote

- [x] 2.1 After `insertIgnore` on the Grafana collect pass, promote sentinel → recovered end; fold duplicate sentinel+recovered (copy blank human fields, delete sentinel); do not write `sla_excluded_downtime`
- [x] 2.2 Sync result counters: include promoted / folded counts in the log message

## 3. List UI

- [x] 3.1 `events.html` and `events-preview.html`: `formatTime` for end (or a dedicated helper) shows `未恢复` when value starts with `9999-12-31`

## 4. Tests

- [x] 4.1 Tests: promote updates sentinel; 否 refreshes `exclude_time`; fold deletes sentinel; `refreshOwner` matches sentinel vs recovered end; `mvn test`
- [x] 4.2 Page assertions that list HTML maps sentinel to `未恢复` and does not display `9999` as the end label
