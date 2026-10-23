package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.ExcludedDowntimeBackfillResult;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.SlaSystemOwner;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcludedDowntimeBackfillServiceTest {

    @Test
    void dryRunListsUnexcludedAndDoesNotWrite() {
        SlaReviewEventMapper reviews = mock(SlaReviewEventMapper.class);
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        ExcludedDowntimeWriteService writer = spy(new ExcludedDowntimeWriteService(
                owners, z6, mock(JdbcTemplate.class)));
        SlaReviewEvent event = reviewed(1L, "晋中系统");
        when(reviews.listReviewedEvents()).thenReturn(Collections.singletonList(event));
        when(owners.selectByLevelAndName("子系统", "晋中系统")).thenReturn(owner());
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("晋中系统")))
                .thenReturn(Long.valueOf(88L));
        when(z6.queryForObject(eq(ExcludedDowntimeWriteService.SQL_OVERLAP_COUNT), eq(Long.class),
                eq(Long.valueOf(88L)), any(), any())).thenReturn(Long.valueOf(0L));

        ExcludedDowntimeBackfillResult result = new ExcludedDowntimeBackfillService(reviews, owners, writer)
                .run(true);

        assertTrue(result.isDryRun());
        assertEquals(1, result.getReviewed());
        assertEquals(1, result.getWouldWrite().size());
        assertEquals(0, result.getWritten().size());
        assertEquals(0, result.getSkippedNoSla().size());
        verify(writer, never()).writeAfterSave(any(SlaReviewEvent.class));
    }

    @Test
    void missingSlaNameIsSkipped() {
        SlaReviewEventMapper reviews = mock(SlaReviewEventMapper.class);
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        ExcludedDowntimeWriteService writer = spy(new ExcludedDowntimeWriteService(
                owners, z6, mock(JdbcTemplate.class)));
        SlaReviewEvent event = reviewed(160L, "石家庄工程开评标系统");
        when(reviews.listReviewedEvents()).thenReturn(Collections.singletonList(event));
        when(owners.selectByLevelAndName("子系统", "石家庄工程开评标系统")).thenReturn(owner());
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class),
                eq("石家庄工程开评标系统"))).thenThrow(new EmptyResultDataAccessException(1));

        ExcludedDowntimeBackfillResult result = new ExcludedDowntimeBackfillService(reviews, owners, writer)
                .run(false);

        assertEquals(1, result.getSkippedNoSla().size());
        assertEquals(Long.valueOf(160L), result.getSkippedNoSla().get(0).getId());
        assertEquals(0, result.getWouldWrite().size());
        verify(writer, never()).writeAfterSave(any(SlaReviewEvent.class));
    }

    @Test
    void executeCallsWriterForUnexcluded() {
        SlaReviewEventMapper reviews = mock(SlaReviewEventMapper.class);
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        ExcludedDowntimeWriteService writer = spy(new ExcludedDowntimeWriteService(
                owners, z6, mock(JdbcTemplate.class)));
        SlaReviewEvent event = reviewed(4925L, "晋中系统");
        when(reviews.listReviewedEvents()).thenReturn(Collections.singletonList(event));
        when(owners.selectByLevelAndName("子系统", "晋中系统")).thenReturn(owner());
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("晋中系统")))
                .thenReturn(Long.valueOf(88L));
        when(z6.queryForObject(eq(ExcludedDowntimeWriteService.SQL_OVERLAP_COUNT), eq(Long.class),
                eq(Long.valueOf(88L)), any(), any())).thenReturn(Long.valueOf(0L));
        doNothing().when(writer).writeAfterSave(event);

        ExcludedDowntimeBackfillResult result = new ExcludedDowntimeBackfillService(reviews, owners, writer)
                .run(false);

        verify(writer).writeAfterSave(event);
        assertEquals(1, result.getWritten().size());
        assertEquals(0, result.getFailed().size());
    }

    @Test
    void alreadyOverlappingIsNotWritten() {
        SlaReviewEventMapper reviews = mock(SlaReviewEventMapper.class);
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        ExcludedDowntimeWriteService writer = spy(new ExcludedDowntimeWriteService(
                owners, z6, mock(JdbcTemplate.class)));
        SlaReviewEvent event = reviewed(9L, "已排除系统");
        when(reviews.listReviewedEvents()).thenReturn(Collections.singletonList(event));
        when(owners.selectByLevelAndName("子系统", "已排除系统")).thenReturn(owner());
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("已排除系统")))
                .thenReturn(Long.valueOf(5L));
        when(z6.queryForObject(eq(ExcludedDowntimeWriteService.SQL_OVERLAP_COUNT), eq(Long.class),
                eq(Long.valueOf(5L)), any(), any())).thenReturn(Long.valueOf(1L));

        ExcludedDowntimeBackfillResult result = new ExcludedDowntimeBackfillService(reviews, owners, writer)
                .run(false);

        assertEquals(1, result.getSkippedAlreadyExcluded());
        assertEquals(0, result.getWouldWrite().size());
        verify(writer, never()).writeAfterSave(any(SlaReviewEvent.class));
    }

    @Test
    void executeContinuesAfterOneFailure() {
        SlaReviewEventMapper reviews = mock(SlaReviewEventMapper.class);
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        ExcludedDowntimeWriteService writer = spy(new ExcludedDowntimeWriteService(
                owners, z6, mock(JdbcTemplate.class)));
        SlaReviewEvent bad = reviewed(1L, "甲");
        SlaReviewEvent ok = reviewed(2L, "乙");
        when(reviews.listReviewedEvents()).thenReturn(Arrays.asList(bad, ok));
        when(owners.selectByLevelAndName("子系统", "甲")).thenReturn(owner());
        when(owners.selectByLevelAndName("子系统", "乙")).thenReturn(owner());
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("甲")))
                .thenReturn(Long.valueOf(1L));
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("乙")))
                .thenReturn(Long.valueOf(2L));
        when(z6.queryForObject(eq(ExcludedDowntimeWriteService.SQL_OVERLAP_COUNT), eq(Long.class),
                any(), any(), any())).thenReturn(Long.valueOf(0L));
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(writer).writeAfterSave(bad);
        doNothing().when(writer).writeAfterSave(ok);

        ExcludedDowntimeBackfillResult result = new ExcludedDowntimeBackfillService(reviews, owners, writer)
                .run(false);

        assertEquals(1, result.getFailed().size());
        assertEquals(1, result.getWritten().size());
        assertEquals(Long.valueOf(2L), result.getWritten().get(0).getId());
    }

    private static SlaSystemOwner owner() {
        SlaSystemOwner owner = new SlaSystemOwner();
        owner.setWriteJdbcKey("zabbix6");
        return owner;
    }

    private static SlaReviewEvent reviewed(Long id, String name) {
        SlaReviewEvent event = new SlaReviewEvent();
        event.setId(id);
        event.setSystemLevel("子系统");
        event.setSystemName(name);
        event.setDownStart(LocalDateTime.of(2026, 7, 30, 13, 14, 54));
        event.setDownEnd(LocalDateTime.of(2026, 7, 30, 13, 17, 5));
        event.setExcludeTime(LocalDateTime.of(2026, 7, 30, 13, 17, 5));
        event.setCountSla("否");
        event.setFaultCategory("间接性故障");
        event.setFaultSubcategory("运营商公网故障");
        event.setFaultDesc("公网");
        return event;
    }
}
