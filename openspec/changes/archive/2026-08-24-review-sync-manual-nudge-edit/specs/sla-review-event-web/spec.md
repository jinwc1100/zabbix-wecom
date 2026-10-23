## ADDED Requirements

### Requirement: Saved row edit then save

The Vue review page SHALL treat a row as already saved when `count_sla` is non-blank (`是` or `否`). For an already-saved row that is not in edit mode, human fields (fault category, subcategory, description, resolution process, root cause, improvement) MUST be read-only and the page MUST offer an edit control instead of persisting from the locked state. Entering edit mode MUST enable those human fields. Saving MUST still use the per-row PUT and MUST require all six fields; after a successful save the row MUST return to read-only. Rows with empty `count_sla` MUST remain editable without requiring the edit control first. Machine fields MUST stay read-only in all modes. The served `/review/` page and the demo HTML MUST behave the same.

#### Scenario: Saved row locked until edit

- **WHEN** a listed row has `count_sla` `是` and the user has not entered edit mode
- **THEN** the human fields MUST NOT be editable and using the save control MUST NOT persist until the user enters edit mode

#### Scenario: Edit then save persists

- **WHEN** the user enters edit mode on a saved row, changes a required field, keeps all six fields filled, and saves
- **THEN** the PUT MUST persist the new values and the row MUST become read-only again

#### Scenario: Never-saved row needs no edit click

- **WHEN** a listed row has empty `count_sla`
- **THEN** the user MUST be able to fill and save without first activating the edit control
