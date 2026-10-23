## 1. Backfill service

- [x] 1.1 List reviewed events (`count_sla` 是/否); resolve pool + `slaid`; classify overlap / no-sla / unexcluded using Grafana window rule
- [x] 1.2 `dryRun=true`: no writes; `dryRun=false`: `writeAfterSave` per unexcluded+slaid row, continue on per-row failure

## 2. HTTP

- [x] 2.1 `POST /api/sla/excluded-downtime-backfill` with default `dryRun=true`; JSON counts and lists; no cron

## 3. Tests

- [x] 3.1 Tests: dry-run lists unexcluded and does not call writer; missing sla skipped; execute calls writer; already overlapping not inserted; `mvn test`
