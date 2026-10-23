## ADDED Requirements

### Requirement: Manual excluded-downtime backfill endpoint

The system SHALL expose `POST /api/sla/excluded-downtime-backfill` that runs **one** complete pass per request and MUST NOT register a scheduled job for this pass. Query parameter `dryRun` MUST default to `true`. When `dryRun` is true the system MUST NOT insert or update `sla_excluded_downtime`. When `dryRun` is `false` the system MAY write exclusions as specified below.

The pass MUST consider every `sla_review_event` row on zabbix6 whose `count_sla` is `是` or `否`. For each such row the system MUST resolve `write_jdbc_key` from `sla_system_owner` and look up `sla.slaid` on that pool where `sla.name` equals `system_name`. The row MUST be treated as already excluded when that pool has an `sla_excluded_downtime` row for that `slaid` whose window overlaps the downtime using Grafana’s rule: `period_to` greater than Unix `down_start` and `period_from` less than Unix `down_end` (`Asia/Shanghai`). Otherwise, if a `slaid` was found, the row MUST be treated as unexcluded (equivalent to Grafana 排除状态 `未排除` and 排除原因 empty or `-`).

If no `slaid` is found, the system MUST skip that row, MUST include it in the response as skipped-for-missing-SLA, and MUST continue the pass. The HTTP request MUST still succeed.

When `dryRun` is false, for each unexcluded row that has a `slaid` the system MUST call the same exclusion write used after review save (`writeAfterSave`). A failure on one row MUST be recorded and MUST NOT abort remaining rows.

The response MUST be JSON that includes whether the run was a dry run, the count of reviewed rows considered, the unexcluded rows that would be written or were written (identity, times, `count_sla`, target pool), the count of rows skipped because already overlapping, the list of rows skipped for missing SLA name, and any per-row write failures. The system MUST NOT limit the pass to Grafana’s default last-one-hour window.

#### Scenario: Dry run lists unexcluded reviewed rows without writing

- **WHEN** a client POSTs with `dryRun` omitted or true and there are reviewed rows whose target pool has no overlapping exclusion
- **THEN** the response MUST list those rows as would-write and MUST NOT change `sla_excluded_downtime`

#### Scenario: Execute writes only unexcluded rows with slaid

- **WHEN** a client POSTs with `dryRun=false` and a reviewed row is unexcluded and has a matching `sla.name`
- **THEN** the system MUST write that exclusion via the save-path writer and MUST include the row in the written list

#### Scenario: Missing sla name is skipped

- **WHEN** a reviewed row has no `sla.name` equal to `system_name` on the target pool
- **THEN** that row MUST appear in the skipped-no-sla list and MUST NOT cause the request to fail, and MUST NOT receive an exclusion insert

#### Scenario: Already overlapping is not rewritten as a new window

- **WHEN** a reviewed row already has an overlapping exclusion on the target pool
- **THEN** dry-run MUST count it as already excluded and execute MUST NOT insert a second window for a different `period_from`
