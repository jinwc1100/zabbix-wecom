## MODIFIED Requirements

### Requirement: Markdown preview and notify share the same content

The system SHALL provide two Markdown representations for the same report result: a detailed Markdown for preview and a summary Markdown for WeCom notify. The detailed preview Markdown MUST continue to include downtime periods and historical remarks when applicable. The WeCom summary Markdown MUST include system name, annual SLA (SLI) value, system level (`父系统` or `子系统`), and monitor system label (`新版监控` or `老版监控` from the SLI panel that produced the row), and MUST NOT include downtime start/end times or historical downtime remarks. The detailed preview Markdown is NOT required to list the monitor system label. Both Markdown variants MUST end with guidance that subsystem downtime details are viewed in dashboard「SLA宕机明细」and parent-system downtime details are viewed in「SLA父系统宕机明细». WeCom notify MUST use the configured webhook URL and MUST additionally @-mention resolved owner mobiles per `owner-rule-mention` after sending the summary Markdown.

#### Scenario: Preview keeps detailed markdown

- **WHEN** a client calls the SLA preview API successfully
- **THEN** the response MUST include a detailed `markdown` field that still contains near-window downtime lines and historical remarks when those data exist on items

#### Scenario: Notify summary includes monitor system

- **WHEN** a client triggers notify and WeCom is enabled (or inspects `wecomMarkdown` from preview)
- **THEN** each listed system line in the summary Markdown MUST include the monitor system value `新版监控` or `老版监控` corresponding to the SLI panel source for that system

#### Scenario: Notify summary still omits downtime detail

- **WHEN** summary Markdown is generated for WeCom
- **THEN** the content MUST NOT include downtime start/end times or historical downtime remarks

#### Scenario: Preview exposes summary for comparison

- **WHEN** a client calls the SLA preview API successfully
- **THEN** the response SHOULD also include `wecomMarkdown` (or equivalent) so callers can inspect the exact text that would be pushed to WeCom without sending it

#### Scenario: Notify mentions owners

- **WHEN** a client triggers notify and at least one system has a resolved owner mobile
- **THEN** the system MUST send a follow-up WeCom text message whose `mentioned_mobile_list` contains those mobiles

#### Scenario: Detail view hints present in both markdowns

- **WHEN** preview or notify Markdown is generated
- **THEN** the content MUST include guidance to view subsystem details in「SLA宕机明细」and parent-system details in「SLA父系统宕机明细」
