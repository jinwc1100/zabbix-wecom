## 1. Configuration and models

- [x] 1.1 Extend `application.yml` with annual/leaf/parent dashboard settings, column mappings, `sla.downtime.recent-window-days` (default 30), remark templates, and unmatched policy
- [x] 1.2 Bind new properties in `GrafanaProperties` / `SlaDowntimeProperties` (or equivalent)
- [x] 1.3 Add `DowntimeNotifyItem` (name, sli, start, end, systemLevel, historicalCount, remark) and extend `NotifyResult` with `markdown` plus item list

## 2. Grafana query and matching

- [x] 2.1 Generalize `GrafanaClient` to fetch dashboard SQL by uid/panel and run `/api/ds/query`
- [x] 2.2 Implement annual low-SLI collection (reuse existing filter threshold logic)
- [x] 2.3 Implement downtime query for leaf + parent panels; index by system name; normalize timestamps; dedupe `(name,start,end)`
- [x] 2.4 Implement match + systemLevel labeling (`父系统` / `子系统`, `parent_first` on dual hit)
- [x] 2.5 Implement recent-window filter on downtime start time and compute out-of-window counts / remarks per system

## 3. Markdown and API

- [x] 3.1 Implement `SlaMarkdownFormatter` for grouped parent/subsystem Markdown with five fields and historical remarks
- [x] 3.2 Wire `SlaReviewNotifyService.preview` / `notifyNow` to full pipeline; put identical `markdown` on result
- [x] 3.3 Ensure `/api/sla/preview` and `/api/sla/notify` return the new fields without breaking health check

## 4. Verification

- [x] 4.1 Run preview against live Grafana; confirm in-window-only detail lines and remarks when older outages exist
- [x] 4.2 Spot-check parent vs subsystem counts and sample systems from both downtime dashboards
- [x] 4.3 Update `README.md` with new config keys and output field meanings
- [x] 4.4 Remove temporary instruction scratch files under the change directory if any
