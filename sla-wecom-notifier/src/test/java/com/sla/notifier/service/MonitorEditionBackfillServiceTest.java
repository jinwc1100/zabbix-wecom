package com.sla.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.SlaSystemOwner;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitorEditionBackfillServiceTest {

    @Test
    void backfillsNewPanelAndDefaultsUnmatchedOwner() {
        SlaSystemOwnerMapper mapper = mock(SlaSystemOwnerMapper.class);
        GrafanaClient grafana = mock(GrafanaClient.class);
        GrafanaProperties properties = properties();
        GrafanaClient.Session session = new GrafanaClient.Session("cookie");
        JsonNode dashboard = new ObjectMapper().createObjectNode();

        when(grafana.login()).thenReturn(session);
        when(grafana.fetchDashboard(any(GrafanaClient.Session.class), anyString()))
                .thenReturn(dashboard);
        when(grafana.resolvePanelMeta(any(JsonNode.class), anyInt()))
                .thenReturn(new GrafanaClient.PanelQueryMeta("panel", "select 1", "uid", "mysql"));
        when(grafana.queryAllRows(any(GrafanaClient.Session.class), any(GrafanaClient.PanelQueryMeta.class)))
                .thenReturn(Collections.<Map<String, String>>emptyList())
                .thenReturn(Collections.singletonList(row("系统名称", "新版系统")));

        when(mapper.selectAll()).thenReturn(Arrays.asList(
                owner("子系统", "新版系统"), owner("子系统", "未匹配系统")));

        new MonitorEditionBackfillService(mapper, grafana, properties).backfill();

        verify(mapper).updateMonitorRouting("子系统", "新版系统", "新版监控", "zabbix7");
        verify(mapper).updateMonitorRouting("子系统", "未匹配系统", "老版监控", "zabbix6");
    }

    @Test
    void collisionIsNotUpdated() {
        SlaSystemOwnerMapper mapper = mock(SlaSystemOwnerMapper.class);
        GrafanaClient grafana = mock(GrafanaClient.class);
        GrafanaProperties properties = properties();
        GrafanaClient.Session session = new GrafanaClient.Session("cookie");

        when(grafana.login()).thenReturn(session);
        when(grafana.fetchDashboard(any(GrafanaClient.Session.class), anyString()))
                .thenReturn(new ObjectMapper().createObjectNode());
        when(grafana.resolvePanelMeta(any(JsonNode.class), anyInt()))
                .thenReturn(new GrafanaClient.PanelQueryMeta("panel", "select 1", "uid", "mysql"));
        when(grafana.queryAllRows(any(GrafanaClient.Session.class), any(GrafanaClient.PanelQueryMeta.class)))
                .thenReturn(Collections.singletonList(row("系统名称", "重复系统")));
        when(mapper.selectAll()).thenReturn(Collections.singletonList(owner("子系统", "重复系统")));

        new MonitorEditionBackfillService(mapper, grafana, properties).backfill();

        verify(mapper, never()).updateMonitorRouting(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void grafanaFailureDefaultsUnmatchedToOldPool() {
        SlaSystemOwnerMapper mapper = mock(SlaSystemOwnerMapper.class);
        GrafanaClient grafana = mock(GrafanaClient.class);
        when(grafana.login()).thenThrow(new IllegalStateException("grafana down"));
        when(mapper.selectAll()).thenReturn(Collections.singletonList(owner("子系统", "任意系统")));

        new MonitorEditionBackfillService(mapper, grafana, properties()).backfill();

        verify(mapper).updateMonitorRouting("子系统", "任意系统", "老版监控", "zabbix6");
    }

    private static GrafanaProperties properties() {
        GrafanaProperties.PanelConfig oldPanel = new GrafanaProperties.PanelConfig();
        oldPanel.setId(4);
        GrafanaProperties.PanelConfig newPanel = new GrafanaProperties.PanelConfig();
        newPanel.setId(2);

        GrafanaProperties.SliSourceConfig source = new GrafanaProperties.SliSourceConfig();
        source.setDashboardUid("dashboard");
        source.setSystemLevel("子系统");
        source.setNameColumns(Collections.singletonList("系统名称"));
        source.setPanels(Arrays.asList(oldPanel, newPanel));

        GrafanaProperties properties = new GrafanaProperties();
        properties.setSliSources(Collections.singletonList(source));
        return properties;
    }

    private static SlaSystemOwner owner(String level, String name) {
        SlaSystemOwner owner = new SlaSystemOwner();
        owner.setSystemLevel(level);
        owner.setSystemName(name);
        return owner;
    }

    private static Map<String, String> row(String column, String value) {
        Map<String, String> row = new LinkedHashMap<String, String>();
        row.put(column, value);
        return row;
    }
}
