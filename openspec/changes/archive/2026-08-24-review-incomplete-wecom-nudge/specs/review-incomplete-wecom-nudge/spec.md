## ADDED Requirements

### Requirement: Incomplete review means empty count_sla

The system SHALL treat a `sla_review_event` row as not yet reviewed when `count_sla` is NULL or blank after trim. The working set MUST be every matching row in the table (no month or time-window filter). Rows with `count_sla` equal to `是` or `否` MUST NOT be in the working set.

#### Scenario: Null count_sla is incomplete

- **WHEN** a row has `count_sla` NULL
- **THEN** that row MUST be counted as not yet reviewed

#### Scenario: Saved yes is complete

- **WHEN** a row has `count_sla` `是`
- **THEN** that row MUST NOT be counted as not yet reviewed

### Requirement: WeCom summary count and owner mentions after scheduled sync

When invoked after a successful scheduled review-event sync, the system SHALL send one WeCom markdown whose human-visible payload is the incomplete row count (not a per-system list). The system SHALL `@` each distinct incomplete-row `owner_name` that maps to a non-blank mobile on an `owners.rules` entry with the same name. Mapping MUST NOT change `OwnerRuleResolver` system-name matching. Names with no mobile (including `待指定责任人`) MUST still be included in the count and MUST NOT appear in `mentioned_mobile_list`. If the incomplete count is zero, the system MUST NOT send WeCom. This nudge MUST NOT run as part of `POST /api/sla/review-event-sync` or `POST /api/sla/notify`.

#### Scenario: Count and mention

- **WHEN** the table has 3 incomplete rows for two owners who have mobiles in the rules
- **THEN** WeCom MUST report count 3 and MUST mention those two mobiles

#### Scenario: Unspecified owner counted not mentioned

- **WHEN** an incomplete row’s `owner_name` is `待指定责任人`
- **THEN** the count MUST include that row and the mention list MUST NOT include a mobile for that name

#### Scenario: Zero incomplete skips send

- **WHEN** no row has empty `count_sla`
- **THEN** the system MUST NOT send a WeCom message for this nudge

#### Scenario: Manual sync does not nudge

- **WHEN** a client calls `POST /api/sla/review-event-sync` and incomplete rows exist
- **THEN** the system MUST NOT send this incomplete-review WeCom nudge as a result of that POST
