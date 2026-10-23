# review-incomplete-wecom-nudge

## Purpose

日同步定时成功后催办；手工 POST 仅当 `notify=true` 时催办。按整表 `count_sla` 为空统计未复盘条数，企微连续发 3 次固定催办文案并 `@` 能解析到手机的责任人。正文含「打开复盘页」链接（`review-sync.page-url`）。`POST /api/sla/notify` 不催办。

## Requirements

### Requirement: Incomplete review means empty count_sla

The system SHALL treat a `sla_review_event` row as not yet reviewed when `count_sla` is NULL or blank after trim. The working set MUST be every matching row in the table (no month or time-window filter). Rows with `count_sla` equal to `是` or `否` MUST NOT be in the working set.

#### Scenario: Null count_sla is incomplete

- **WHEN** a row has `count_sla` NULL
- **THEN** that row MUST be counted as not yet reviewed

#### Scenario: Saved yes is complete

- **WHEN** a row has `count_sla` `是`
- **THEN** that row MUST NOT be counted as not yet reviewed

### Requirement: WeCom summary count and owner mentions after scheduled sync

When invoked after a successful scheduled review-event sync, or after a successful `POST /api/sla/review-event-sync?notify=true`, the system SHALL send the incomplete-review WeCom payload **three** times in succession. Each send’s human-visible markdown MUST start with exactly:

1. the line `重要的内容说3遍`
2. the line `SLA 复盘催办：以下责任人尚有宕机明细未复盘。请打开复盘页完成填写并保存。`

After those two lines, the markdown MUST include one line per incomplete-review owner whose `owner_name` is non-blank after trim, formatted `- {ownerName}：{count} 条` where `count` is the number of incomplete event rows for that owner (`COUNT(*)` of rows with empty `count_sla`, same predicate and whole-table scope as this nudge; MUST NOT distinct `system_name`). Lines MUST be ordered by `count` descending, then `ownerName` ascending. Owners whose name is NULL or blank after trim MUST NOT appear in this list. The name `待指定责任人` MUST appear when that name has incomplete rows.

When `review-sync.page-url` is non-blank after trim, the markdown MUST end with a line `[打开复盘页](<page-url>)` using that trimmed URL (after the owner-count lines, or immediately after the two fixed lines when the owner list is empty).

The markdown MUST NOT include the text `count_sla`. The shipped default for `review-sync.page-url` MUST be `http://10.32.201.201:8086/review/index.html` (the review overview). Each send SHALL `@` each distinct incomplete-row `owner_name` that maps to a non-blank `mobile` on any `sla_system_owner` row with the same `owner_name`. Mapping MUST NOT use `owners.rules`. Names with no mobile (including `待指定责任人`) MUST still be included in the incomplete working set used to decide whether to send, and MUST NOT appear in `mentioned_mobile_list`. If the incomplete count is zero, the system MUST NOT send WeCom (zero times). This nudge MUST NOT run as part of `POST /api/sla/notify`.

#### Scenario: Count and mention three times with per-owner lines

- **WHEN** the table has 3 incomplete rows for two named owners who have mobiles in the dictionary (for example 2 and 1 rows) and the nudge runs
- **THEN** WeCom MUST be invoked three times, each mentioning those two mobiles, each markdown starting with `重要的内容说3遍`, each containing the fixed second line, each containing a `- 姓名：N 条` line for each of those owners with the matching N, and MUST NOT contain `count_sla`

#### Scenario: Blank owner omitted from list

- **WHEN** incomplete rows exist for a named owner and for a NULL or whitespace `owner_name`
- **THEN** the markdown MUST include a count line for the named owner and MUST NOT include a count line whose name is empty

#### Scenario: Unspecified owner counted listed not mentioned

- **WHEN** an incomplete row’s `owner_name` is `待指定责任人`
- **THEN** that row MUST still cause the nudge to consider incomplete work, the markdown MUST include `- 待指定责任人：N 条` for that row count, and the mention list MUST NOT include a mobile for that name

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
- **THEN** each markdown MUST contain the exact line `[打开复盘页](http://10.32.201.201:8086/review/index.html)` after the owner-count lines (or after the two fixed lines if none)

#### Scenario: Blank page-url omits link

- **WHEN** `review-sync.page-url` is blank or whitespace and the nudge sends
- **THEN** each markdown MUST still include the two fixed lines and MUST NOT contain `[打开复盘页]`
