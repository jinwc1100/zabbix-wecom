package com.sla.notifier.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourceConfigTest {

    @Test
    void zabbix7PoolIsLazyAndUsesConfiguredUrl() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:mysql://10.32.201.132:3306/zabbix");
        properties.setUsername("zabbix_readonly");
        properties.setPassword("test");

        HikariDataSource dataSource = new DataSourceConfig().zabbix7DataSource(properties);
        try {
            assertEquals("jdbc:mysql://10.32.201.132:3306/zabbix", dataSource.getJdbcUrl());
            assertEquals("zabbix7", dataSource.getPoolName());
            assertEquals(-1L, dataSource.getInitializationFailTimeout());
        } finally {
            dataSource.close();
        }
    }

    @Test
    void zabbix6BeansRemainPrimary() throws Exception {
        assertNotNull(DataSourceConfig.class.getMethod("zabbix6DataSourceProperties")
                .getAnnotation(Primary.class));
        assertNotNull(DataSourceConfig.class
                .getMethod("zabbix6DataSource", DataSourceProperties.class)
                .getAnnotation(Primary.class));
        assertNotNull(DataSourceConfig.class.getMethod("jdbcTemplate", javax.sql.DataSource.class)
                .getAnnotation(Primary.class));
    }
}
