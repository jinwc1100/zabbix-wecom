## ADDED Requirements

### Requirement: Persist system owner dictionary in Zabbix

The system SHALL create table `zabbix.sla_system_owner` if it does not exist. Each row MUST store `system_level`, `system_name`, `owner_name`, and `mobile`. The table MUST enforce uniqueness on `(system_level, system_name)`.

#### Scenario: Unique key is level plus name

- **WHEN** two rows share the same `system_name` but different `system_level`
- **THEN** both rows MUST be insertable

#### Scenario: Duplicate level and name is rejected

- **WHEN** an insert repeats an existing `(system_level, system_name)` pair
- **THEN** the insert MUST NOT create a second row (ignore or unique-key conflict)

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
