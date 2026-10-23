## ADDED Requirements

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

## MODIFIED Requirements

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
