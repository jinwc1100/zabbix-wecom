## 1. Manual sync nudges

- [x] 1.1 After successful `ReviewEventSyncService.runNow()` in `POST /api/sla/review-event-sync`, call the same incomplete-review nudge as the scheduler; if sync throws, do not nudge
- [x] 1.2 If nudge throws, log the error and still return the sync result body
- [x] 1.3 Keep `review-sync.enabled` gating cron only (disabled still allows POST sync+nudge)
- [x] 1.4 Confirm `POST /api/sla/notify` does not call the incomplete-review nudge

## 2. Triple WeCom send

- [x] 2.1 Prefix nudge markdown with the exact first line `重要的内容说3遍`
- [x] 2.2 Invoke `WeComWebhookClient.sendMarkdown` three times per nudge with the same count text and mention lists; ~400ms pause between sends
- [x] 2.3 Count 0 still sends zero times
- [x] 2.4 Unit-test: N>0 → three sends with prefix and mobiles; unspecified owner counted not mentioned; N=0 → no send

## 3. Review page edit lock

- [x] 3.1 On `/review/` (and demo HTML): rows with non-blank `count_sla` start read-only with an 编辑 control; never-saved rows stay immediately editable
- [x] 3.2 编辑 unlocks the six human fields; save still PUT with all six required; success returns to read-only
- [x] 3.3 Machine columns stay read-only; keep dirty confirm on page/filter change

## 4. Verify

- [x] 4.1 Run module tests; smoke: manual POST sync+nudge, cron path still triples, saved row needs 编辑 before save
