## MODIFIED Requirements

### Requirement: Manual POST can refresh exclude_time from the table

The POST MUST accept a query parameter `refreshExcludeTime` whose default is false. When `refreshExcludeTime` is true, the system MUST NOT collect Grafana downtime for this mode, MUST NOT insert any row, and MUST recompute `exclude_time` on existing `sla_review_event` rows using the same derivation as review save: `count_sla` `是` → `down_start` plus one second (MUST NOT clamp to `down_end`); `count_sla` `否` → `down_end`; NULL or blank `count_sla` → `exclude_time` MUST be set NULL. The refresh MUST NOT change fault_category, fault_subcategory, fault_desc, count_sla, resolution_process, root_cause, improvement, owner_name, down_start, or down_end. Cron MUST NOT use this mode. This flag is independent of `refreshOwner`; both MAY be true on one request.

#### Scenario: Refresh sets yes rows from start plus one second

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `是` with `down_start` plus one second before `down_end`
- **THEN** that row’s `exclude_time` MUST equal `down_start` plus one second and the system MUST NOT insert rows from Grafana for this flag

#### Scenario: Refresh sets yes rows from start plus one second when not before down_end

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `是` and `down_start` plus one second is after or equal to `down_end`
- **THEN** that row’s `exclude_time` MUST equal `down_start` plus one second

#### Scenario: Refresh sets no rows to down_end

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has `count_sla` `否`
- **THEN** that row’s `exclude_time` MUST equal `down_end`

#### Scenario: Refresh clears incomplete rows

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshExcludeTime=true` and a table row has empty `count_sla`
- **THEN** that row’s `exclude_time` MUST be NULL
