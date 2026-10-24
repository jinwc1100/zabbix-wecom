package com.sla.notifier.service;

import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.model.MonitorEditionBackfillResult;
import com.sla.notifier.model.ReviewEventSyncResult;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.WeeklyReviewRow;
import com.sla.notifier.util.ReviewEventSyncMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewEventSyncServiceTest {

    private WeeklyReviewDataService dataService;
    private SlaReviewEventMapper mapper;
    private ExcludedDowntimeWriteService exclusionWrite;
    private MonitorEditionBackfillService routingBackfill;
    private LowSliPreWindowBackfillService preWindowBackfill;
    private ReviewEventSyncService service;

    @BeforeEach
    void setUp() {
        dataService = mock(WeeklyReviewDataService.class);
        mapper = mock(SlaReviewEventMapper.class);
        exclusionWrite = mock(ExcludedDowntimeWriteService.class);
        routingBackfill = mock(MonitorEditionBackfillService.class);
        preWindowBackfill = mock(LowSliPreWindowBackfillService.class);
        when(preWindowBackfill.collectPreWindowRows()).thenReturn(Collections.<WeeklyReviewRow>emptyList());
        ReviewSyncProperties syncProps = new ReviewSyncProperties();
        syncProps.setZone("Asia/Shanghai");
        service = new ReviewEventSyncService(
                dataService, mapper, exclusionWrite, routingBackfill, syncProps,
                new WeeklyReviewProperties(), preWindowBackfill);
    }

    @Test
    void defaultModeInsertsAndNeverUpdatesOwner() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class))).thenReturn(Collections.<SlaReviewEvent>emptyList());
        when(mapper.insertIgnore(any(SlaReviewEvent.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getInserted());
        assertEquals(0, result.getOwnerUpdated());
        verify(mapper).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper, never()).updateOwnerByIdentity(any(SlaReviewEvent.class));
        verify(mapper, never()).refreshExcludeTime();
    }

    @Test
    void recoveredCollectOverwritesSentinelEndWithoutSecondInsert() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        SlaReviewEvent sentinel = new SlaReviewEvent();
        sentinel.setId(9L);
        sentinel.setDownEnd(ReviewEventSyncMapper.UNRECOVERED_END);
        sentinel.setCountSla("是");
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class)))
                .thenReturn(Collections.singletonList(sentinel));
        when(mapper.updateAlignedDownEnd(eq(9L), any(LocalDateTime.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getEndUpdated());
        assertEquals(0, result.getInserted());
        assertEquals(0, result.getFolded());
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper).updateAlignedDownEnd(eq(9L),
                eq(LocalDateTime.of(2026, 8, 1, 11, 0, 0)));
        verify(exclusionWrite, never()).updatePeriodToAfterSync(any(SlaReviewEvent.class));
    }

    @Test
    void recoveredEndOverwritesPriorRecoveredEnd() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        SlaReviewEvent prior = new SlaReviewEvent();
        prior.setId(3L);
        prior.setDownEnd(LocalDateTime.of(2026, 8, 1, 10, 30, 0));
        prior.setCountSla("是");
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class)))
                .thenReturn(Collections.singletonList(prior));
        when(mapper.updateAlignedDownEnd(eq(3L), any(LocalDateTime.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getEndUpdated());
        assertEquals(0, result.getInserted());
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper).updateAlignedDownEnd(eq(3L),
                eq(LocalDateTime.of(2026, 8, 1, 11, 0, 0)));
    }

    @Test
    void countSlaNoUpdatesExcludeAndPeriodTo() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        SlaReviewEvent sentinel = new SlaReviewEvent();
        sentinel.setId(9L);
        sentinel.setDownEnd(ReviewEventSyncMapper.UNRECOVERED_END);
        sentinel.setCountSla("否");
        sentinel.setSystemLevel("子系统");
        sentinel.setSystemName("北京海淀交易平台");
        sentinel.setDownStart(LocalDateTime.of(2026, 8, 1, 10, 0, 0));
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class)))
                .thenReturn(Collections.singletonList(sentinel));
        when(mapper.updateAlignedDownEnd(eq(9L), any(LocalDateTime.class))).thenReturn(1);
        when(exclusionWrite.updatePeriodToAfterSync(any(SlaReviewEvent.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getEndUpdated());
        assertEquals(1, result.getExclusionPeriodToUpdated());
        verify(exclusionWrite).updatePeriodToAfterSync(any(SlaReviewEvent.class));
    }

    @Test
    void twoRecoveredRowsFoldIntoMatchingEnd() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        SlaReviewEvent oldEnd = new SlaReviewEvent();
        oldEnd.setId(1L);
        oldEnd.setDownEnd(LocalDateTime.of(2026, 8, 1, 10, 30, 0));
        oldEnd.setRootCause("旧根因");
        SlaReviewEvent matching = new SlaReviewEvent();
        matching.setId(2L);
        matching.setDownEnd(LocalDateTime.of(2026, 8, 1, 11, 0, 0));
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class)))
                .thenReturn(Arrays.asList(oldEnd, matching));

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getFolded());
        assertEquals(0, result.getEndUpdated());
        assertEquals(0, result.getInserted());
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper, never()).updateAlignedDownEnd(any(Long.class), any(LocalDateTime.class));
        verify(mapper).updateReviewFields(any(SlaReviewEvent.class));
        verify(mapper).deleteById(1L);
    }

    @Test
    void preWindowPassInsertsBeforeWindowCollect() {
        when(preWindowBackfill.collectPreWindowRows())
                .thenReturn(Collections.singletonList(preWindowRow()));
        when(dataService.collect(any(LocalDateTime.class), eq(true)))
                .thenReturn(Collections.<WeeklyReviewRow>emptyList());
        when(mapper.insertIgnore(any(SlaReviewEvent.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getPreWindowInserted());
        assertEquals(0, result.getInserted());
        assertTrue(result.getMessage().contains("730前低SLI"));
        verify(mapper).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper, never()).listByStartIdentity(any(SlaReviewEvent.class));
    }

    @Test
    void preWindowPassRunsBeforePassA() {
        when(preWindowBackfill.collectPreWindowRows()).thenReturn(Collections.<WeeklyReviewRow>emptyList());
        when(dataService.collect(any(LocalDateTime.class), eq(true)))
                .thenReturn(Collections.singletonList(row()));
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class))).thenReturn(Collections.<SlaReviewEvent>emptyList());
        when(mapper.insertIgnore(any(SlaReviewEvent.class))).thenReturn(1);

        service.runNow(false);

        org.mockito.InOrder order = inOrder(preWindowBackfill, dataService);
        order.verify(preWindowBackfill).collectPreWindowRows();
        order.verify(dataService).collect(any(LocalDateTime.class), eq(true));
    }

    @Test
    void refreshOwnerSkipsPreWindowPass() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        when(mapper.updateOwnerByIdentity(any(SlaReviewEvent.class))).thenReturn(1);

        service.runNow(true);

        verify(preWindowBackfill, never()).collectPreWindowRows();
    }

    @Test
    void refreshOwnerUpdatesAndNeverInserts() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        when(mapper.updateOwnerByIdentity(any(SlaReviewEvent.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(true);

        assertEquals(0, result.getInserted());
        assertEquals(1, result.getOwnerUpdated());
        assertEquals(0, result.getSkippedNotFound());
        verify(mapper).updateOwnerByIdentity(any(SlaReviewEvent.class));
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
    }

    @Test
    void refreshOwnerMissingRowDoesNotInsert() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        when(mapper.updateOwnerByIdentity(any(SlaReviewEvent.class))).thenReturn(0);

        ReviewEventSyncResult result = service.runNow(true);

        assertEquals(0, result.getInserted());
        assertEquals(0, result.getOwnerUpdated());
        assertEquals(1, result.getSkippedNotFound());
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
    }

    @Test
    void refreshExcludeTimeScansTableWithoutGrafana() {
        when(mapper.refreshExcludeTime()).thenReturn(4);

        ReviewEventSyncResult result = service.runNow(false, true);

        assertEquals(4, result.getExcludeTimeUpdated());
        assertEquals(0, result.getInserted());
        assertEquals(0, result.getCollected());
        verify(mapper).refreshExcludeTime();
        verify(dataService, never()).collect(any(LocalDateTime.class), anyBoolean());
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper, never()).updateOwnerByIdentity(any(SlaReviewEvent.class));
    }

    @Test
    void refreshOwnerAndExcludeTimeBothRun() {
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        when(mapper.updateOwnerByIdentity(any(SlaReviewEvent.class))).thenReturn(1);
        when(mapper.refreshExcludeTime()).thenReturn(7);

        ReviewEventSyncResult result = service.runNow(true, true);

        assertEquals(1, result.getOwnerUpdated());
        assertEquals(7, result.getExcludeTimeUpdated());
        assertEquals(0, result.getInserted());
        verify(mapper).refreshExcludeTime();
        verify(mapper).updateOwnerByIdentity(any(SlaReviewEvent.class));
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
    }

    @Test
    void partialExclusionDoesNotInsertWhenStartMissing() {
        when(dataService.collect(any(LocalDateTime.class), eq(true)))
                .thenReturn(Collections.singletonList(partialRow()));
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(0, result.getInserted());
        assertEquals(0, result.getEndUpdated());
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper, never()).updateAlignedDownEnd(any(Long.class), any(LocalDateTime.class));
    }

    @Test
    void partialExclusionOverwritesExistingEnd() {
        when(dataService.collect(any(LocalDateTime.class), eq(true)))
                .thenReturn(Collections.singletonList(partialRow()));
        SlaReviewEvent existing = new SlaReviewEvent();
        existing.setId(4L);
        existing.setDownEnd(LocalDateTime.of(2026, 8, 1, 10, 30, 0));
        existing.setCountSla("是");
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class)))
                .thenReturn(Collections.singletonList(existing));
        when(mapper.updateAlignedDownEnd(eq(4L), any(LocalDateTime.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getEndUpdated());
        assertEquals(0, result.getInserted());
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
        verify(mapper).updateAlignedDownEnd(eq(4L),
                eq(LocalDateTime.of(2026, 8, 1, 11, 0, 0)));
    }

    @Test
    void refreshRoutingOnlyRunsBackfillWithoutGrafanaCollect() {
        MonitorEditionBackfillResult backfill = new MonitorEditionBackfillResult();
        backfill.setRowsUpdated(2);
        backfill.setCollisionCount(1);
        backfill.setGrafanaDiscoveryOk(true);
        when(routingBackfill.backfill()).thenReturn(backfill);

        ReviewEventSyncResult result = service.runNow(false, false, true);

        assertEquals(2, result.getRoutingRowsUpdated());
        assertEquals(1, result.getRoutingCollisionCount());
        assertTrue(result.isRoutingGrafanaDiscoveryOk());
        assertEquals(0, result.getCollected());
        assertTrue(result.getMessage().contains("监控路由"));
        verify(routingBackfill).backfill();
        verify(dataService, never()).collect(any(LocalDateTime.class), anyBoolean());
    }

    @Test
    void refreshRoutingBeforeOwnerRefresh() {
        MonitorEditionBackfillResult backfill = new MonitorEditionBackfillResult();
        backfill.setRowsUpdated(3);
        when(routingBackfill.backfill()).thenReturn(backfill);
        when(dataService.collect(any(LocalDateTime.class), eq(true))).thenReturn(Collections.singletonList(row()));
        when(mapper.updateOwnerByIdentity(any(SlaReviewEvent.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(true, false, true);

        assertEquals(3, result.getRoutingRowsUpdated());
        assertEquals(1, result.getOwnerUpdated());
        assertTrue(result.getMessage().contains("监控路由更新"));
        verify(routingBackfill).backfill();
        verify(mapper).updateOwnerByIdentity(any(SlaReviewEvent.class));
    }

    @Test
    void refreshRoutingWithExcludeTimeRunsBothWithoutCollect() {
        MonitorEditionBackfillResult backfill = new MonitorEditionBackfillResult();
        backfill.setRowsUpdated(5);
        when(routingBackfill.backfill()).thenReturn(backfill);
        when(mapper.refreshExcludeTime()).thenReturn(4);

        ReviewEventSyncResult result = service.runNow(false, true, true);

        assertEquals(5, result.getRoutingRowsUpdated());
        assertEquals(4, result.getExcludeTimeUpdated());
        assertTrue(result.getMessage().contains("exclude_time"));
        assertTrue(result.getMessage().contains("监控路由更新"));
        verify(dataService, never()).collect(any(LocalDateTime.class), anyBoolean());
    }

    @Test
    void partialExclusionNoSlaUpdatesPeriodTo() {
        when(dataService.collect(any(LocalDateTime.class), eq(true)))
                .thenReturn(Collections.singletonList(partialRow()));
        SlaReviewEvent existing = new SlaReviewEvent();
        existing.setId(4L);
        existing.setDownEnd(LocalDateTime.of(2026, 8, 1, 10, 30, 0));
        existing.setCountSla("否");
        existing.setSystemLevel("子系统");
        existing.setSystemName("北京海淀交易平台");
        existing.setDownStart(LocalDateTime.of(2026, 8, 1, 10, 0, 0));
        when(mapper.listByStartIdentity(any(SlaReviewEvent.class)))
                .thenReturn(Collections.singletonList(existing));
        when(mapper.updateAlignedDownEnd(eq(4L), any(LocalDateTime.class))).thenReturn(1);
        when(exclusionWrite.updatePeriodToAfterSync(any(SlaReviewEvent.class))).thenReturn(1);

        ReviewEventSyncResult result = service.runNow(false);

        assertEquals(1, result.getEndUpdated());
        assertEquals(1, result.getExclusionPeriodToUpdated());
        verify(exclusionWrite).updatePeriodToAfterSync(any(SlaReviewEvent.class));
        verify(mapper, never()).insertIgnore(any(SlaReviewEvent.class));
    }

    private static WeeklyReviewRow row() {
        return new WeeklyReviewRow("子系统", "北京海淀交易平台",
                "2026-08-01 10:00:00", "2026-08-01 11:00:00",
                "王华聪", "13521207069", true);
    }

    private static WeeklyReviewRow partialRow() {
        return new WeeklyReviewRow("子系统", "北京海淀交易平台",
                "2026-08-01 10:00:00", "2026-08-01 11:00:00",
                "王华聪", "13521207069", true, "部分排除");
    }

    private static WeeklyReviewRow preWindowRow() {
        return new WeeklyReviewRow("子系统", "北京海淀交易平台",
                "2026-06-01 10:00:00", "2026-06-01 11:00:00",
                "王华聪", "13521207069", true, "未排除");
    }
}
