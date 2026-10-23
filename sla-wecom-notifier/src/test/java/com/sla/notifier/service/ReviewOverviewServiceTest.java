package com.sla.notifier.service;

import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.model.CountedSlaDowntime;
import com.sla.notifier.model.OwnerIncompleteCount;
import com.sla.notifier.model.ReviewOverview;
import com.sla.notifier.model.SlaLowSliItem;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewOverviewServiceTest {

    @Test
    void returnsRowCountsAndSplitsSortedLowSliBySource() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteBySystemLevel("父系统")).thenReturn(4L);
        when(mapper.countIncompleteBySystemLevel("子系统")).thenReturn(2L);
        OwnerIncompleteCount first = owner("卓松林", 5);
        OwnerIncompleteCount second = owner("王华聪", 1);
        when(mapper.countIncompleteByOwner()).thenReturn(Arrays.asList(first, second));

        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.login()).thenReturn(session);
        AnnualSliService annual = mock(AnnualSliService.class);
        when(annual.collectLowSliItems(session)).thenReturn(Arrays.asList(
                item("父-A", "父系统", 98.1),
                item("子-A", "子系统", 98.2),
                item("父-B", "父系统", 99.1)));
        int currentMonth = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).getMonthValue();
        AnnualSliService.MonthlySliCollection monthly =
                new AnnualSliService.MonthlySliCollection();
        monthly.getLowItemsByMonth().get(currentMonth).addAll(Arrays.asList(
                item("月父-A", "父系统", 97.1),
                item("月子-A", "子系统", 96.2)));
        when(annual.collectMonthlySliData(session)).thenReturn(monthly);
        CountedSlaDowntimeService counted = mock(CountedSlaDowntimeService.class);
        when(counted.collect(session)).thenReturn(Arrays.asList(
                downtime("父-A", "父系统", currentMonth),
                downtime("月子-A", "子系统", currentMonth)));
        GrafanaProperties properties = new GrafanaProperties();
        properties.setSliThreshold(99.9);

        ReviewOverview result = new ReviewOverviewService(
                mapper, client, annual, counted, properties).getOverview();

        assertEquals(4L, result.getIncompleteParentCount());
        assertEquals(2L, result.getIncompleteChildCount());
        assertEquals(5L, result.getIncompleteByOwner().get(0).getCount());
        assertEquals(2, result.getLowSliParents().size());
        assertEquals("父-A", result.getLowSliParents().get(0).getSystemName());
        assertEquals("子-A", result.getLowSliChildren().get(0).getSystemName());
        assertEquals("月父-A", result.getLowMonthlySliParents().get(0).getSystemName());
        assertEquals("月子-A", result.getLowMonthlySliChildren().get(0).getSystemName());
        assertEquals(currentMonth, result.getMonthlyCurrentMonth());
        assertEquals("low", result.getLowMonthlyByMonth().get(currentMonth).getParentStatus());
        assertNull(result.getGrafanaError());
        assertNull(result.getMonthlyGrafanaError());
    }

    @Test
    void grafanaLoginFailureKeepsSqlCountsAndBothErrors() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteBySystemLevel("父系统")).thenReturn(3L);
        when(mapper.countIncompleteBySystemLevel("子系统")).thenReturn(6L);
        when(mapper.countIncompleteByOwner()).thenReturn(Arrays.asList(owner("卓松林", 9)));
        GrafanaClient client = mock(GrafanaClient.class);
        when(client.login()).thenThrow(new IllegalStateException("Grafana unavailable"));
        AnnualSliService annual = mock(AnnualSliService.class);
        CountedSlaDowntimeService counted = mock(CountedSlaDowntimeService.class);

        ReviewOverview result = new ReviewOverviewService(
                mapper, client, annual, counted, new GrafanaProperties())
                .getOverview();

        assertEquals(3L, result.getIncompleteParentCount());
        assertEquals(6L, result.getIncompleteChildCount());
        assertEquals(0, result.getLowSliParents().size());
        assertEquals(0, result.getLowSliChildren().size());
        assertEquals(0, result.getLowMonthlySliParents().size());
        assertNotNull(result.getGrafanaError());
        assertNotNull(result.getMonthlyGrafanaError());
        verify(annual, never()).collectLowSliItems(org.mockito.ArgumentMatchers.any());
        verify(annual, never()).collectMonthlySliData(org.mockito.ArgumentMatchers.any());
        verify(counted, never()).collect(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void monthlyFailureKeepsAnnualRanks() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteBySystemLevel("父系统")).thenReturn(1L);
        when(mapper.countIncompleteBySystemLevel("子系统")).thenReturn(0L);
        when(mapper.countIncompleteByOwner()).thenReturn(Collections.<OwnerIncompleteCount>emptyList());
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.login()).thenReturn(session);
        AnnualSliService annual = mock(AnnualSliService.class);
        when(annual.collectLowSliItems(session)).thenReturn(Arrays.asList(
                item("父-A", "父系统", 98.1)));
        when(annual.collectMonthlySliData(session))
                .thenThrow(new IllegalStateException("monthly board down"));
        CountedSlaDowntimeService counted = mock(CountedSlaDowntimeService.class);
        when(counted.collect(session)).thenReturn(Collections.<CountedSlaDowntime>emptyList());

        ReviewOverview result = new ReviewOverviewService(
                mapper, client, annual, counted, new GrafanaProperties()).getOverview();

        assertEquals("父-A", result.getLowSliParents().get(0).getSystemName());
        assertEquals(0, result.getLowMonthlySliParents().size());
        assertNull(result.getGrafanaError());
        assertEquals("monthly board down", result.getMonthlyGrafanaError());
    }

    @Test
    void monthlyStatusDistinguishesOkFromEmpty() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteByOwner()).thenReturn(Collections.<OwnerIncompleteCount>emptyList());
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.login()).thenReturn(session);
        AnnualSliService annual = mock(AnnualSliService.class);
        when(annual.collectLowSliItems(session)).thenReturn(Collections.<SlaLowSliItem>emptyList());
        AnnualSliService.MonthlySliCollection monthly =
                mock(AnnualSliService.MonthlySliCollection.class);
        Map<Integer, List<SlaLowSliItem>> byMonth =
                new LinkedHashMap<Integer, List<SlaLowSliItem>>();
        for (int month = 1; month <= 12; month++) {
            byMonth.put(month, new ArrayList<SlaLowSliItem>());
        }
        when(monthly.getLowItemsByMonth()).thenReturn(byMonth);
        when(monthly.getNumericCount(1, "父系统")).thenReturn(2);
        when(annual.collectMonthlySliData(session)).thenReturn(monthly);
        CountedSlaDowntimeService counted = mock(CountedSlaDowntimeService.class);
        when(counted.collect(session)).thenReturn(
                Collections.<CountedSlaDowntime>emptyList());

        ReviewOverview result = new ReviewOverviewService(
                mapper, client, annual, counted, new GrafanaProperties()).getOverview();

        assertEquals("ok", result.getLowMonthlyByMonth().get(1).getParentStatus());
        assertEquals("empty", result.getLowMonthlyByMonth().get(1).getChildStatus());
    }

    @Test
    void countedDowntimeFailureKeepsSliRanks() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteByOwner()).thenReturn(Collections.<OwnerIncompleteCount>emptyList());
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.login()).thenReturn(session);
        AnnualSliService annual = mock(AnnualSliService.class);
        when(annual.collectLowSliItems(session)).thenReturn(
                Collections.singletonList(item("父-A", "父系统", 98.1)));
        AnnualSliService.MonthlySliCollection monthly =
                new AnnualSliService.MonthlySliCollection();
        int currentMonth = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).getMonthValue();
        monthly.getLowItemsByMonth().get(currentMonth).add(
                item("月父-A", "父系统", 97.1));
        when(annual.collectMonthlySliData(session)).thenReturn(monthly);
        CountedSlaDowntimeService counted = mock(CountedSlaDowntimeService.class);
        when(counted.collect(session)).thenThrow(new IllegalStateException("downtime failed"));

        ReviewOverview result = new ReviewOverviewService(
                mapper, client, annual, counted, new GrafanaProperties()).getOverview();

        assertEquals(1, result.getLowSliParents().size());
        assertEquals("父-A", result.getLowSliParents().get(0).getSystemName());
        assertEquals(1, result.getLowMonthlySliParents().size());
        assertEquals("downtime failed", result.getCountedDowntimeError());
        assertEquals(0, result.getCountedSlaDowntimes().size());
    }

    @Test
    void lowSliWithoutCountedDowntimeStillRanks() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteByOwner()).thenReturn(Collections.<OwnerIncompleteCount>emptyList());
        GrafanaClient client = mock(GrafanaClient.class);
        GrafanaClient.Session session = mock(GrafanaClient.Session.class);
        when(client.login()).thenReturn(session);
        AnnualSliService annual = mock(AnnualSliService.class);
        when(annual.collectLowSliItems(session)).thenReturn(Arrays.asList(
                item("系统甲", "子系统", 98.1),
                item("系统甲备份", "子系统", 98.2)));
        AnnualSliService.MonthlySliCollection monthly =
                new AnnualSliService.MonthlySliCollection();
        monthly.getLowItemsByMonth().get(7).add(item("系统甲", "子系统", 98.1));
        monthly.getLowItemsByMonth().get(8).add(item("系统甲", "子系统", 98.1));
        when(annual.collectMonthlySliData(session)).thenReturn(monthly);
        CountedSlaDowntimeService counted = mock(CountedSlaDowntimeService.class);
        when(counted.collect(session)).thenReturn(Collections.singletonList(
                downtime("系统甲", "子系统", 8)));

        ReviewOverview result = new ReviewOverviewService(
                mapper, client, annual, counted, new GrafanaProperties()).getOverview();

        assertEquals(2, result.getLowSliChildren().size());
        assertEquals(1, result.getLowMonthlyByMonth().get(7).getChildren().size());
        assertEquals(1, result.getLowMonthlyByMonth().get(8).getChildren().size());
        assertEquals(1, result.getCountedSlaDowntimes().size());
    }

    private static OwnerIncompleteCount owner(String name, long count) {
        OwnerIncompleteCount value = new OwnerIncompleteCount();
        value.setOwnerName(name);
        value.setCount(count);
        return value;
    }

    private static SlaLowSliItem item(String name, String level, double value) {
        SlaLowSliItem item = new SlaLowSliItem();
        item.setSystemName(name);
        item.setSourceLevel(level);
        item.setAnnualSliValue(value);
        item.setAnnualSli(String.valueOf(value));
        return item;
    }

    private static CountedSlaDowntime downtime(String name, String level, int month) {
        CountedSlaDowntime item = new CountedSlaDowntime();
        item.setSystemName(name);
        item.setSystemLevel(level);
        item.setDownStart(String.format("2026-%02d-06 01:27:17", month));
        item.setDownEnd(String.format("2026-%02d-06 02:46:02", month));
        item.setReviewDesc("测试复盘");
        return item;
    }
}
