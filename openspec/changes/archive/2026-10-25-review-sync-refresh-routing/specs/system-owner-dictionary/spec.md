# Spec Delta

## ADDED Requirements

### Requirement: Manual POST may trigger monitor edition backfill

In addition to application startup, the system MUST allow operators to trigger the monitor-edition backfill defined for `sla_system_owner` by calling `POST /api/sla/review-event-sync?refreshRouting=true`. That invocation MUST apply the same rules as startup backfill (Grafana panel 4/2 mapping, collision skip, unmatched rows default to `老版监控`/`zabbix6`, Grafana discovery failure behavior, no change to owner name or mobile). The dictionary table MUST remain on the zabbix6 pool only.

#### Scenario: Manual trigger matches startup rules

- **WHEN** a subsystem name appears on panel 2 of dashboard `afj5c5o5v3rb4f` and an operator calls `POST /api/sla/review-event-sync?refreshRouting=true`
- **THEN** that dictionary row’s `monitor_edition` MUST be `新版监控` and `write_jdbc_key` MUST be `zabbix7` as it would after startup backfill

#### Scenario: Manual trigger does not change owner fields

- **WHEN** an operator has set `owner_name` and `mobile` on a dictionary row and calls `POST /api/sla/review-event-sync?refreshRouting=true`
- **THEN** those fields MUST remain unchanged after backfill
