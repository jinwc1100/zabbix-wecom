## MODIFIED Requirements

### Requirement: No scheduled SLA WeCom notify

The system SHALL NOT register or execute a scheduled job that invokes the **annual-SLI** notify pipeline (`SlaReviewNotifyService.notifyNow` / `POST /api/sla/notify`). WeCom messages for that annual-SLI review list MUST be sent only as a result of `POST /api/sla/notify` (or an equivalent manual trigger of the same pipeline). The application MUST NOT send that annual-SLI WeCom payload solely because wall-clock time reached a cron slot. A separate weekly review-doc pipeline (Tencent Doc upload + WeCom with document URL) MAY run on its own schedule and MUST NOT be treated as the annual-SLI notify pipeline.

#### Scenario: Former cron times do not auto-push

- **WHEN** the process is running and the local clock reaches 10:00 or 17:00 on any calendar day, including a day in the last 7 calendar days of the month
- **THEN** the system MUST NOT invoke the annual-SLI notify pipeline and MUST NOT call the WeCom webhook for that pipeline unless a client has triggered `POST /api/sla/notify`

#### Scenario: Last day of month is not special

- **WHEN** it is the last calendar day of the month and no client has called `POST /api/sla/notify`
- **THEN** the system MUST NOT auto-execute the annual-SLI notify pipeline
