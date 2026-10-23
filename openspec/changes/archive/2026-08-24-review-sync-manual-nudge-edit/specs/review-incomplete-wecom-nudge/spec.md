## MODIFIED Requirements

### Requirement: WeCom summary count and owner mentions after scheduled sync

When invoked after a successful review-event sync (scheduled job or `POST /api/sla/review-event-sync`), the system SHALL send the incomplete-review WeCom payload **three** times in succession. Each send’s human-visible markdown MUST start with the exact line `重要的内容说3遍` and MUST then include the incomplete row count (not a per-system list). Each send SHALL `@` each distinct incomplete-row `owner_name` that maps to a non-blank mobile on an `owners.rules` entry with the same name. Mapping MUST NOT change `OwnerRuleResolver` system-name matching. Names with no mobile (including `待指定责任人`) MUST still be included in the count and MUST NOT appear in `mentioned_mobile_list`. If the incomplete count is zero, the system MUST NOT send WeCom (zero times). This nudge MUST NOT run as part of `POST /api/sla/notify`.

#### Scenario: Count and mention three times

- **WHEN** the table has 3 incomplete rows for two owners who have mobiles in the rules and the nudge runs
- **THEN** WeCom MUST be invoked three times, each reporting count 3, each mentioning those two mobiles, and each markdown starting with `重要的内容说3遍`

#### Scenario: Unspecified owner counted not mentioned

- **WHEN** an incomplete row’s `owner_name` is `待指定责任人`
- **THEN** the count MUST include that row and the mention list MUST NOT include a mobile for that name

#### Scenario: Zero incomplete skips send

- **WHEN** no row has empty `count_sla`
- **THEN** the system MUST NOT send a WeCom message for this nudge

#### Scenario: Manual sync nudges

- **WHEN** a client calls `POST /api/sla/review-event-sync`, sync succeeds, and incomplete rows exist
- **THEN** the system MUST send this incomplete-review WeCom nudge (three times) as a result of that POST

#### Scenario: Annual notify does not nudge

- **WHEN** a client calls `POST /api/sla/notify`
- **THEN** the system MUST NOT send this incomplete-review WeCom nudge as a result of that POST
