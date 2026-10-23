## 1. Schedule

- [x] 1.1 Default `review-sync.cron` hourly `0 0 * * * *`; keep zone Shanghai
- [x] 1.2 After successful cron sync, nudge only when job start hour is 17 in the zone; other hours sync without WeCom
- [x] 1.3 `enabled=false` still skips sync and cron nudge; POST `notify` unchanged

## 2. Tests

- [x] 2.1 Scheduler: 17:00 start → sync+nudge; non-17 → sync only; disabled/fail skip nudge
- [x] 2.2 `mvn test`
