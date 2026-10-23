package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.SlaSystemOwner;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcludedDowntimeWriteServiceTest {

    @Test
    void zabbix7PathUsesZabbix7Template() {
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        JdbcTemplate z7 = mock(JdbcTemplate.class);
        when(owners.selectByLevelAndName("子系统", "新版系统")).thenReturn(owner("zabbix7"));
        when(z7.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("新版系统")))
                .thenReturn(Long.valueOf(88L));
        when(z7.queryForList(eq(ExcludedDowntimeWriteService.SQL_IDS_BY_START), eq(Long.class),
                eq(Long.valueOf(88L)), eq(Integer.valueOf(1785388494))))
                .thenReturn(Collections.<Long>emptyList());
        when(z7.queryForObject(eq("SELECT nextid FROM ids WHERE table_name = ? AND field_name = ?"),
                eq(Long.class), eq("sla_excluded_downtime"), eq("sla_excluded_downtimeid")))
                .thenReturn(Long.valueOf(10L));
        when(z7.update(eq("UPDATE ids SET nextid = ? WHERE table_name = ? AND field_name = ?"),
                eq(Long.valueOf(11L)), eq("sla_excluded_downtime"), eq("sla_excluded_downtimeid")))
                .thenReturn(1);

        new ExcludedDowntimeWriteService(owners, z6, z7).writeAfterSave(saved("新版系统"));

        verify(z7).update(eq(
                        "INSERT INTO sla_excluded_downtime (sla_excluded_downtimeid, slaid, name, period_from, period_to) "
                                + "VALUES (?, ?, ?, ?, ?)"),
                eq(Long.valueOf(11L)), eq(Long.valueOf(88L)), anyString(), any(), any());
        verify(z6, never()).update(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void missingSlaNameFails() {
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        when(owners.selectByLevelAndName("子系统", "新版系统")).thenReturn(owner("zabbix6"));
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("新版系统")))
                .thenThrow(new EmptyResultDataAccessException(1));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new ExcludedDowntimeWriteService(owners, z6, mock(JdbcTemplate.class))
                        .writeAfterSave(saved("新版系统")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void sameWindowResaveUpdatesInsteadOfInsert() {
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        when(owners.selectByLevelAndName("子系统", "新版系统")).thenReturn(owner("zabbix6"));
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("新版系统")))
                .thenReturn(Long.valueOf(5L));
        when(z6.queryForList(eq(ExcludedDowntimeWriteService.SQL_IDS_BY_START), eq(Long.class),
                eq(Long.valueOf(5L)), eq(Integer.valueOf(1785388494))))
                .thenReturn(Collections.singletonList(Long.valueOf(7L)));

        SlaReviewEvent event = saved("新版系统");
        new ExcludedDowntimeWriteService(owners, z6, mock(JdbcTemplate.class)).writeAfterSave(event);

        verify(z6).update(eq(ExcludedDowntimeWriteService.SQL_UPDATE_WINDOW),
                eq(ExcludedDowntimeWriteService.exclusionName(event)),
                eq(Integer.valueOf(1785388495)), eq(Long.valueOf(7L)));
        verify(z6, never()).update(eq(
                        "INSERT INTO sla_excluded_downtime (sla_excluded_downtimeid, slaid, name, period_from, period_to) "
                                + "VALUES (?, ?, ?, ?, ?)"),
                any(), any(), any(), any(), any());
    }

    @Test
    void countSlaChangeUpdatesPeriodToWithoutSecondInsert() {
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        when(owners.selectByLevelAndName("子系统", "新版系统")).thenReturn(owner("zabbix6"));
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("新版系统")))
                .thenReturn(Long.valueOf(5L));
        when(z6.queryForList(eq(ExcludedDowntimeWriteService.SQL_IDS_BY_START), eq(Long.class),
                eq(Long.valueOf(5L)), eq(Integer.valueOf(1785388494))))
                .thenReturn(Collections.singletonList(Long.valueOf(7L)));

        SlaReviewEvent event = saved("新版系统");
        event.setExcludeTime(LocalDateTime.of(2026, 7, 30, 13, 17, 5));
        int periodTo = ExcludedDowntimeWriteService.toUnix(event.getExcludeTime());
        new ExcludedDowntimeWriteService(owners, z6, mock(JdbcTemplate.class)).writeAfterSave(event);

        verify(z6).update(eq(ExcludedDowntimeWriteService.SQL_UPDATE_WINDOW),
                eq(ExcludedDowntimeWriteService.exclusionName(event)),
                eq(Integer.valueOf(periodTo)), eq(Long.valueOf(7L)));
        verify(z6, never()).update(eq(ExcludedDowntimeWriteService.SQL_DELETE_BY_ID), any(Long.class));
        verify(z6, never()).update(eq(
                        "INSERT INTO sla_excluded_downtime (sla_excluded_downtimeid, slaid, name, period_from, period_to) "
                                + "VALUES (?, ?, ?, ?, ?)"),
                any(), any(), any(), any(), any());
    }

    @Test
    void duplicateStartWindowsCollapseToOneRow() {
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        when(owners.selectByLevelAndName("子系统", "新版系统")).thenReturn(owner("zabbix6"));
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("新版系统")))
                .thenReturn(Long.valueOf(5L));
        when(z6.queryForList(eq(ExcludedDowntimeWriteService.SQL_IDS_BY_START), eq(Long.class),
                eq(Long.valueOf(5L)), eq(Integer.valueOf(1785388494))))
                .thenReturn(Arrays.asList(Long.valueOf(3L), Long.valueOf(9L)));

        SlaReviewEvent event = saved("新版系统");
        new ExcludedDowntimeWriteService(owners, z6, mock(JdbcTemplate.class)).writeAfterSave(event);

        verify(z6).update(eq(ExcludedDowntimeWriteService.SQL_UPDATE_WINDOW),
                eq(ExcludedDowntimeWriteService.exclusionName(event)),
                eq(Integer.valueOf(1785388495)), eq(Long.valueOf(3L)));
        verify(z6).update(eq(ExcludedDowntimeWriteService.SQL_DELETE_BY_ID), eq(Long.valueOf(9L)));
        verify(z6, never()).update(eq(
                        "INSERT INTO sla_excluded_downtime (sla_excluded_downtimeid, slaid, name, period_from, period_to) "
                                + "VALUES (?, ?, ?, ?, ?)"),
                any(), any(), any(), any(), any());
    }

    @Test
    void syncPeriodToUpdatesExistingWithoutInsert() {
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        when(owners.selectByLevelAndName("子系统", "新版系统")).thenReturn(owner("zabbix6"));
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("新版系统")))
                .thenReturn(Long.valueOf(5L));
        when(z6.queryForList(eq(ExcludedDowntimeWriteService.SQL_IDS_BY_START), eq(Long.class),
                eq(Long.valueOf(5L)), eq(Integer.valueOf(1785388494))))
                .thenReturn(Collections.singletonList(Long.valueOf(7L)));
        when(z6.update(eq(ExcludedDowntimeWriteService.SQL_UPDATE_PERIOD_TO_ONLY),
                eq(Integer.valueOf(1785388495)), eq(Long.valueOf(7L)))).thenReturn(1);

        SlaReviewEvent event = saved("新版系统");
        event.setCountSla("否");
        int updated = new ExcludedDowntimeWriteService(owners, z6, mock(JdbcTemplate.class))
                .updatePeriodToAfterSync(event);

        assertEquals(1, updated);
        verify(z6).update(eq(ExcludedDowntimeWriteService.SQL_UPDATE_PERIOD_TO_ONLY),
                eq(Integer.valueOf(1785388495)), eq(Long.valueOf(7L)));
        verify(z6, never()).update(eq(
                        "INSERT INTO sla_excluded_downtime (sla_excluded_downtimeid, slaid, name, period_from, period_to) "
                                + "VALUES (?, ?, ?, ?, ?)"),
                any(), any(), any(), any(), any());
    }

    @Test
    void syncPeriodToMissingExclusionDoesNotInsert() {
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        when(owners.selectByLevelAndName("子系统", "新版系统")).thenReturn(owner("zabbix6"));
        when(z6.queryForObject(eq("SELECT slaid FROM sla WHERE name = ?"), eq(Long.class), eq("新版系统")))
                .thenReturn(Long.valueOf(5L));
        when(z6.queryForList(eq(ExcludedDowntimeWriteService.SQL_IDS_BY_START), eq(Long.class),
                eq(Long.valueOf(5L)), eq(Integer.valueOf(1785388494))))
                .thenReturn(Collections.<Long>emptyList());

        SlaReviewEvent event = saved("新版系统");
        event.setCountSla("否");
        int updated = new ExcludedDowntimeWriteService(owners, z6, mock(JdbcTemplate.class))
                .updatePeriodToAfterSync(event);

        assertEquals(0, updated);
        verify(z6, never()).update(anyString(), any(), any());
        verify(z6, never()).update(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void unixConversionUsesShanghai() {
        int unix = ExcludedDowntimeWriteService.toUnix(LocalDateTime.of(2026, 7, 30, 13, 14, 54));
        assertEquals(1785388494, unix);
    }

    @Test
    void nameIsConcatenatedAndTruncated() {
        SlaReviewEvent event = new SlaReviewEvent();
        event.setFaultCategory("直接性故障");
        event.setFaultSubcategory("应用类故障");
        event.setFaultDesc("短");
        assertEquals("故障分类:直接性故障--故障子类:应用类故障--故障描述:短",
                ExcludedDowntimeWriteService.exclusionName(event));

        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            desc.append("a");
        }
        event.setFaultDesc(desc.toString());
        assertEquals(255, ExcludedDowntimeWriteService.exclusionName(event).length());
    }

    @Test
    void jdbcForSelectsPool() {
        JdbcTemplate z6 = mock(JdbcTemplate.class);
        JdbcTemplate z7 = mock(JdbcTemplate.class);
        ExcludedDowntimeWriteService service =
                new ExcludedDowntimeWriteService(mock(SlaSystemOwnerMapper.class), z6, z7);
        assertSame(z6, service.jdbcFor("zabbix6"));
        assertSame(z7, service.jdbcFor("zabbix7"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.jdbcFor("other"));
        assertTrue(error.getReason().contains("写库标记"));
    }

    private static SlaSystemOwner owner(String key) {
        SlaSystemOwner owner = new SlaSystemOwner();
        owner.setWriteJdbcKey(key);
        return owner;
    }

    private static SlaReviewEvent saved(String name) {
        SlaReviewEvent event = new SlaReviewEvent();
        event.setSystemLevel("子系统");
        event.setSystemName(name);
        event.setDownStart(LocalDateTime.of(2026, 7, 30, 13, 14, 54));
        event.setExcludeTime(LocalDateTime.of(2026, 7, 30, 13, 14, 55));
        event.setFaultCategory("直接性故障");
        event.setFaultSubcategory("应用类故障");
        event.setFaultDesc("应用异常");
        return event;
    }
}
