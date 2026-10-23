## 1. Remove schedule runtime

- [x] 1.1 Delete `SlaNotifyScheduler` and `LastDaysOfMonthGate`
- [x] 1.2 Delete `LastDaysOfMonthGateTest`
- [x] 1.3 Delete `SlaScheduleProperties` and remove it from `@EnableConfigurationProperties`
- [x] 1.4 Remove `@EnableScheduling` from `SlaWecomNotifierApplication` (confirm no other `@Scheduled`)
- [x] 1.5 Remove `sla.schedule` from `application.yml` (and any env-specific yml if present)

## 2. Guard owner / mention path

- [x] 2.1 Confirm diff does not touch `OwnerRuleResolver`, WeCom mention sending, or markdown owner lines
- [x] 2.2 Confirm `POST /api/sla/notify` and `GET /api/sla/preview` still call `notifyNow()` / `preview()` unchanged

## 3. Verify

- [x] 3.1 Run existing unit tests (`OwnerRuleResolverTest` and remaining suite)
- [x] 3.2 Grep the notifier module for `LastDaysOfMonth`, `sla.schedule`, and `@Scheduled` and ensure no leftovers
