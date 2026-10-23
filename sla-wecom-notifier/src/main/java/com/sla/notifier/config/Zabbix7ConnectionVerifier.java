package com.sla.notifier.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Best-effort verification only. Failure must not affect the 133 primary
 * datasource or application availability.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class Zabbix7ConnectionVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(Zabbix7ConnectionVerifier.class);

    private final DataSource zabbix7DataSource;

    public Zabbix7ConnectionVerifier(@Qualifier("zabbix7DataSource") DataSource zabbix7DataSource) {
        this.zabbix7DataSource = zabbix7DataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection ignored = zabbix7DataSource.getConnection()) {
            log.info("预留数据源 zabbix7 连接成功");
        } catch (Exception e) {
            log.warn("预留数据源 zabbix7 连接失败；主库 zabbix6 继续可用: {}", e.getMessage());
        }
    }
}
