## 1. Upsert by start window

- [x] 1.1 Locate exclusions by `slaid + period_from`; if none, keep existing INSERT and `ids` allocation
- [x] 1.2 If any exist: UPDATE smallest id’s `period_to` and `name`; DELETE other rows with the same `slaid + period_from`; never INSERT a second window when `period_to` changes

## 2. Tests

- [x] 2.1 Tests: 是→否 updates `period_to` and leaves one row; same-window resave stays one row; two existing rows collapse to one; `mvn test`
