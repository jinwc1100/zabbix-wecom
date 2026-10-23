# owner-rule-mention

## Purpose

按 `sla_system_owner` 字典为低 SLI 系统解析责任人与手机，并在企微推送时 @ 对应手机号。

## Requirements

### Requirement: Mention matched owners on WeCom notify

When notifying via WeCom, the system SHALL send the summary Markdown and then a text message that @-mentions the distinct mobile numbers of owners resolved for systems in that report from `sla_system_owner` (same dictionary as review-event sync).

#### Scenario: Deduplicate mobiles

- **WHEN** multiple systems resolve to the same owner mobile
- **THEN** that mobile MUST appear at most once in `mentioned_mobile_list`

#### Scenario: Preview does not mention

- **WHEN** a client calls preview
- **THEN** the system MUST NOT send WeCom messages, and SHOULD still expose resolved owner name/mobile on each item for verification
