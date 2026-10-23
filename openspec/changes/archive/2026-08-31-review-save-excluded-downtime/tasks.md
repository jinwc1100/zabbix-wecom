## 1. Target-pool access

- [x] 1.1 Add a zabbix7 `JdbcTemplate` on `zabbix7DataSource`; keep MyBatis on primary zabbix6
- [x] 1.2 Resolve `write_jdbc_key` via `sla_system_owner` `(system_level, system_name)`; missing owner or key fails the save

## 2. SLA lookup and ids

- [x] 2.1 On the target pool, `SELECT slaid FROM sla WHERE name = system_name`; zero rows fails the save
- [x] 2.2 Allocate `sla_excluded_downtimeid` from `ids` (`nextid + 1` then update `nextid`); never AUTO_INCREMENT

## 3. Insert after review save

- [x] 3.1 After existing review UPDATE, convert `down_start`/`exclude_time` to Unix seconds `Asia/Shanghai`; skip insert if same `slaid+period_from+period_to` exists
- [x] 3.2 Insert `name` as `故障分类:{cat}--故障子类:{sub}--故障描述:{desc}` truncated to 255; wire into `ReviewEventService.update` so insert failure fails the API

## 4. Tests

- [x] 4.1 Tests: zabbix7 path uses zabbix7 template; missing sla name fails; duplicate window no second insert; Unix conversion; `mvn test`
