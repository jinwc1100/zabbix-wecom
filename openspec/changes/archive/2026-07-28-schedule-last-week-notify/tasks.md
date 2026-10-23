## 1. Configuration

- [x] 1.1 Update `sla.schedule` in `application.yml`: `enabled: true`, `cron: "0 0 17 * * ?"`, optional `zone: Asia/Shanghai`, `last-days-of-month: 7`
- [x] 1.2 Extend `SlaScheduleProperties` for `zone` and `lastDaysOfMonth`

## 2. Calendar gate

- [x] 2.1 Implement last-N-days-of-month check (`dayOfMonth >= lengthOfMonth - (N - 1)`)
- [x] 2.2 Update `SlaNotifyScheduler` to use cron zone + gate before `notifyNow()`; log skip outside window
- [x] 2.3 Ensure gate is NOT applied inside `SlaReviewNotifyService.notifyNow()` (manual API unrestricted)

## 3. Docs and verify

- [x] 3.1 Document schedule window / timezone / manual bypass in README
- [x] 3.2 Rebuild; spot-check gate logic for 31/30/28-day months (unit test or quick main/assert)
- [x] 3.3 Confirm manual `POST /api/sla/notify` still works while schedule enabled
