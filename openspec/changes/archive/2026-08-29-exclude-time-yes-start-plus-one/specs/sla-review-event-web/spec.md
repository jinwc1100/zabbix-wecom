## MODIFIED Requirements

### Requirement: Save derives exclude_time from count_sla

When a per-row review save succeeds, the system SHALL set that row’s `exclude_time` from the persisted `count_sla` and the row’s existing `down_start` and `down_end` as follows: if `count_sla` is `是`, `exclude_time` MUST be `down_start` plus one second (MUST NOT clamp to `down_end`, even when that instant is after or equal to `down_end`); if `count_sla` is `否`, `exclude_time` MUST equal `down_end`. The save MUST NOT change `down_start` or `down_end`.

#### Scenario: Count yes uses start plus one second

- **WHEN** a client saves a row whose dictionary `count_sla` is `是` and `down_start` plus one second is before `down_end`
- **THEN** `exclude_time` MUST equal `down_start` plus one second

#### Scenario: Count yes uses start plus one second when not before down_end

- **WHEN** a client saves a row whose dictionary `count_sla` is `是` and `down_start` plus one second is after or equal to `down_end`
- **THEN** `exclude_time` MUST equal `down_start` plus one second and MUST NOT equal `down_end` unless `down_end` is already that instant

#### Scenario: Count no uses down_end

- **WHEN** a client saves a row whose dictionary `count_sla` is `否`
- **THEN** `exclude_time` MUST equal that row’s `down_end`
