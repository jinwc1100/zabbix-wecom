## ADDED Requirements

### Requirement: Auto-notify only during last 7 calendar days of month

When scheduled notify is enabled, the system SHALL run a daily scheduled job at 17:00 in the configured timezone (default `Asia/Shanghai`). The job MUST invoke the same notify pipeline as manual notify only when the current local date falls within the last 7 calendar days of the current month (inclusive), i.e. `dayOfMonth >= lengthOfMonth - 6`. On all other days the scheduled job MUST skip sending and MUST NOT call the WeCom webhook.

#### Scenario: Fire on last day of month

- **WHEN** scheduled notify is enabled and the local clock is 17:00 on the last calendar day of the month
- **THEN** the system MUST execute the notify pipeline and push to WeCom when WeCom is enabled

#### Scenario: Fire on first day of the last-7 window

- **WHEN** scheduled notify is enabled and today is day `lengthOfMonth - 6` (e.g. day 25 in a 31-day month) at 17:00 local time
- **THEN** the system MUST execute the notify pipeline

#### Scenario: Skip outside the last-7 window

- **WHEN** scheduled notify is enabled and today is before day `lengthOfMonth - 6` at the scheduled time
- **THEN** the system MUST skip notify and MUST NOT send a WeCom message for that trigger

#### Scenario: Include weekends inside the window

- **WHEN** a Saturday or Sunday falls inside the last 7 calendar days of the month and the scheduled time arrives
- **THEN** the system MUST still execute the notify pipeline (weekends are not excluded)

### Requirement: Manual notify ignores calendar window

The HTTP notify API MUST remain available at any time and MUST NOT be gated by the last-7-days calendar window. Disabling `sla.schedule.enabled` MUST stop only the scheduled job, not the manual notify API.

#### Scenario: Manual notify mid-month

- **WHEN** a client calls `POST /api/sla/notify` on a day outside the last 7 calendar days of the month
- **THEN** the system MUST still run the notify pipeline according to WeCom settings

#### Scenario: Schedule disabled does not block manual notify

- **WHEN** `sla.schedule.enabled` is false and a client calls `POST /api/sla/notify`
- **THEN** the system MUST still attempt notify as today
