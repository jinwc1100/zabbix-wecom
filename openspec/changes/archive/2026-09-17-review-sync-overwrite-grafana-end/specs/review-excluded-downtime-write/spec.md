## ADDED Requirements

### Requirement: Sync overwrite of down_end updates existing exclusion period_to

When review-event sync updates a `sla_review_event` row’s `down_end` because Grafana reported a new parseable recovery for the same start identity, and that row’s `count_sla` is `否`, the system MUST set `sla_excluded_downtime.period_to` on the dictionary JDBC pool to the Unix epoch seconds of the new `exclude_time` (`down_end`) for the matching `slaid` and `period_from` (from `down_start`). If no exclusion row exists for that pair, the system MUST NOT insert. The system MUST NOT allocate `ids`, MUST NOT change `name`, and MUST NOT fail the entire sync when this UPDATE affects zero rows or the pool has no matching `sla`.

#### Scenario: Existing exclusion period_to follows 否 new end

- **WHEN** sync sets a reviewed-`否` row’s `down_end` to `2026-09-13 22:27:49` and zabbix6 already has an exclusion for that `slaid` and `period_from`
- **THEN** that exclusion row’s `period_to` MUST equal the Unix seconds of `2026-09-13 22:27:49` Shanghai and `name` MUST be unchanged

#### Scenario: No exclusion row skips insert

- **WHEN** sync updates `down_end` on a `否` row and the target pool has no exclusion for that `slaid` and `period_from`
- **THEN** the system MUST NOT insert `sla_excluded_downtime`
