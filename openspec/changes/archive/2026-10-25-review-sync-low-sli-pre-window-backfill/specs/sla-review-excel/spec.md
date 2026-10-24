# Spec Delta

## ADDED Requirements

### Requirement: Low-SLI sheet shares data rules with Pass B

The low-SLI review sheet and review-event Pass B MUST use the same annual SLI threshold, low-SLI inventory, full-panel downtime index query, dashboard matching, and exclusion filter (`未排除` or `-`).

#### Scenario: Excel row eligible implies Pass B candidate when pre-window

- **WHEN** a row appears on the low-SLI review sheet, its downtime start is before configured window-start, and no table row exists for that start identity
- **THEN** the next successful default review-event sync Pass B MUST insert that row unless Grafana data changed between export and sync

### Requirement: Excel export ignores window-start for row eligibility

The low-SLI review sheet MUST NOT drop rows because downtime start is before configured `weekly-review.window-start`. Pass B alone applies that filter for table inserts.

#### Scenario: Excel export not filtered by window-start

- **WHEN** a low-SLI system has an unexcluded downtime starting before configured window-start
- **THEN** that row MUST remain eligible for the low-SLI review sheet
