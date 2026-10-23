## ADDED Requirements

### Requirement: List page applies template fields to checked rows

The Vue review list at `/review/events.html` and the matching demo HTML MUST provide per-row checkboxes and a page-level control to apply a template row’s human fields onto checked rows on the **current page only**.

The template MUST be the row the user marked as template. If none is marked, the template MUST be the first row in current page order whose six required fields are all non-blank after trim (fault category, subcategory, description, resolution process, root cause, improvement). If no such row exists, apply MUST NOT change any row and MUST tell the user to fill a template first.

Apply MUST copy those six fields onto every checked row whose `id` is not the template’s `id`. Apply MUST set `count_sla` from the fault-category dictionary for the copied pair (same as changing subcategory). Apply MUST NOT copy `system_name`, times, owner, or `id`. Apply MUST NOT call the save API and MUST NOT write `sla_excluded_downtime`. After apply, those target rows MUST be editable and marked dirty. Apply MUST NOT use each prior target as the source for the next (no chain of empty rows).

If any target already has at least one of the six fields non-blank, the page MUST ask for confirmation before overwriting; cancel MUST leave all rows unchanged.

#### Scenario: Fill first then apply to the next two

- **WHEN** row 1 has all six fields filled, rows 2 and 3 are blank, the user checks rows 2 and 3, and applies with no explicit template other than the default
- **THEN** rows 2 and 3 MUST have the same six fields (and dictionary `count_sla`) as row 1, and the list API MUST NOT have been called for save

#### Scenario: Apply does not write exclusion

- **WHEN** the user applies a template to checked rows
- **THEN** the system MUST NOT insert or update `sla_excluded_downtime`

#### Scenario: No complete template

- **WHEN** no current-page row has all six fields filled and the user applies
- **THEN** no row’s fields MUST change

#### Scenario: Explicit template row is highlighted

- **WHEN** the user marks a row as template
- **THEN** that row (including its expanded review panel if open) MUST use a pale-blue full-row wash distinct from the unsaved dirty background, MUST NOT use a brand-color left accent bar, and that row’s template control MUST show as the current template with an outline style distinct from the primary solid button

#### Scenario: Default template is not pre-highlighted

- **WHEN** a current-page row is complete but the user has not marked any template
- **THEN** no row MUST use the template highlight class

#### Scenario: Switching template moves the highlight

- **WHEN** the user marks row A as template then marks row B
- **THEN** only row B MUST keep the template highlight

### Requirement: List page batch-saves checked complete rows

The same list pages MUST provide a control to save checked rows on the current page. For each checked row in page order that has all six required fields non-blank, the system MUST perform the same persist as the existing per-row save (including deriving `count_sla` and `exclude_time`, and writing `sla_excluded_downtime` via the existing save path). Checked rows that are incomplete MUST be skipped and MUST NOT be sent. The per-row six-field requirement MUST NOT be relaxed.

Saves MUST run one row after another. A failure on one row MUST NOT roll back already succeeded rows and MUST NOT skip remaining complete checked rows. After the pass the page MUST report how many succeeded, how many were skipped as incomplete, and which ids failed. Unchecked complete rows MUST NOT be saved by this control.

#### Scenario: Three checked complete rows save like three single saves

- **WHEN** three checked rows are complete and the user batch-saves
- **THEN** each MUST be persisted as if saved individually, including one exclusion write per successful save keyed by that row’s system and `down_start`

#### Scenario: Incomplete checked rows are skipped

- **WHEN** two rows are checked, only the first is complete, and the user batch-saves
- **THEN** the first MUST be saved and the second MUST NOT be sent to the save API

#### Scenario: Later row still saves after an earlier failure

- **WHEN** the first of two complete checked rows fails to save and the second would succeed
- **THEN** the second MUST still be saved and the first MUST remain unsaved
