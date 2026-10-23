## MODIFIED Requirements

### Requirement: Per-row save with required human fields

The system SHALL update one `sla_review_event` row by `id` with fault category, subcategory, description, resolution process, root cause, and improvement. All six MUST be non-blank after trim or the update MUST fail without writing. The system MUST resolve `count_sla` from `sla_fault_category` for the category+subcategory pair and MUST reject unknown pairs. The update MUST NOT change `system_level`, `system_name`, `down_start`, `down_end`, `owner_name`, or `period_month`. On a successful update the system MUST also persist `exclude_time` using the save-derivation rules for that row’s `count_sla`, `down_start`, and `down_end`. After that update the system MUST write `sla_excluded_downtime` as specified by `review-excluded-downtime-write`. If that write fails, the save request MUST fail.

#### Scenario: Complete row saves

- **WHEN** a client PUTs a known id with all six fields filled and a valid category pair
- **THEN** the row MUST persist those fields and `count_sla` from the dictionary

#### Scenario: Missing long text rejected

- **WHEN** a client PUTs with category, subcategory, and description filled but empty `root_cause`
- **THEN** the system MUST NOT update the row and MUST return a client error

#### Scenario: Unknown subcategory rejected

- **WHEN** a client PUTs a category and subcategory that are not in `sla_fault_category`
- **THEN** the system MUST NOT update the row and MUST return a client error

#### Scenario: Exclusion write failure fails save

- **WHEN** human fields are valid but inserting `sla_excluded_downtime` fails
- **THEN** the save API MUST return an error
