## 1. Config and mapping

- [x] 1.1 Add `review-sync` properties (enabled default false, cron `0 0 17 * * *`, zone Asia/Shanghai)
- [x] 1.2 Add MyBatis insertIgnore for `sla_review_event` (period_month, level, name, start, end, owner only)
- [x] 1.3 Map blank/`未恢复` end times to `9999-12-31 00:00:00`; derive period_month from start `yyyyMM`

## 2. Sync pipeline

- [x] 2.1 Service: `WeeklyReviewDataService.collect` then INSERT IGNORE each row; skip unparseable starts; never update category columns
- [x] 2.2 Restore `@EnableScheduling` and a 17:00 scheduler gated by enabled
- [x] 2.3 `POST /api/sla/review-event-sync` runs the same pipeline ignoring the switch
- [x] 2.4 Confirm `POST /api/sla/notify` does not write `sla_review_event`

## 3. Tests

- [x] 3.1 Unit-test period_month from start, unrecovered sentinel, and duplicate skip (no category overwrite)
