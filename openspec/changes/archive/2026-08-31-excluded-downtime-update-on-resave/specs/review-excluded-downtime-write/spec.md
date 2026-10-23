## MODIFIED Requirements

### Requirement: Save writes sla_excluded_downtime on the dictionary pool

After a successful per-row review save that persisted `exclude_time`, the system MUST write one row into `zabbix.sla_excluded_downtime` on the JDBC pool named by `sla_system_owner.write_jdbc_key` for the saved row’s `system_level` and `system_name` (dictionary remains on zabbix6). Key `zabbix6` MUST use the primary datasource; key `zabbix7` MUST use `sla.datasources.zabbix7`. The system MUST NOT create tables on zabbix7.

`slaid` MUST be the `sla.slaid` on **that same pool** whose `sla.name` equals the review row’s `system_name` (trimmed). Missing dictionary row, missing `write_jdbc_key`, or no matching `sla` row MUST fail the save request.

`period_from` MUST be the Unix epoch seconds of `down_start` in `Asia/Shanghai`. `period_to` MUST be the Unix epoch seconds of `exclude_time` in `Asia/Shanghai`. `name` MUST equal `故障分类:` concatenated with `fault_category`, then `--故障子类:`, `fault_subcategory`, `--故障描述:`, and `fault_desc`. If that string exceeds 255 characters it MUST be truncated to 255.

The system MUST locate an existing exclusion on that pool by `slaid` and `period_from` (MUST NOT require `period_to` to match). If none exists, the system MUST insert one row. `sla_excluded_downtimeid` for an insert MUST be allocated from that pool’s `ids` row for table `sla_excluded_downtime` field `sla_excluded_downtimeid` (next id is `nextid + 1`, then `nextid` MUST be updated to that id). The insert MUST NOT rely on AUTO_INCREMENT.

If one or more rows exist for that `slaid` and `period_from`, the system MUST update the row with the smallest `sla_excluded_downtimeid`: set `period_to` and `name` to the values derived from this save, MUST NOT insert another row, and MUST delete any other rows with the same `slaid` and `period_from`. After the save the target pool MUST have exactly one exclusion row for that `slaid` and `period_from`.

#### Scenario: Old-monitor save writes zabbix6 exclusion

- **WHEN** a saved row’s dictionary `write_jdbc_key` is `zabbix6` and `sla.name` matches `system_name` on zabbix6
- **THEN** `sla_excluded_downtime` on zabbix6 MUST contain a row with that `slaid`, `period_from` from `down_start`, `period_to` from `exclude_time`, and the concatenated `name`

#### Scenario: New-monitor save writes zabbix7 exclusion

- **WHEN** a saved row’s dictionary `write_jdbc_key` is `zabbix7` and `sla.name` matches `system_name` on zabbix7
- **THEN** the write MUST run on zabbix7 and MUST NOT write that exclusion on zabbix6

#### Scenario: Missing sla name fails the save

- **WHEN** the target pool has no `sla` row whose `name` equals `system_name`
- **THEN** the save API MUST fail

#### Scenario: Repeat save with the same exclude_time keeps one row

- **WHEN** the client saves the same row again with the same derived `exclude_time`
- **THEN** the target pool MUST still have a single exclusion row for that `slaid` and `period_from`

#### Scenario: Count-SLA change updates period_to instead of inserting

- **WHEN** the client had saved with `count_sla` `是` (so `period_to` is `down_start` plus one second) and then saves again with `count_sla` `否` (so `period_to` equals `down_end`)
- **THEN** that pool MUST still have exactly one exclusion row for that `slaid` and `period_from`, that row’s `period_to` MUST equal the Unix seconds of `down_end`, and the system MUST NOT leave a second row with the old `period_to`

#### Scenario: Duplicate windows for the same start are collapsed

- **WHEN** the target pool already has two exclusion rows with the same `slaid` and `period_from` and different `period_to`, and the client saves that review row
- **THEN** the pool MUST have exactly one exclusion row for that `slaid` and `period_from` with `period_to` and `name` from this save
