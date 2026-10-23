## ADDED Requirements

### Requirement: Associate low-SLI systems with downtime details

The system SHALL query systems whose annual SLI is below the configured threshold, match each system name against Grafana leaf downtime and parent downtime dashboards, and produce downtime rows that include system name, annual SLI value, downtime start time, downtime end time, and system level (`父系统` or `子系统`).

#### Scenario: Parent dashboard match labels parent

- **WHEN** a low-SLI system name matches a row in the parent downtime dashboard
- **THEN** the system level MUST be `父系统` and end time MUST come from the parent dashboard end column

#### Scenario: Leaf dashboard match labels subsystem

- **WHEN** a low-SLI system name matches a row in the leaf downtime dashboard
- **THEN** the system level MUST be `子系统` and end time MUST come from the leaf recovery/end column (including literal `未恢复` when applicable)

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

The system SHALL render a Markdown document containing the five fields and any remarks, expose it on preview without sending WeCom, and use the identical Markdown when notifying via WeCom webhook.

#### Scenario: Preview returns markdown

- **WHEN** a client calls the SLA preview API successfully
- **THEN** the response MUST include a `markdown` field with system name, annual SLI, downtime start/end, system level, and remarks when applicable

#### Scenario: Notify uses preview markdown

- **WHEN** a client triggers notify and WeCom is enabled
- **THEN** the webhook payload content MUST match the Markdown produced for that same result set
