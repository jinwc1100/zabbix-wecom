## ADDED Requirements

### Requirement: Weekly unreviewed downtime window

When the weekly review-doc job runs (scheduled or manual HTTP), the system SHALL load downtime rows from both the subsystem downtime dashboard and the parent downtime dashboard. A row MUST be included only when all of the following hold: (1) downtime start time is parseable and lies in the closed interval from the configured window start (default `2026-07-30 00:00` in `Asia/Shanghai`) through the job trigger instant inclusive; (2) exclusion status is `未排除` or `-` (literal hyphen after trim). Annual SLI MUST NOT be used as an inclusion filter. Parent-dashboard rows and subsystem-dashboard rows MUST both be eligible (no collapsing of parent/child pairs).

#### Scenario: Start time inside window is included

- **WHEN** a subsystem downtime starts at `2026-07-30 13:14:54` and exclusion status is `未排除` and the job triggers later that day
- **THEN** that row MUST appear in the review-doc working set as level `子系统`

#### Scenario: Start time before window is excluded

- **WHEN** a downtime starts before `2026-07-30 00:00` in `Asia/Shanghai`
- **THEN** that row MUST NOT appear in the review-doc working set

#### Scenario: Reviewed exclusion status is omitted

- **WHEN** a downtime in the window has exclusion status `部分排除` or any value other than `未排除` or `-`
- **THEN** that row MUST NOT appear in the review-doc working set

#### Scenario: Parent and subsystem both listed

- **WHEN** the same outage period exists on both parent and subsystem dashboards and both pass the window and exclusion filters
- **THEN** the working set MUST contain both a `父系统` row and a `子系统` row

#### Scenario: Low SLI is not required

- **WHEN** a system has annual SLI at or above 99.9% but has an unexcluded downtime in the window
- **THEN** that downtime row MUST still be included

### Requirement: Fill local review template and resolve owners

The system SHALL copy `sla-wecom-notifier/doc/复盘明细.xlsx` and write working-set rows into the data sheet by matching header names. Each output row MUST set: `父/子系统` (source level `子系统` or `父系统`), `系统名称`, `宕机开始时间`, `宕机结束时间`, and `责任人` using the existing owner-rule resolver (same rules as current notify). The system MUST NOT change owner matching rules. The system MUST NOT overwrite helper sheets used for dropdowns or fault-category instructions when those sheets exist.

#### Scenario: Five columns populated

- **WHEN** a working-set row is written
- **THEN** the five named columns MUST be filled and owner MUST equal the name returned by the existing owner rules for that system name

#### Scenario: Missing template fails the job

- **WHEN** the template file is absent
- **THEN** the job MUST fail without uploading a Tencent Doc and without sending a success WeCom message

### Requirement: Upload via Tencent Docs Open API

The system SHALL upload the filled workbook through the Tencent Docs Open API (not Cursor MCP). The online file title MUST be `复盘明细-` concatenated with the trigger date in `yyyyMMdd` (`Asia/Shanghai`), with no hyphens in the date (example: `复盘明细-20260821`). The job MUST obtain a document URL for WeCom. Credentials MUST come from application configuration or environment, not from Cursor MCP login.

#### Scenario: Title format

- **WHEN** the job triggers on 2026-08-21 in `Asia/Shanghai`
- **THEN** the uploaded document title MUST be `复盘明细-20260821`

#### Scenario: Open API used

- **WHEN** the filled xlsx is ready
- **THEN** the system MUST call the configured Tencent Docs Open API import path and MUST NOT depend on Cursor MCP tools to create the file

### Requirement: WeCom notify with document URL and owner mentions

After a successful upload, the system SHALL send a WeCom message that includes the Tencent Doc URL and SHALL @ owners who have a resolved mobile in the working set, using the existing WeCom mention mechanism. The annual-SLI compact markdown pipeline MUST NOT be used for this message.

#### Scenario: Success notify contains URL

- **WHEN** upload succeeds and WeCom is enabled
- **THEN** the WeCom content MUST include the online document URL

#### Scenario: Mentions from working set

- **WHEN** at least one included row has a resolved owner mobile
- **THEN** the WeCom mention list MUST include that mobile

### Requirement: Friday schedule switch and manual POST

The system SHALL provide a scheduled job at 11:00 every Friday in `Asia/Shanghai` that invokes the weekly review-doc pipeline when the schedule switch is enabled. Disabling the switch MUST skip the scheduled run only. The system SHALL expose a manual HTTP POST that invokes the same pipeline regardless of the schedule switch. The annual-SLI endpoint `POST /api/sla/notify` MUST remain a separate pipeline.

#### Scenario: Friday 11:00 with switch on

- **WHEN** the schedule switch is enabled and the local clock is Friday 11:00
- **THEN** the system MUST run the weekly review-doc pipeline

#### Scenario: Switch off skips cron only

- **WHEN** the schedule switch is disabled and Friday 11:00 arrives
- **THEN** the system MUST NOT run the weekly review-doc pipeline from cron

#### Scenario: Manual POST ignores switch

- **WHEN** a client calls the weekly review-doc POST while the schedule switch is disabled
- **THEN** the system MUST still run the weekly review-doc pipeline

#### Scenario: SLI notify endpoint unchanged

- **WHEN** a client calls `POST /api/sla/notify`
- **THEN** the system MUST run the existing annual-SLI notify pipeline and MUST NOT upload a weekly Tencent review workbook as part of that call
