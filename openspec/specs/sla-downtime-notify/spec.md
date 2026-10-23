# sla-downtime-notify

## Purpose

从 Grafana 拉取年度 SLI 不达标系统（父系统与子系统双看板合计），关联对应宕机明细，生成预览与企微精简 Markdown，并经手工 HTTP 推送到企业微信（无定时自动推送）。

## Requirements

### Requirement: Collect low-SLI systems from parent and subsystem SLA dashboards

The system SHALL query both the subsystem annual SLA dashboard (`afj5c5o5v3rb4f`) and the parent annual SLA dashboard (`aftk5ryifcfeof`), filter rows whose annual SLI is below the configured threshold (default 99.9), and form the low-SLI working set as the union of systems from both sources. Each retained system MUST keep its annual SLI value and the source system level configured for that dashboard (`子系统` or `父系统`). Cross-source name deduplication is NOT required when names do not collide.

#### Scenario: Subsystem source contributes low-SLI rows

- **WHEN** the subsystem SLA dashboard returns rows with annual SLI below threshold
- **THEN** those systems MUST appear in the combined low-SLI set with source level `子系统`

#### Scenario: Parent source contributes low-SLI rows

- **WHEN** the parent SLA dashboard returns rows with annual SLI below threshold
- **THEN** those systems MUST appear in the combined low-SLI set with source level `父系统`

#### Scenario: Combined set is the union

- **WHEN** both sources return low-SLI systems
- **THEN** the notify/preview working set MUST include all systems from both sources

### Requirement: Associate low-SLI systems with downtime details

The system SHALL take the combined low-SLI working set from parent and subsystem SLA dashboards, match each system name against Grafana leaf downtime and parent downtime dashboards for downtime periods, and produce notify rows that include system name, annual SLI value, downtime start/end times when matched, and system level. System level MUST be taken from the SLI source configuration (`父系统` or `子系统`) — Plan B — and MUST NOT be overwritten to `未知` solely because downtime rows are missing. Downtime period matching SHOULD prefer the downtime index that corresponds to the SLI source level (parent source → parent downtime dashboard; subsystem source → leaf downtime dashboard).

#### Scenario: Parent SLI source labels parent even without downtime

- **WHEN** a system comes from the parent SLA dashboard and has no matching downtime rows
- **THEN** the system level MUST remain `父系统` and the row MUST still appear when unmatched-policy is keep

#### Scenario: Subsystem SLI source labels subsystem even without downtime

- **WHEN** a system comes from the subsystem SLA dashboard and has no matching downtime rows
- **THEN** the system level MUST remain `子系统` and the row MUST still appear when unmatched-policy is keep

#### Scenario: Parent source matches parent downtime periods

- **WHEN** a parent-sourced low-SLI system name matches a row in the parent downtime dashboard
- **THEN** downtime start/end MUST come from the parent downtime columns while system level remains `父系统`

#### Scenario: Subsystem source matches leaf downtime periods

- **WHEN** a subsystem-sourced low-SLI system name matches a row in the leaf downtime dashboard
- **THEN** downtime start/end MUST come from the leaf recovery/end column (including literal `未恢复` when applicable) while system level remains `子系统`

### Requirement: Show only recent-window downtime rows

For each low-SLI system with multiple downtime matches, the system SHALL include in the primary detail list only downtime records whose downtime start time falls within the configured recent window (default: rolling last 30 days).

#### Scenario: Older outages excluded from detail list

- **WHEN** a system has downtime starting inside the recent window and also downtime starting before the window
- **THEN** only the in-window downtime records MUST appear as detail lines

#### Scenario: Window size is configurable

- **WHEN** `sla.downtime.recent-window-days` is set to a positive integer N
- **THEN** the recent window MUST be `[now - N days, now]` based on downtime start time

### Requirement: Remark when historical downtime exists outside the window

When a system has one or more downtime records outside the recent window, the system SHALL attach a remark for that system stating that earlier months also have downtime details (and SHOULD include the count of omitted historical records). The remark MUST NOT expand the full historical detail list.

#### Scenario: In-window details plus historical remark

- **WHEN** a system has in-window downtime and also out-of-window downtime
- **THEN** the output MUST list the in-window rows AND include a remark that earlier periods also have downtime details not expanded

#### Scenario: Only historical downtime

- **WHEN** a system has no in-window downtime but has out-of-window downtime
- **THEN** the output MUST omit in-window detail lines AND include a remark that there is no downtime in the recent window while earlier months have downtime details

#### Scenario: No historical outside window

- **WHEN** all matched downtime for a system falls inside the recent window
- **THEN** the system MUST NOT show the historical-months remark

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

### Requirement: WeCom webhook is configurable

The system SHALL send WeCom notifications to the webhook URL configured in application settings.

#### Scenario: Updated webhook target

- **WHEN** `wecom.webhook-url` is set to the new robot endpoint and notify is triggered with WeCom enabled
- **THEN** the HTTP request MUST be posted to that configured URL

### Requirement: No scheduled SLA WeCom notify

