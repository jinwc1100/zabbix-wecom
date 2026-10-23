## 1. Collect vs insert

- [x] 1.1 Collect in-window Grafana rows regardless of exclusion status
- [x] 1.2 INSERT only `未排除` / `-`; skip excluded rows with no start identity

## 2. Align + period_to

- [x] 2.1 Excluded Grafana parseable end overwrites existing start identity (reuse alignEnd)
- [x] 2.2 `count_sla=否` still UPDATEs existing exclusion `period_to`; no exclusion INSERT

## 3. Tests

- [x] 3.1 部分排除 collected; no insert when start missing; overwrite when start exists
- [x] 3.2 否 + existing exclusion `period_to` updates after excluded overwrite
- [x] 3.3 `mvn test`
