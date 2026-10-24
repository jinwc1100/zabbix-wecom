# Tasks

## 1. Shared low-SLI downtime matching

- [x] 1.1 Extract shared helper for low-SLI system list + leaf/parent index expansion with `未排除`/`-` filter (reuse or relocate `ReviewExcelService.isUnexcluded`) and verify unit test matches Excel `expandRows` output keys for a fixed fixture
- [x] 1.2 Refactor `ReviewExcelService` to call the shared helper and verify existing review Excel tests still pass (`mvn -q -pl sla-wecom-notifier test -Dtest=ReviewExcelServiceTest`)

## 2. Pass B in review-event sync

- [x] 2.1 Extend `ReviewEventSyncResult` with Pass B counters and wire human-readable message segments; verify serialization in controller test if applicable
- [x] 2.2 Implement Pass B in `ReviewEventSyncService` (before Pass A, default insert path only): full-panel indexes, pre-window filter, insert-only via existing mapper/insert; verify `ReviewEventSyncServiceTest` covers insert, skip existing, partial exclusion skip, and refreshOwner skips Pass B
- [x] 2.3 Add integration-style test or mapper test that Pass B runs before Pass A when both apply (order assertion via mocks or call sequence)

## 3. Documentation and ops

- [x] 3.1 Update `sla-wecom-notifier/README.md` with Pass B behavior, first-run manual POST note, and verify wording matches spec deltas
- [x] 3.2 Run full module tests (`mvn -q -pl sla-wecom-notifier test`) and verify green

## Workflow follow-up

- After review, run `/opsx-apply` implementation if not already done via tasks above, then archive when deployed.
