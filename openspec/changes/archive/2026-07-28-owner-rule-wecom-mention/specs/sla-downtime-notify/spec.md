## MODIFIED Requirements

### Requirement: Markdown preview and notify share the same content

The system SHALL provide two Markdown representations for the same report result: a detailed Markdown for preview and a summary Markdown for WeCom notify. The detailed preview Markdown MUST continue to include downtime periods and historical remarks when applicable. The WeCom summary Markdown MUST include only system name, annual SLA (SLI) value, and system level (`父系统` or `子系统`), and MUST NOT include downtime start/end times or historical downtime remarks. WeCom notify MUST additionally @-mention resolved owner mobiles per `owner-rule-mention` after sending the summary Markdown.

#### Scenario: Preview keeps detailed markdown

- **WHEN** a client calls the SLA preview API successfully
- **THEN** the response MUST include a detailed `markdown` field that still contains near-window downtime lines and historical remarks when those data exist on items

#### Scenario: Notify uses summary markdown only

- **WHEN** a client triggers notify and WeCom is enabled
- **THEN** the webhook payload content MUST be the summary Markdown containing only system name, SLI value, and system level for each listed system

#### Scenario: Preview exposes summary for comparison

- **WHEN** a client calls the SLA preview API successfully
- **THEN** the response SHOULD also include `wecomMarkdown` (or equivalent) so callers can inspect the exact text that would be pushed to WeCom without sending it

#### Scenario: Notify mentions owners

- **WHEN** a client triggers notify and at least one system has a resolved owner mobile
- **THEN** the system MUST send a follow-up WeCom text message whose `mentioned_mobile_list` contains those mobiles
