## 1. Assets and config

- [x] 1.1 Add `sla-wecom-notifier/doc/复盘明细.xlsx` to the repo (required template)
- [x] 1.2 Add weekly-review + Tencent Docs properties (enabled, cron, zone, window-start, template path, API credentials via env)
- [x] 1.3 Re-enable `@EnableScheduling` only for the new weekly job (do not restore last-7-days SLI cron)

## 2. Query and template

- [x] 2.1 Collect leaf+parent downtime, filter start ∈ [window-start, trigger] and exclusion `未排除`/`-`; do not filter by annual SLI
- [x] 2.2 Fill template columns 父/子系统、系统名称、开始、结束、责任人 via existing `OwnerRuleResolver`; leave dropdown/instruction sheets intact
- [x] 2.3 Unit-test window, exclusion, and title `复盘明细-yyyyMMdd`

## 3. Tencent Docs Open API

- [x] 3.1 Spike: import a filled xlsx via Open API; record whether dropdowns survive; fallback in design if not
- [x] 3.2 Implement import client: upload workbook, set title, return shareable URL; fail the job on import error

## 4. Notify, schedule, HTTP

- [x] 4.1 After success, send WeCom markdown with doc URL and @ mobiles from the working set (reuse mention helper; do not use SLI compact markdown)
- [x] 4.2 Add Friday 11:00 scheduler gated by enabled switch
- [x] 4.3 Add `POST /api/sla/weekly-review-doc` that runs the same pipeline even when the switch is off
- [x] 4.4 Confirm `POST /api/sla/notify` is unchanged
