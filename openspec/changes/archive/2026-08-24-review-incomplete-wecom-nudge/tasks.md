## 1. Config

- [x] 1.1 Set `review-sync.enabled` default to true and `review-sync.cron` to `0 30 17 * * *` in `application.yml` (keep `Asia/Shanghai`)

## 2. Query incomplete rows

- [x] 2.1 Add mapper method(s) to count `sla_review_event` rows where `count_sla` is NULL or blank, and to list distinct `owner_name` for those rows
- [x] 2.2 Cover empty vs `是`/`否` in a mapper/SQL unit test (or service test with stubbed mapper)

## 3. Nudge service

- [x] 3.1 Add a nudge service that: loads incomplete count + names; maps names to mobiles via `owners.rules` (first non-blank `mobile` for equal `name`, do not call `OwnerRuleResolver.resolve`); if count is 0 return without WeCom; else `WeComWebhookClient.sendMarkdown` with count-only markdown and mention lists; skip `@` for `待指定责任人` / names with no mobile
- [x] 3.2 Unit-test: N>0 mentions mobiles; unspecified owner counted not mentioned; N=0 does not send

## 4. Scheduler wiring

- [x] 4.1 In `ReviewEventSyncScheduler.runDaily`, after successful `ReviewEventSyncService.runNow()`, call the nudge; if sync throws, do not nudge (keep existing catch)
- [x] 4.2 Confirm `SlaNotifyController` `POST /api/sla/review-event-sync` still only calls `runNow()` (no nudge)
- [x] 4.3 Confirm `POST /api/sla/notify` is unchanged

## 5. Verify

- [x] 5.1 Run module tests; smoke: enabled+cron values, manual POST does not send WeCom
