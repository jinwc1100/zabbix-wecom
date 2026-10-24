## MODIFIED Requirements

### Requirement: Daily 17:00 schedule and manual POST

The system SHALL register a scheduled job that runs the review-event Grafana sync when `review-sync.enabled` is true. The shipped default cron MUST be hourly at second 0 minute 0 (`0 0 * * * *`) in `review-sync.zone` (shipped default `Asia/Shanghai`). The shipped default for `review-sync.enabled` MUST be true. Disabling the switch MUST skip the scheduled sync and MUST skip the incomplete-review WeCom nudge from cron. The system SHALL expose an HTTP POST that runs the same sync regardless of the switch.

After a successful **scheduled** sync, the system MUST invoke the incomplete-review WeCom nudge if and only if the job’s start instant in `review-sync.zone` has hour `17`. Scheduled syncs whose start hour is not 17 MUST NOT send that nudge. If the scheduled sync fails, the system MUST NOT send the nudge.

The POST MUST accept a query parameter `notify` whose default is false: when omitted or false the system MUST sync and MUST NOT send the incomplete-review WeCom nudge; when true and the sync succeeds, the system MUST invoke the same incomplete-review WeCom nudge. If POST sync fails, the system MUST NOT send the nudge. `POST /api/sla/notify` MUST remain the annual-SLI pipeline and MUST NOT write `sla_review_event`.

#### Scenario: Cron at 17:00 when enabled

- **WHEN** `review-sync.enabled` is true and the clock is 17:00 in the review-sync zone
- **THEN** the system MUST run the review-event sync

#### Scenario: Hourly cron syncs when enabled

- **WHEN** `review-sync.enabled` is true and the clock is 10:00 in the review-sync zone
- **THEN** the system MUST run the review-event sync and MUST NOT send the incomplete-review WeCom nudge

#### Scenario: Scheduled sync then nudge

- **WHEN** the scheduled sync whose start hour is 17 in the review-sync zone completes successfully
- **THEN** the system MUST run the incomplete-review WeCom nudge

#### Scenario: Switch off skips cron

- **WHEN** `review-sync.enabled` is false and a scheduled fire arrives (including 17:00)
- **THEN** the system MUST NOT run this sync from cron and MUST NOT send the incomplete-review WeCom nudge from cron

#### Scenario: Manual POST ignores switch and does not nudge by default

- **WHEN** a client calls `POST /api/sla/review-event-sync` without `notify` or with `notify=false`, while the switch is disabled, and the sync succeeds
- **THEN** the system MUST still run the sync and MUST NOT send the incomplete-review WeCom nudge

#### Scenario: Manual POST nudges when notify is true

- **WHEN** a client calls `POST /api/sla/review-event-sync?notify=true` and the sync succeeds
- **THEN** the system MUST run the incomplete-review WeCom nudge

#### Scenario: Failed sync does not nudge

- **WHEN** the review-event sync throws or otherwise fails (scheduled or POST)
- **THEN** the system MUST NOT send the incomplete-review WeCom nudge
