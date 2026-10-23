package com.sla.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.CountedSlaDowntime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CountedSlaDowntimeServiceTest {

    @Test
    void queriesOldAndNewPanelsAndDeduplicatesMappedRows() throws Exception {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.fetchDashboard(eq(session), any(String.class)))
                .thenReturn(new ObjectMapper().createObjectNode());
        GrafanaClient.PanelQueryMeta meta4 =
                new GrafanaClient.PanelQueryMeta("老版", "SELECT 4;\n", "old-ds", "mysql");
        GrafanaClient.PanelQueryMeta meta2 =
                new GrafanaClient.PanelQueryMeta("新版", "SELECT 2;", "new-ds", "mysql");
        when(client.resolvePanelMeta(any(), eq(4))).thenReturn(meta4);
        when(client.resolvePanelMeta(any(), eq(2))).thenReturn(meta2);
        Map<String, String> row = reviewedRow(
                "雄安服务平台", "故障分类:网络--故障子类:专线--故障描述:中断");
        when(client.queryTable(eq(session), any(GrafanaClient.PanelQueryMeta.class),
                any(String.class), any(String.class), any(String.class)))
                .thenReturn(Collections.singletonList(row));

        GrafanaProperties properties = new GrafanaProperties();
        properties.setDowntimeLeaf(source("leaf", "子系统", "子系统名称",
                "宕机开始时间", "恢复时间"));
        properties.setDowntimeParent(null);

        List<CountedSlaDowntime> result =
                new CountedSlaDowntimeService(client, properties).collect(session);

        assertEquals(1, result.size());
        assertEquals("故障分类:网络--故障子类:专线--故障描述:中断",
                result.get(0).getReviewDesc());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> from = ArgumentCaptor.forClass(String.class);
        verify(client, times(2)).queryTable(
                eq(session), any(GrafanaClient.PanelQueryMeta.class),
                sql.capture(), from.capture(), eq("now"));
        for (String value : sql.getAllValues()) {
            assertTrue(value.contains("IN ('未排除', '-')"));
            assertTrue(value.contains("`排除状态` = '部分排除'"));
            assertTrue(value.contains("`排除故障秒数` = 1"));
            assertTrue(value.contains("\n) AS t WHERE"));
            assertFalse(value.contains(";\n) AS t"));
        }
        assertTrue(Long.parseLong(from.getValue()) > 0L);
    }

    @Test
    void unreviewedUnexcludedHasBlankReviewDesc() {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.fetchDashboard(eq(session), any(String.class)))
                .thenReturn(new ObjectMapper().createObjectNode());
        when(client.resolvePanelMeta(any(), eq(4))).thenReturn(
                new GrafanaClient.PanelQueryMeta("老版", "SELECT 1", "ds", "mysql"));
        when(client.resolvePanelMeta(any(), eq(2))).thenReturn(
                new GrafanaClient.PanelQueryMeta("新版", "SELECT 2", "ds", "mysql"));
        Map<String, String> row = new LinkedHashMap<String, String>();
        row.put("子系统名称", "山西省金服");
        row.put("宕机开始时间", "2026-09-01 10:00:00");
        row.put("恢复时间", "2026-09-01 11:00:00");
        row.put("排除状态", "未排除");
        row.put("排除原因", "-");
        when(client.queryTable(eq(session), any(GrafanaClient.PanelQueryMeta.class),
                any(String.class), any(String.class), any(String.class)))
                .thenReturn(Collections.singletonList(row));

        GrafanaProperties properties = new GrafanaProperties();
        properties.setDowntimeLeaf(source("leaf", "子系统", "子系统名称",
                "宕机开始时间", "恢复时间"));
        properties.setDowntimeParent(null);

        List<CountedSlaDowntime> result =
                new CountedSlaDowntimeService(client, properties).collect(session);

        assertEquals(1, result.size());
        assertNull(result.get(0).getReviewDesc());
        assertEquals("山西省金服", result.get(0).getSystemName());
    }

    private static Map<String, String> reviewedRow(String name, String reason) {
        Map<String, String> row = new LinkedHashMap<String, String>();
        row.put("子系统名称", name);
        row.put("宕机开始时间", "2026-08-06 01:27:17");
        row.put("恢复时间", "2026-08-06 02:46:02");
        row.put("排除状态", "部分排除");
        row.put("排除故障秒数", "1");
        row.put("排除原因", reason);
        return row;
    }

    private static GrafanaProperties.DowntimeSourceConfig source(
            String uid, String level, String name, String start, String end) {
        GrafanaProperties.DowntimeSourceConfig source =
                new GrafanaProperties.DowntimeSourceConfig();
        source.setDashboardUid(uid);
        source.setSystemLevel(level);
        source.setNameColumn(name);
        source.setStartColumn(start);
        source.setEndColumn(end);
        source.setReasonColumn("排除原因");
        source.setPanels(Arrays.asList(panel(4, "老版"), panel(2, "新版")));
        return source;
    }

    private static GrafanaProperties.PanelConfig panel(int id, String name) {
        GrafanaProperties.PanelConfig panel = new GrafanaProperties.PanelConfig();
        panel.setId(id);
        panel.setName(name);
        return panel;
    }
}
