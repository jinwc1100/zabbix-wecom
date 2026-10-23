## ADDED Requirements

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

When sending WeCom notify (scheduled or manual `POST /api/sla/notify`), the system SHALL omit low-SLI systems that are month-fully-reviewed from the WeCom summary payload and from owner @ mentions derived from that send. Systems with annual SLI below threshold that have **no** current-month downtime events MUST still be included in the notify set. Preview MUST NOT remove systems based on this filter.

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
