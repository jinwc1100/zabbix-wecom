# system-owner-dictionary

## Purpose

在 `zabbix.sla_system_owner` 按 `(system_level, system_name)` 存放系统责任人与手机；启动时用 `responsible_persons.xlsx` 做 INSERT IGNORE 种子。低 SLI 通知、周报、复盘同步与催办共用此查找。

## Requirements

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

### Requirement: Seed dictionary from responsible_persons workbook

On application startup, the system SHALL load `sla-wecom-notifier/doc/responsible_persons.xlsx` (or the same workbook copied into application resources) whose columns are 子系统/父系统, 系统名称, 责任人, 手机号码, and MUST insert those rows into `sla_system_owner` using insert-ignore semantics so existing dictionary rows are not overwritten.

#### Scenario: Empty table receives Excel rows

- **WHEN** the table has no rows and startup runs with the workbook
- **THEN** each workbook data row MUST appear as a dictionary row with matching level, name, owner, and mobile

#### Scenario: Restart does not clobber manual edits

- **WHEN** an operator has updated `owner_name` or `mobile` for an existing `(system_level, system_name)` and the application starts again
- **THEN** that row MUST keep the operator’s values

### Requirement: Resolve owner by system level and system name

The system SHALL resolve owner name and mobile by exact match on trimmed `system_level` and trimmed `system_name` against `sla_system_owner`. Resolution MUST NOT use `application.yml` `owners.rules`, region keywords, TRADE/BID type detection, or priority. Low-SLI notify, weekly review Excel, review-event sync (including `refreshOwner`), and incomplete-review WeCom mentions MUST use this same resolution for owner name and/or mobile as applicable.

#### Scenario: Dictionary hit returns owner and mobile

- **WHEN** the dictionary contains `子系统` / a system name / `王华聪` / `13521207069` and a caller resolves that level and name
- **THEN** the resolved owner name MUST be `王华聪` and the mobile MUST be `13521207069`

#### Scenario: Same name different level are distinct

- **WHEN** the dictionary has different owners for the same `system_name` under `子系统` and `父系统`
- **THEN** resolve with `子系统` MUST return the subsystem row’s owner and MUST NOT return the parent row’s owner

### Requirement: Backfill monitor edition from SLA Grafana boards

After Excel insert-ignore seed, the system MUST load system names from Grafana dashboards `afj5c5o5v3rb4f` (子系统) and `aftk5ryifcfeof` (父系统), panels id 4 (老版监控) and id 2 (新版监控), via Grafana HTTP using each panel’s datasource uid. Matching dictionary rows by exact `system_level` (子系统 vs 父系统 from the dashboard) and trimmed `system_name` MUST set `monitor_edition` and `write_jdbc_key` from that panel (id 4 → `老版监控`/`zabbix6`, id 2 → `新版监控`/`zabbix7`). A name that appears on both panel 4 and panel 2 for the same level MUST NOT be updated and MUST be logged as an error. Dictionary rows that match neither panel MUST be set to `老版监控` and `zabbix6`. Owner name and mobile MUST NOT change during backfill. The dictionary table MUST remain on zabbix6 only.

#### Scenario: Child on new panel tagged zabbix7

- **WHEN** panel 2 of `afj5c5o5v3rb4f` returns a subsystem name that exists in the dictionary
- **THEN** that row’s `monitor_edition` MUST be `新版监控` and `write_jdbc_key` MUST be `zabbix7`

#### Scenario: Unmatched Excel row defaults to old pool

- **WHEN** a dictionary row’s name does not appear on either panel of the matching level dashboards
- **THEN** that row MUST have `monitor_edition` `老版监控` and `write_jdbc_key` `zabbix6`

### Requirement: Manual POST may trigger monitor edition backfill

In addition to application startup, the system MUST allow operators to trigger the monitor-edition backfill defined for `sla_system_owner` by calling `POST /api/sla/review-event-sync?refreshRouting=true`. That invocation MUST apply the same rules as startup backfill (Grafana panel 4/2 mapping, collision skip, unmatched rows default to `老版监控`/`zabbix6`, Grafana discovery failure behavior, no change to owner name or mobile). The dictionary table MUST remain on the zabbix6 pool only.

#### Scenario: Manual trigger matches startup rules

- **WHEN** a subsystem name appears on panel 2 of dashboard `afj5c5o5v3rb4f` and an operator calls `POST /api/sla/review-event-sync?refreshRouting=true`
- **THEN** that dictionary row’s `monitor_edition` MUST be `新版监控` and `write_jdbc_key` MUST be `zabbix7` as it would after startup backfill

#### Scenario: Manual trigger does not change owner fields

- **WHEN** an operator has set `owner_name` and `mobile` on a dictionary row and calls `POST /api/sla/review-event-sync?refreshRouting=true`
- **THEN** those fields MUST remain unchanged after backfill
