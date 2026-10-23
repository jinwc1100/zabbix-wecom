## REMOVED Requirements

### Requirement: Resolve owner by region and system type rules

**Reason**: Owner assignment is an exact dictionary lookup on `(system_level, system_name)`, not region/type rules from `doc/list.txt` or `owners.rules`.

**Migration**: Use `system-owner-dictionary` resolution. Operators maintain `sla_system_owner`.

### Requirement: Trade-owner region fallback for non-bid or unmatched systems

**Reason**: No region fallback after dictionary lookup.

**Migration**: Insert or update the dictionary row for that level and name.

### Requirement: Beijing roster split by bid vs trade vs two municipal names

**Reason**: Beijing owners are explicit rows in the dictionary (from `responsible_persons.xlsx`), not keyword rules.

**Migration**: Dictionary rows for those Grafana names.

### Requirement: Audio-video systems resolve to Zhang Hongtao

**Reason**: Audio-video owners are dictionary rows, not a `音视频` contains rule.

**Migration**: Dictionary rows whose names include those systems.

## MODIFIED Requirements

### Requirement: Mention matched owners on WeCom notify

When notifying via WeCom, the system SHALL send the summary Markdown and then a text message that @-mentions the distinct mobile numbers of owners resolved for systems in that report from `sla_system_owner` (same dictionary as review-event sync).

#### Scenario: Deduplicate mobiles

- **WHEN** multiple systems resolve to the same owner mobile
- **THEN** that mobile MUST appear at most once in `mentioned_mobile_list`

#### Scenario: Preview does not mention

- **WHEN** a client calls preview
- **THEN** the system MUST NOT send WeCom messages, and SHOULD still expose resolved owner name/mobile on each item for verification
