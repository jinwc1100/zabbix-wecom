## 1. JDBC reservation

- [x] 1.1 Bind `sla.datasources.zabbix7` to a second DataSource bean; keep `spring.datasource` as the only MyBatis primary (zabbix6 / 133)
- [x] 1.2 zabbix7 connect/init failure logs and MUST NOT prevent startup; no `AbstractRoutingDataSource` writes to zabbix7 in this change

## 2. Dictionary schema and backfill (133 only)

- [x] 2.1 ALTER/CREATE `sla_system_owner` on zabbix6: `monitor_edition`, `write_jdbc_key`; unique key remains `(system_level, system_name)`
- [x] 2.2 After Excel INSERT IGNORE, Grafana-backfill from `afj5c5o5v3rb4f`/`aftk5ryifcfeof` panels 4/2; collision log+skip; unmatched default 老版/zabbix6; do not change owner/mobile

## 3. Guardrails

- [x] 3.1 Schema initializer MUST NOT run CREATE/ALTER on the zabbix7 DataSource
- [x] 3.2 Review-event sync, SLI snapshot, review list/save, overview incomplete, WeCom nudge remain zabbix6-only

## 4. Tests

- [x] 4.1 Tests: dictionary columns and backfill defaults; initializer SQL never targets zabbix7; MyBatis still uses primary DS; `mvn test`
