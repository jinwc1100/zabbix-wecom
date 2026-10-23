package com.sla.notifier.service;

import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnualSliServiceTest {

    @Test
    void annualCollectDoesNotFetchMonthlyDashboard() throws Exception {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.fetchDashboard(any(GrafanaClient.Session.class), any(String.class)))
                .thenReturn(new ObjectMapper().createObjectNode());

        GrafanaProperties properties = new GrafanaProperties();
        GrafanaProperties.SliSourceConfig annual = source("年度", "ann-uid");
        GrafanaProperties.SliSourceConfig monthly = source("月度", "mon-uid");
        properties.setSliSources(Collections.singletonList(annual));
        properties.setMonthlySliSources(Collections.singletonList(monthly));

        new AnnualSliService(client, properties).collectLowSliItems(session);

        verify(client).fetchDashboard(session, "ann-uid");
        verify(client, never()).fetchDashboard(session, "mon-uid");
    }

    @Test
    void monthlyCollectDoesNotFetchAnnualDashboard() throws Exception {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.fetchDashboard(any(GrafanaClient.Session.class), any(String.class)))
                .thenReturn(new ObjectMapper().createObjectNode());

        GrafanaProperties properties = new GrafanaProperties();
        properties.setSliSources(Collections.singletonList(source("年度", "ann-uid")));
        properties.setMonthlySliSources(Collections.singletonList(source("月度", "mon-uid")));

        new AnnualSliService(client, properties).collectMonthlyLowSliItems(session);

        verify(client).fetchDashboard(session, "mon-uid");
        verify(client, never()).fetchDashboard(session, "ann-uid");
    }

    @Test
    void blankMonthlyUidIsSkipped() {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        GrafanaProperties properties = new GrafanaProperties();
        properties.setMonthlySliSources(Collections.singletonList(source("月度", "  ")));

        new AnnualSliService(client, properties).collectMonthlyLowSliItems(session);

        verify(client, never()).fetchDashboard(eq(session), any(String.class));
    }

    @Test
    void monthlyCollectionUsesMonthColumnsAndSkipsNonNumericWithoutDeduplication() throws Exception {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.fetchDashboard(session, "mon-uid"))
                .thenReturn(new ObjectMapper().createObjectNode());
        GrafanaClient.PanelQueryMeta oldMeta =
                new GrafanaClient.PanelQueryMeta("老版", "SELECT ...", "ds", "mysql");
        GrafanaClient.PanelQueryMeta newMeta =
                new GrafanaClient.PanelQueryMeta("新版", "SELECT ...", "ds", "mysql");
        when(client.resolvePanelMeta(any(), eq(4))).thenReturn(oldMeta);
        when(client.resolvePanelMeta(any(), eq(2))).thenReturn(newMeta);

        Map<String, String> dash = row("同一系统", "-", "100%");
        Map<String, String> low = row("同一系统", "98.5%", "-");
        when(client.queryAllRows(session, oldMeta)).thenReturn(Arrays.asList(dash, low));
        when(client.queryAllRows(session, newMeta)).thenReturn(Collections.singletonList(low));

        GrafanaProperties properties = new GrafanaProperties();
        properties.setSliThreshold(99.9);
        GrafanaProperties.SliSourceConfig monthly = source("月度", "mon-uid");
        monthly.setSystemLevel("父系统");
        monthly.setNameColumns(Collections.singletonList("父系统名称"));
        monthly.setPanels(Arrays.asList(panel(4, "老版"), panel(2, "新版")));
        properties.setMonthlySliSources(Collections.singletonList(monthly));

        AnnualSliService.MonthlySliCollection result =
                new AnnualSliService(client, properties).collectMonthlySliData(session);

        assertEquals(2, result.getLowItemsByMonth().get(8).size());
        assertEquals("同一系统", result.getLowItemsByMonth().get(8).get(0).getSystemName());
        assertEquals(2, result.getNumericCount(8, "父系统"));
        assertEquals(1, result.getNumericCount(9, "父系统"));
        assertEquals(0, result.getLowItemsByMonth().get(9).size());
        verify(client, never()).queryLowSliRows(
                any(GrafanaClient.Session.class), any(GrafanaClient.PanelQueryMeta.class),
                any(Double.class), any(String.class));
    }

    private static GrafanaProperties.SliSourceConfig source(String name, String uid) {
        GrafanaProperties.SliSourceConfig source = new GrafanaProperties.SliSourceConfig();
        source.setName(name);
        source.setDashboardUid(uid);
        return source;
    }

    private static GrafanaProperties.PanelConfig panel(int id, String name) {
        GrafanaProperties.PanelConfig panel = new GrafanaProperties.PanelConfig();
        panel.setId(id);
        panel.setName(name);
        return panel;
    }

    private static Map<String, String> row(String name, String august, String september) {
        Map<String, String> row = new LinkedHashMap<String, String>();
        row.put("父系统名称", name);
        row.put("八月", august);
        row.put("九月", september);
        return row;
    }
}
