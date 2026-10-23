## MODIFIED Requirements

### Requirement: Persist system owner dictionary in Zabbix

The system SHALL create table `zabbix.sla_system_owner` if it does not exist on the zabbix6 pool only. Each row MUST store `system_level`, `system_name`, `owner_name`, `mobile`, `monitor_edition`, and `write_jdbc_key`. `monitor_edition` MUST be `老版监控` or `新版监控`. `write_jdbc_key` MUST be `zabbix6` or `zabbix7`. The pair MUST be consistent: `老版监控` with `zabbix6`, `新版监控` with `zabbix7`. The table MUST enforce uniqueness on `(system_level, system_name)` only (MUST NOT include `monitor_edition` in the unique key).

#### Scenario: Unique key is level plus name

- **WHEN** two rows share the same `system_name` but different `system_level`
- **THEN** both rows MUST be insertable

#### Scenario: Duplicate level and name is rejected

- **WHEN** an insert repeats an existing `(system_level, system_name)` pair
- **THEN** the insert MUST NOT create a second row (ignore or unique-key conflict)

#### Scenario: Existing table gains edition columns

- **WHEN** the application starts against a zabbix6 `sla_system_owner` that lacks `monitor_edition` or `write_jdbc_key`
- **THEN** those columns MUST exist after startup

## ADDED Requirements

### Requirement: Backfill monitor edition from SLA Grafana boards

After Excel insert-ignore seed, the system MUST load system names from Grafana dashboards `afj5c5o5v3rb4f` (子系统) and `aftk5ryifcfeof` (父系统), panels id 4 (老版监控) and id 2 (新版监控), via Grafana HTTP using each panel’s datasource uid. Matching dictionary rows by exact `system_level` (子系统 vs 父系统 from the dashboard) and trimmed `system_name` MUST set `monitor_edition` and `write_jdbc_key` from that panel (id 4 → `老版监控`/`zabbix6`, id 2 → `新版监控`/`zabbix7`). A name that appears on both panel 4 and panel 2 for the same level MUST NOT be updated and MUST be logged as an error. Dictionary rows that match neither panel MUST be set to `老版监控` and `zabbix6`. Owner name and mobile MUST NOT change during backfill. The dictionary table MUST remain on zabbix6 only.

#### Scenario: Child on new panel tagged zabbix7

- **WHEN** panel 2 of `afj5c5o5v3rb4f` returns a subsystem name that exists in the dictionary
- **THEN** that row’s `monitor_edition` MUST be `新版监控` and `write_jdbc_key` MUST be `zabbix7`

#### Scenario: Unmatched Excel row defaults to old pool

- **WHEN** a dictionary row’s name does not appear on either panel of the matching level dashboards
- **THEN** that row MUST have `monitor_edition` `老版监控` and `write_jdbc_key` `zabbix6`
