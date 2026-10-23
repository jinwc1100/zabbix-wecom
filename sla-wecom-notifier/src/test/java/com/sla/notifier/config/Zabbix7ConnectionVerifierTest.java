package com.sla.notifier.config;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Zabbix7ConnectionVerifierTest {

    @Test
    void connectionFailureDoesNotThrow() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("refused"));

        assertDoesNotThrow(() -> new Zabbix7ConnectionVerifier(dataSource).run(null));
    }
}