The system SHALL NOT register or execute a scheduled job that invokes the **annual-SLI** notify pipeline (`SlaReviewNotifyService.notifyNow` / `POST /api/sla/notify`). WeCom messages for that annual-SLI review list MUST be sent only as a result of `POST /api/sla/notify` (or an equivalent manual trigger of the same pipeline). The application MUST NOT send that annual-SLI WeCom payload solely because wall-clock time reached a cron slot. A separate weekly review-doc pipeline (Tencent Doc upload + WeCom with document URL) MAY run on its own schedule and MUST NOT be treated as the annual-SLI notify pipeline.

#### Scenario: Former cron times do not auto-push

- **WHEN** the process is running and the local clock reaches 10:00 or 17:00 on any calendar day, including a day in the last 7 calendar days of the month
- **THEN** the system MUST NOT invoke the annual-SLI notify pipeline and MUST NOT call the WeCom webhook for that pipeline unless a client has triggered `POST /api/sla/notify`

#### Scenario: Last day of month is not special

- **WHEN** it is the last calendar day of the month and no client has called `POST /api/sla/notify`
- **THEN** the system MUST NOT auto-execute the annual-SLI notify pipeline

### Requirement: Manual notify ignores calendar window

The HTTP notify API MUST remain available at any time. The absence of a scheduled SLA notify job MUST NOT disable or gate the manual notify API. Manual notify MUST continue to run the same notify pipeline, including owner matching and WeCom `@` mentions, as before this change.

#### Scenario: Manual notify mid-month

- **WHEN** a client calls `POST /api/sla/notify` on a day outside the last 7 calendar days of the month
- **THEN** the system MUST still run the notify pipeline according to WeCom settings

#### Scenario: Manual notify on last day of month

- **WHEN** a client calls `POST /api/sla/notify` on the last calendar day of the month
- **THEN** the system MUST still run the notify pipeline according to WeCom settings

#### Scenario: No schedule switch required for manual notify

- **WHEN** `sla.schedule.enabled` is unset or the schedule configuration no longer exists and a client calls `POST /api/sla/notify`
- **THEN** the system MUST still attempt notify as today

### Requirement: Load exclusion reason on downtime events

When loading downtime rows from leaf and parent dashboards, the system SHALL capture the exclusion-reason column (configurable, default `排除原因`) onto each downtime event in addition to the existing exclusion-status field.

#### Scenario: Reason field populated

- **WHEN** a downtime row contains an exclusion-reason cell
- **THEN** the corresponding downtime event MUST store that trimmed value (empty string when blank)

### Requirement: Classify downtime review status for the current calendar month

For each low-SLI system, the system SHALL consider downtime events whose start time falls in the current calendar month in `Asia/Shanghai`. A downtime event is **unreviewed** when its exclusion status equals `未排除` OR its exclusion reason equals `-` (literal hyphen after trim). A downtime event is **reviewed** when it is not unreviewed. Missing or unparsable status/reason needed for a conclusive reviewed decision MUST be treated as unreviewed. The system is **month-fully-reviewed** only when it has at least one current-month downtime event and every current-month event is reviewed.

#### Scenario: Unreviewed when status is 未排除

- **WHEN** a current-month downtime has exclusion status `未排除` (regardless of reason)
- **THEN** that event MUST be classified as unreviewed

#### Scenario: Unreviewed when reason is hyphen

- **WHEN** a current-month downtime has exclusion reason `-` (regardless of status, including non-未排除 statuses)
- **THEN** that event MUST be classified as unreviewed

#### Scenario: Reviewed when neither condition holds

- **WHEN** a current-month downtime has exclusion status not equal to `未排除` AND exclusion reason not equal to `-`
- **THEN** that event MUST be classified as reviewed

#### Scenario: Month fully reviewed only if all current-month events reviewed

- **WHEN** a system has two or more current-month downtime events and every one is reviewed
- **THEN** the system MUST be marked month-fully-reviewed

#### Scenario: Any unreviewed current-month event blocks full review

- **WHEN** a system has at least one current-month unreviewed downtime event
- **THEN** the system MUST NOT be marked month-fully-reviewed

### Requirement: Skip WeCom notify for month-fully-reviewed systems

When sending WeCom notify (`POST /api/sla/notify`), the system SHALL omit low-SLI systems that are month-fully-reviewed from the WeCom summary payload and from owner @ mentions derived from that send. Systems with annual SLI below threshold that have **no** current-month downtime events MUST still be included in the notify set. Preview MUST NOT remove systems based on this filter.

#### Scenario: Fully reviewed system omitted from notify

- **WHEN** notify runs and a low-SLI system is month-fully-reviewed
- **THEN** that system MUST NOT appear in the WeCom markdown sent to the webhook and MUST NOT contribute an @ mobile solely for that omitted system

#### Scenario: No current-month downtime still notifies

- **WHEN** a low-SLI system has no downtime starting in the current calendar month
- **THEN** notify MUST still include that system when building the WeCom payload

#### Scenario: Preview keeps fully reviewed systems

- **WHEN** a client calls preview and a low-SLI system is month-fully-reviewed
- **THEN** that system MUST still appear in the preview result items

#### Scenario: Preview exposes skip flag

- **WHEN** preview returns items
- **THEN** each item SHOULD expose whether WeCom notify would skip it due to month-full review (e.g. boolean flag) so operators can verify
