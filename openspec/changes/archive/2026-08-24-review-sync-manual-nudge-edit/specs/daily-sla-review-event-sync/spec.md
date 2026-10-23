## MODIFIED Requirements

### Requirement: Seventeen-thirty schedule and manual POST

The system SHALL register a daily job at 17:30 `Asia/Shanghai` that runs this sync when `review-sync.enabled` is true. The shipped default for `review-sync.enabled` MUST be true. Disabling the switch MUST skip the scheduled sync and MUST skip the incomplete-review WeCom nudge from cron. The system SHALL expose an HTTP POST that runs the same sync regardless of the switch. After a successful scheduled sync, the system MUST invoke the incomplete-review WeCom nudge. After a successful POST sync, the system MUST invoke the same incomplete-review WeCom nudge. If sync fails, the system MUST NOT send the nudge. `POST /api/sla/notify` MUST remain the annual-SLI pipeline and MUST NOT write `sla_review_event`.

#### Scenario: Cron at 17:30 when enabled

- **WHEN** `review-sync.enabled` is true and the clock is 17:30
- **THEN** the system MUST run the review-event sync

#### Scenario: Scheduled sync then nudge

- **WHEN** the scheduled sync completes successfully
- **THEN** the system MUST run the incomplete-review WeCom nudge

#### Scenario: Switch off skips cron

- **WHEN** `review-sync.enabled` is false and 17:30 arrives
- **THEN** the system MUST NOT run this sync from cron and MUST NOT send the incomplete-review WeCom nudge from cron

#### Scenario: Manual POST ignores switch and nudges after success

- **WHEN** a client calls `POST /api/sla/review-event-sync` while the switch is disabled and the sync succeeds
- **THEN** the system MUST still run the sync and MUST run the incomplete-review WeCom nudge

#### Scenario: Failed sync does not nudge

- **WHEN** the review-event sync throws or otherwise fails (scheduled or POST)
- **THEN** the system MUST NOT send the incomplete-review WeCom nudge
