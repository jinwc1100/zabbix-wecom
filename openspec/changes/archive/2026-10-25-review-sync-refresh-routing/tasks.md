# Tasks

## 1. Backfill result API

- [x] 1.1 Add a small result type (rows updated, collision count, grafana discovery ok) from `MonitorEditionBackfillService.backfill()` and unit-test collision/success paths without changing backfill rules
- [x] 1.2 Extend `ReviewEventSyncResult` with routing fields and verify JSON serialization in an existing or new controller test

## 2. Review-event-sync wiring

- [x] 2.1 Add `refreshRouting` query param on `POST /api/sla/review-event-sync` and thread through `ReviewEventSyncService.run(...)`
- [x] 2.2 Implement execution order (routing → exclude / owner / default sync) and routing-only mode; verify with service tests for param combinations
- [x] 2.3 Append routing counts to sync `message`; verify message text in tests when `refreshRouting=true`

## 3. Documentation

- [x] 3.1 Update `sla-wecom-notifier/README.md` with SOP: after dictionary edits use `refreshRouting=true&refreshOwner=true` before review save; verify examples match controller param names

## 4. Validation

- [x] 4.1 Run `mvn test` in `sla-wecom-notifier` and confirm all tests pass
- [x] 4.2 Run `openspec validate review-sync-refresh-routing --strict` and confirm no errors

## Workflow follow-up

- Archive the change after review and merge per project process.
