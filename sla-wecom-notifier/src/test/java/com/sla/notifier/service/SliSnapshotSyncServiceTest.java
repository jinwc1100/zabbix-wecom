package com.sla.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.mapper.SlaSliSnapshotMapper;
import com.sla.notifier.model.SlaSliSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SliSnapshotSyncServiceTest {

    @Test
    void dashMonthIsNullAndBlankNameSkippedAndPanelsAreSeparateRows() {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.fetchDashboard(eq(session), any(String.class)))
                .thenReturn(new ObjectMapper().createObjectNode());
        GrafanaClient.PanelQueryMeta meta4 =
                new GrafanaClient.PanelQueryMeta("老版", "SELECT 4", "ds", "mysql");
        GrafanaClient.PanelQueryMeta meta2 =
                new GrafanaClient.PanelQueryMeta("新版", "SELECT 2", "ds", "mysql");
        when(client.resolvePanelMeta(any(), eq(4))).thenReturn(meta4);
        when(client.resolvePanelMeta(any(), eq(2))).thenReturn(meta2);
        Map<String, String> named = new LinkedHashMap<String, String>();
        named.put("子系统名称", "雄安服务平台");
        named.put("年度SLI", "99.40%");
        named.put("八月", "-");
        named.put("一月", "99.9");
        Map<String, String> blank = new LinkedHashMap<String, String>();
        blank.put("子系统名称", "  ");
        blank.put("年度SLI", "98");
        when(client.queryAllRows(session, meta4)).thenReturn(Arrays.asList(named, blank));
        when(client.queryAllRows(session, meta2)).thenReturn(Collections.singletonList(named));

        GrafanaProperties properties = new GrafanaProperties();
        properties.setMonthlySliSources(Collections.singletonList(source()));

        List<SlaSliSnapshot> rows = new SliSnapshotSyncService(
                client, properties, mock(SlaSliSnapshotMapper.class))
                .collect(session, 2026, LocalDateTime.of(2026, 8, 28, 17, 0));

        assertEquals(2, rows.size());
        assertEquals(Integer.valueOf(4), Integer.valueOf(rows.get(0).getPanelId()));
        assertEquals(Integer.valueOf(2), Integer.valueOf(rows.get(1).getPanelId()));
        assertEquals("雄安服务平台", rows.get(0).getSystemName());
        assertEquals(Double.valueOf(99.4), rows.get(0).getSliAnnual());
        assertEquals(Double.valueOf(99.9), rows.get(0).getSliM01());
        assertNull(rows.get(0).getSliM08());
        assertEquals(2026, rows.get(0).getReportYear());
    }

    @Test
    void runNowUpsertsCollectedRowsWithoutDelete() {
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.login()).thenReturn(session);
        when(client.fetchDashboard(eq(session), any(String.class)))
                .thenReturn(new ObjectMapper().createObjectNode());
        GrafanaClient.PanelQueryMeta meta4 =
                new GrafanaClient.PanelQueryMeta("老版", "SELECT 4", "ds", "mysql");
        GrafanaClient.PanelQueryMeta meta2 =
                new GrafanaClient.PanelQueryMeta("新版", "SELECT 2", "ds", "mysql");
        when(client.resolvePanelMeta(any(), eq(4))).thenReturn(meta4);
        when(client.resolvePanelMeta(any(), eq(2))).thenReturn(meta2);
        Map<String, String> named = new LinkedHashMap<String, String>();
        named.put("子系统名称", "系统甲");
        named.put("年度SLI", "99");
        when(client.queryAllRows(session, meta4)).thenReturn(Collections.singletonList(named));
        when(client.queryAllRows(session, meta2)).thenReturn(Collections.singletonList(named));
        GrafanaProperties properties = new GrafanaProperties();
        properties.setMonthlySliSources(Collections.singletonList(source()));
        SlaSliSnapshotMapper mapper = mock(SlaSliSnapshotMapper.class);

        int upserted = new SliSnapshotSyncService(client, properties, mapper).runNow().getUpserted();

        assertEquals(2, upserted);
        verify(mapper, times(2)).upsert(any(SlaSliSnapshot.class));
    }

    @Test
    void parseNumericSliTreatsDashAsNull() {
        assertNull(SliSnapshotSyncService.parseNumericSli("-"));
        assertNull(SliSnapshotSyncService.parseNumericSli(""));
        assertEquals(Double.valueOf(98.5), SliSnapshotSyncService.parseNumericSli("98.5%"));
    }

    private static GrafanaProperties.SliSourceConfig source() {
        GrafanaProperties.SliSourceConfig source = new GrafanaProperties.SliSourceConfig();
        source.setDashboardUid("afj5c5o5v3rb4f");
        source.setSystemLevel("子系统");
        source.setNameColumns(Collections.singletonList("子系统名称"));
        GrafanaProperties.PanelConfig p4 = new GrafanaProperties.PanelConfig();
        p4.setId(4);
        p4.setName("老版监控");
        GrafanaProperties.PanelConfig p2 = new GrafanaProperties.PanelConfig();
        p2.setId(2);
        p2.setName("新版监控");
        source.setPanels(Arrays.asList(p4, p2));
        return source;
    }
}
