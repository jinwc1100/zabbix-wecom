package com.sla.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.SlaSystemOwner;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DowntimeQueryServiceTest {

    @Test
    void notifyIndexKeepsUnfilteredQueryAllRowsPath() {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        GrafanaProperties properties = leafProperties();
        when(client.fetchDashboard(session, "leaf"))
                .thenReturn(new ObjectMapper().createObjectNode());
        GrafanaClient.PanelQueryMeta meta =
                new GrafanaClient.PanelQueryMeta("老版", "SELECT *", "old-ds", "mysql");
        when(client.resolvePanelMeta(any(), eq(4))).thenReturn(meta);
        when(client.queryAllRows(session, meta)).thenReturn(Collections.emptyList());

        new DowntimeQueryService(client, properties).loadLeafIndex(session);

        verify(client).queryAllRows(session, meta);
        verify(client, never()).queryTable(
                any(GrafanaClient.Session.class),
                any(GrafanaClient.PanelQueryMeta.class), any(String.class),
                any(String.class), any(String.class));
    }

    @Test
    void reviewCollectUsesExplicitFromToNotNowMinusOneHour() {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        GrafanaProperties properties = leafProperties();
        when(client.fetchDashboard(session, "leaf"))
                .thenReturn(new ObjectMapper().createObjectNode());
        GrafanaClient.PanelQueryMeta meta =
                new GrafanaClient.PanelQueryMeta("老版", "SELECT *", "old-ds", "mysql");
        when(client.resolvePanelMeta(any(), eq(4))).thenReturn(meta);
        when(client.queryTable(eq(session), eq(meta), eq("SELECT *"),
                eq("1000"), eq("2000"))).thenReturn(Collections.emptyList());

        new DowntimeQueryService(client, properties).loadLeafIndex(session, "1000", "2000");

        verify(client).queryTable(session, meta, "SELECT *", "1000", "2000");
        verify(client, never()).queryAllRows(session, meta);
    }

    @Test
    void weeklyReviewCollectPassesWindowStartMillisAsFrom() {
        GrafanaClient grafanaClient = mock(GrafanaClient.class);
        DowntimeQueryService downtimeQueryService = mock(DowntimeQueryService.class);
        OwnerRuleResolver ownerRuleResolver = mock(OwnerRuleResolver.class);
        WeeklyReviewProperties properties = new WeeklyReviewProperties();
        properties.setZone("Asia/Shanghai");
        properties.setWindowStart("2026-07-30 00:00:00");
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(grafanaClient.login()).thenReturn(session);
        when(ownerRuleResolver.loadSnapshot()).thenReturn(Collections.<String, SlaSystemOwner>emptyMap());
        when(downtimeQueryService.loadLeafIndex(any(GrafanaClient.Session.class), any(String.class), any(String.class)))
                .thenReturn(Collections.<String, java.util.List<com.sla.notifier.model.DowntimeEvent>>emptyMap());
        when(downtimeQueryService.loadParentIndex(any(GrafanaClient.Session.class), any(String.class), any(String.class)))
                .thenReturn(Collections.<String, java.util.List<com.sla.notifier.model.DowntimeEvent>>emptyMap());

        LocalDateTime trigger = LocalDateTime.of(2026, 8, 21, 11, 0, 0);
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        String expectedFrom = String.valueOf(
                LocalDateTime.of(2026, 7, 30, 0, 0, 0).atZone(zone).toInstant().toEpochMilli());
        String expectedTo = String.valueOf(trigger.atZone(zone).toInstant().toEpochMilli());

        new WeeklyReviewDataService(grafanaClient, downtimeQueryService, ownerRuleResolver, properties)
                .collect(trigger);

        verify(downtimeQueryService).loadLeafIndex(session, expectedFrom, expectedTo);
        verify(downtimeQueryService).loadParentIndex(session, expectedFrom, expectedTo);
        verify(downtimeQueryService, never()).loadLeafIndex(session);
        verify(downtimeQueryService, never()).loadParentIndex(session);
    }

    private static GrafanaProperties leafProperties() {
        GrafanaProperties properties = new GrafanaProperties();
        GrafanaProperties.DowntimeSourceConfig source =
                new GrafanaProperties.DowntimeSourceConfig();
        source.setDashboardUid("leaf");
        source.setSystemLevel("子系统");
        source.setNameColumn("子系统名称");
        source.setStartColumn("宕机开始时间");
        source.setEndColumn("恢复时间");
        GrafanaProperties.PanelConfig panel = new GrafanaProperties.PanelConfig();
        panel.setId(4);
        panel.setName("老版");
        source.setPanels(Collections.singletonList(panel));
        properties.setDowntimeLeaf(source);
        return properties;
    }
}
