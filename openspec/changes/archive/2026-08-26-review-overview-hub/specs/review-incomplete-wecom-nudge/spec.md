## MODIFIED Requirements

### Requirement: WeCom summary count and owner mentions after scheduled sync

When invoked after a successful scheduled review-event sync, or after a successful `POST /api/sla/review-event-sync?notify=true`, the system SHALL send the incomplete-review WeCom payload **three** times in succession. Each send’s human-visible markdown MUST be exactly:

1. the line `重要的内容说3遍`
2. the line `SLA 复盘催办：以下责任人尚有宕机明细未复盘。请打开复盘页完成填写并保存。`
3. when `review-sync.page-url` is non-blank after trim, a third line `[打开复盘页](<page-url>)` using that trimmed URL

The markdown MUST NOT include the incomplete row count or the text `count_sla`. The shipped default for `review-sync.page-url` MUST be `http://10.32.201.201:8086/review/index.html` (the review overview). Each send SHALL `@` each distinct incomplete-row `owner_name` that maps to a non-blank `mobile` on any `sla_system_owner` row with the same `owner_name`. Mapping MUST NOT use `owners.rules`. Names with no mobile (including `待指定责任人`) MUST still be included in the incomplete working set used to decide whether to send, and MUST NOT appear in `mentioned_mobile_list`. If the incomplete count is zero, the system MUST NOT send WeCom (zero times). This nudge MUST NOT run as part of `POST /api/sla/notify`.

#### Scenario: Count and mention three times

- **WHEN** the table has 3 incomplete rows for two owners who have mobiles in the dictionary and the nudge runs
- **THEN** WeCom MUST be invoked three times, each mentioning those two mobiles, each markdown starting with `重要的内容说3遍`, each containing the fixed second line, and MUST NOT contain `count_sla` or `条未保存`

#### Scenario: Unspecified owner counted not mentioned

- **WHEN** an incomplete row’s `owner_name` is `待指定责任人`
- **THEN** that row MUST still cause the nudge to consider incomplete work and the mention list MUST NOT include a mobile for that name

#### Scenario: Zero incomplete skips send

- **WHEN** no row has empty `count_sla`
- **THEN** the system MUST NOT send a WeCom message for this nudge

#### Scenario: Manual sync nudges when notify is true

- **WHEN** a client calls `POST /api/sla/review-event-sync?notify=true`, sync succeeds, and incomplete rows exist
- **THEN** the system MUST send this incomplete-review WeCom nudge (three times) as a result of that POST

#### Scenario: Manual sync without notify does not nudge

- **WHEN** a client calls `POST /api/sla/review-event-sync` with `notify` omitted or false, sync succeeds, and incomplete rows exist
- **THEN** the system MUST NOT send this incomplete-review WeCom nudge as a result of that POST

#### Scenario: Annual notify does not nudge

- **WHEN** a client calls `POST /api/sla/notify`
- **THEN** the system MUST NOT send this incomplete-review WeCom nudge as a result of that POST

#### Scenario: Review page markdown link

- **WHEN** `review-sync.page-url` is `http://10.32.201.201:8086/review/index.html` and the nudge sends
- **THEN** each markdown MUST contain the exact line `[打开复盘页](http://10.32.201.201:8086/review/index.html)`

#### Scenario: Blank page-url omits link

- **WHEN** `review-sync.page-url` is blank or whitespace and the nudge sends
- **THEN** each markdown MUST still include the two fixed lines and MUST NOT contain `[打开复盘页]`
