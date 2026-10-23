# daily-sli-snapshot-sync

## Purpose

每日（及手工）把父子 SLA Grafana 看板行 UPSERT 到 `sla_sli_snapshot`，与复盘日同步隔离。

## Requirements

### Requirement: Persist Grafana parent and child SLA board rows daily

The system MUST create table `zabbix.sla_sli_snapshot` if it does not exist. On each successful sync the system MUST load every configured panel of `grafana.monthly-sli-sources` (child dashboard uid `afj5c5o5v3rb4f` and parent uid `aftk5ryifcfeof`, including old panel id 4 and new panel id 2) via Grafana HTTP `/api/ds/query` using each panel’s own SQL and datasource uid. The system MUST NOT read these boards through the application JDBC datasource. Each Grafana table row with a non-blank system name MUST be upserted as one snapshot row keyed by report year (calendar year in `Asia/Shanghai` at sync time), `system_level` from the source, exact system name, and `panel_id`. Columns MUST store parsed annual SLI (`年度SLI`) and month cells `一月` through `十二月`. A cell that does not parse as a number after stripping `%` (including Grafana `-` and blank) MUST be stored as SQL NULL and MUST NOT be stored as zero. Old and new panels for the same system name MUST produce separate rows. Rows present in the table but missing from the latest Grafana result MUST remain (MUST NOT delete stale names). Notify and Excel MUST NOT read this table.

#### Scenario: Upsert same system twice in a day

- **WHEN** a child named `雄安服务平台` on panel 2 is synced at 10:00 and again at 17:00 in the same Shanghai calendar year
- **THEN** the table MUST contain one row for that year, level, name, and panel 2 whose SLI columns match the 17:00 Grafana values

#### Scenario: Old and new panels both stored

- **WHEN** both panel 4 and panel 2 return a parent named `某父系统`
- **THEN** the table MUST contain two rows that differ by `panel_id`

#### Scenario: Dash month is null

- **WHEN** a system’s August Grafana cell is `-`
- **THEN** that row’s August snapshot column MUST be NULL

### Requirement: Scheduled snapshot sync isolated from review events

The system MUST run snapshot sync on a dedicated schedule (`sli-snapshot-sync.cron`, zone `Asia/Shanghai` by default) when `sli-snapshot-sync.enabled` is true. Failure of snapshot sync MUST NOT prevent `sla_review_event` daily sync or the incomplete WeCom nudge. When `sli-snapshot-sync.enabled` is false the scheduled snapshot MUST NOT run.

#### Scenario: Snapshot Grafana failure leaves review sync free

- **WHEN** the daily snapshot Grafana query fails and review-event sync is still enabled
- **THEN** the review-event job MUST still be able to run independently

### Requirement: Manual snapshot sync API

The system SHALL expose `POST /api/sla/sli-snapshot-sync` that performs the same Grafana load and UPSERT as the scheduled job. The endpoint MUST run even when `sli-snapshot-sync.enabled` is false. The response MUST report success or failure of the Grafana/MySQL write (including counts of upserted rows on success). The call MUST NOT send WeCom messages.

#### Scenario: Manual sync while schedule disabled

- **WHEN** `sli-snapshot-sync.enabled` is false and a client POSTs `/api/sla/sli-snapshot-sync`
- **THEN** the system MUST still query Grafana and upsert snapshot rows
