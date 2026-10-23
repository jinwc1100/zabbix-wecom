## MODIFIED Requirements

### Requirement: Fill local review template and resolve owners

The system SHALL copy `sla-wecom-notifier/doc/复盘明细.xlsx` and write working-set rows into the data sheet by matching header names. Each output row MUST set: `父/子系统` (source level `子系统` or `父系统`), `系统名称`, `宕机开始时间`, `宕机结束时间`, and `责任人` from `sla_system_owner` matched on that level and system name (same dictionary as low-SLI notify). The system MUST NOT use `owners.rules`. The system MUST NOT overwrite helper sheets used for dropdowns or fault-category instructions when those sheets exist.

#### Scenario: Five columns populated

- **WHEN** a working-set row is written
- **THEN** the five named columns MUST be filled and owner MUST equal the dictionary `owner_name` for that `system_level` and `system_name`

#### Scenario: Missing template fails the job

- **WHEN** the template file is absent
- **THEN** the job MUST fail without uploading a Tencent Doc and without sending a success WeCom message

### Requirement: WeCom notify with document URL and owner mentions

After a successful upload, the system SHALL send a WeCom message that includes the Tencent Doc URL and SHALL @ owners who have a resolved mobile in the working set from `sla_system_owner`, using the existing WeCom mention mechanism. The annual-SLI compact markdown pipeline MUST NOT be used for this message.

#### Scenario: Success notify contains URL

- **WHEN** upload succeeds and WeCom is enabled
- **THEN** the WeCom content MUST include the online document URL

#### Scenario: Mentions from working set

- **WHEN** at least one included row has a resolved owner mobile from the dictionary
- **THEN** the WeCom mention list MUST include that mobile
