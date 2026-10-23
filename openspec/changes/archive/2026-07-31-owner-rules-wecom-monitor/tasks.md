## 1. Owner rules

- [x] 1.1 Update `owners.rules` in `application.yml` from `doc/list.txt`: add 陕西/成都/平潭；雷志加 `西藏工程交易`；王华聪雄安 TRADE 改为 `雄安工程交易`；保留 types 为空的雄安兜底→王华聪
- [x] 1.2 Sanity-check resolver behavior for portal vs engineering-trading vs bid (no code change unless region keywords cannot express rules)

## 2. WeCom monitor system field

- [x] 2.1 Pass SLI `panelName` through match into `DowntimeNotifyItem` (e.g. `monitorSystem`)
- [x] 2.2 Update `formatWecomSummary` to append 监控系统 (`新版监控`/`老版监控`); leave detail markdown unchanged

## 3. Verify

- [x] 3.1 Rebuild, restart service
- [x] 3.2 Re-export `GET /api/sla/review-excel?month=2026-07` and confirm former 待指定 systems resolve (成都→刘彩霞, 陕西交易→卓松林, 陕西开评标→刘彩霞, 平潭交易→张宇, 平潭开评标→雷志, 西藏工程交易→雷志)
- [x] 3.3 Preview `wecomMarkdown` contains 监控系统; detail markdown unchanged in field set
