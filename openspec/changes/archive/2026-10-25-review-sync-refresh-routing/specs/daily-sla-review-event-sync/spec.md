# Spec Delta

## ADDED Requirements

### Requirement: Manual POST accepts refreshRouting

The POST MUST accept a query parameter `refreshRouting` whose default is false. Cron MUST NOT use `refreshRouting`.

#### Scenario: Default is false

- **WHEN** a client calls `POST /api/sla/review-event-sync` without `refreshRouting`
- **THEN** the system MUST NOT run dictionary routing backfill for that request

### Requirement: refreshRouting runs startup backfill

When `refreshRouting` is true, the system MUST run the same monitor-edition backfill as application startup before any other work on that request that collects Grafana downtime or refreshes `sla_review_event` rows.

#### Scenario: Routing only

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshRouting=true` with `refreshOwner` and `refreshExcludeTime` omitted or false
- **THEN** the system MUST run dictionary routing backfill and MUST NOT collect Grafana downtime for insert or owner refresh

#### Scenario: Routing before owner refresh

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshRouting=true&refreshOwner=true`
- **THEN** the system MUST complete routing backfill before updating `sla_review_event.owner_name` from the dictionary

#### Scenario: Routing with exclude_time refresh

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshRouting=true&refreshExcludeTime=true`
- **THEN** the system MUST run routing backfill first and MUST then recompute `exclude_time` on existing rows without collecting Grafana downtime

### Requirement: refreshRouting response includes routing stats

When `refreshRouting` is true and backfill updates at least one dictionary row, the sync result MUST include a count of routing rows updated and the human-readable message MUST mention routing refresh.

#### Scenario: Response reports routing stats

- **WHEN** a client calls `POST /api/sla/review-event-sync?refreshRouting=true` and backfill updates at least one dictionary row
- **THEN** the sync result MUST include a count of routing rows updated and the message MUST mention routing refresh
