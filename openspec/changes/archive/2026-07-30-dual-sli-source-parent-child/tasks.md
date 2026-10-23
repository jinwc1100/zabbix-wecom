## 1. Spike and configuration

- [x] 1.1 Inspect Grafana dashboard `aftk5ryifcfeof` for panel ids and name/SLI column names
- [x] 1.2 Add `grafana.sli-sources` in `application.yml` for subsystem (`afj5c5o5v3rb4f`) and parent (`aftk5ryifcfeof`) with `system-level`, panels, name-columns
- [x] 1.3 Extend `GrafanaProperties` for `sli-sources` (keep optional legacy fallback if useful)

## 2. Collect and label (Plan B)

- [x] 2.1 Extend `SlaLowSliItem` with source level from SLI source
- [x] 2.2 Update `AnnualSliService` to iterate all `sli-sources`, filter `年度SLI < threshold`, union results
- [x] 2.3 Update `DowntimeMatchService` to keep level from SLI source and match downtime only against the corresponding parent/leaf index

## 3. Docs and verify

- [x] 3.1 Note dual SLI sources and Plan B level behavior in README
- [x] 3.2 Rebuild; preview and confirm combined count and `父系统`/`子系统` labels without forcing `未知` when downtime missing
- [x] 3.3 Spot-check notify still uses summary markdown + owner @ unchanged
