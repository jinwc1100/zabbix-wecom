## ADDED Requirements

### Requirement: Two JDBC pools keyed zabbix6 and zabbix7

The system MUST keep the primary pool as Spring Boot `spring.datasource` (key `zabbix6`, MySQL `10.32.201.133:3306` database `zabbix`). The system MUST also build a second DataSource from `sla.datasources.zabbix7` (key `zabbix7`, MySQL `10.32.201.132:3306` database `zabbix`) for **later** inserts. The system MUST NOT treat `sla.datasources.zabbix7` as a replacement for `spring.datasource`. Dictionary `write_jdbc_key` values MUST be exactly `zabbix6` or `zabbix7`. Hostnames MUST NOT be stored on `sla_system_owner` rows.

This change MUST route all schema DDL, review-event writes, SLI snapshot writes, review-page reads/writes, overview incomplete SQL, and incomplete-nudge SQL to **zabbix6 only**.

#### Scenario: Primary pool unchanged

- **WHEN** the application starts with both YAML blocks present
- **THEN** MyBatis mapper calls used by review sync, snapshot sync, review web, overview incomplete counts, and owner dictionary MUST use `zabbix6`

#### Scenario: zabbix7 pool is registered

- **WHEN** the application starts and `sla.datasources.zabbix7` is configured
- **THEN** a DataSource bean for that URL MUST exist whose JDBC host is `10.32.201.132`

#### Scenario: zabbix7 down does not block primary

- **WHEN** the zabbix7 pool cannot be created or cannot connect at startup
- **THEN** the application MUST still start and serve zabbix6 traffic, and MUST log the zabbix7 failure

### Requirement: No schema changes on zabbix7

The system MUST NOT execute CREATE TABLE, ALTER TABLE, DROP, or other DDL against the zabbix7 pool. Startup schema initialization MUST run only on zabbix6.

#### Scenario: Missing sla_review_event on 132 is ignored

- **WHEN** the application starts and zabbix7 has no `sla_review_event` table
- **THEN** the system MUST NOT create that table on zabbix7 and MUST still create or migrate it on zabbix6 as today
