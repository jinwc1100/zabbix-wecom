## 1. Schema and persistence

- [x] 1.1 Create `zabbix.sla_sli_snapshot` on startup (`CREATE TABLE IF NOT EXISTS`) with unique `(report_year, system_level, system_name, panel_id)` and nullable annual + `sli_m01`…`sli_m12`
- [x] 1.2 Mapper UPSERT (`INSERT ... ON DUPLICATE KEY UPDATE`); do not DELETE missing Grafana names

## 2. Grafana collection and write

- [x] 2.1 Load `grafana.monthly-sli-sources` panels 4 and 2 via `queryAllRows` (HTTP only); parse `年度SLI` and `一月`…`十二月`; skip blank names; non-numeric → NULL
- [x] 2.2 `report_year` from `Asia/Shanghai` at sync time; old/new panels as separate rows

## 3. Schedule and API

- [x] 3.1 `sli-snapshot-sync` yml (`enabled`, `cron` default 17:00, `zone`); dedicated `@Scheduled` isolated from review-event sync/nudge
- [x] 3.2 `POST /api/sla/sli-snapshot-sync` same UPSERT, ignores `enabled`, no WeCom; return upsert counts or error
- [x] 3.3 Tests: dash cell NULL; duplicate same-day upsert one row; two panels two rows; schedule off still allows POST; review-event scheduler unchanged; `mvn test`
