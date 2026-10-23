## ADDED Requirements

### Requirement: No scheduled SLA WeCom notify

The system SHALL NOT register or execute a scheduled job that invokes the SLA notify pipeline. WeCom messages for SLA review MUST be sent only as a result of the HTTP notify API (or an equivalent manual trigger that calls the same pipeline). The application MUST NOT send a WeCom SLA review message solely because wall-clock time reached a configured cron slot.

#### Scenario: Former cron times do not auto-push

- **WHEN** the process is running and the local clock reaches 10:00 or 17:00 on any calendar day, including a day in the last 7 calendar days of the month
- **THEN** the system MUST NOT invoke the notify pipeline and MUST NOT call the WeCom webhook unless a client has triggered the HTTP notify API

#### Scenario: Last day of month is not special

- **WHEN** it is the last calendar day of the month and no client has called `POST /api/sla/notify`
- **THEN** the system MUST NOT auto-execute the notify pipeline

## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Auto-notify only during last 7 calendar days of month

**Reason**: Month-end automatic WeCom pushes are no longer wanted; review notify is on-demand only (plan A: remove the job, do not widen the calendar window).

**Migration**: When a push is needed, call `POST /api/sla/notify`. Remove `sla.schedule` from application configuration. Do not rely on 10:00/17:00 cron during the last 7 calendar days of the month.
