package com.sla.notifier.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 133 remains the only primary/MyBatis datasource. 132 is used for explicit
 * sla_excluded_downtime writes and is never part of schema initialization.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties zabbix6DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = {"dataSource", "zabbix6DataSource"})
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource zabbix6DataSource(
            @Qualifier("zabbix6DataSourceProperties") DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setPoolName("zabbix6");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("sla.datasources.zabbix7")
    public DataSourceProperties zabbix7DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "zabbix7DataSource")
    @ConfigurationProperties("sla.datasources.zabbix7.hikari")
    public HikariDataSource zabbix7DataSource(
            @Qualifier("zabbix7DataSourceProperties") DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setPoolName("zabbix7");
        // Do not fail application startup when the reserved database is down.
        dataSource.setInitializationFailTimeout(-1L);
        return dataSource;
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("zabbix6DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "zabbix7JdbcTemplate")
    public JdbcTemplate zabbix7JdbcTemplate(@Qualifier("zabbix7DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
