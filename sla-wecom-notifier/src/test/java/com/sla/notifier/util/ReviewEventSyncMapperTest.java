package com.sla.notifier.util;

import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.WeeklyReviewRow;
import com.sla.notifier.service.ReviewEventSyncService;
import com.sla.notifier.service.WeeklyReviewDataService;
import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.config.WeeklyReviewProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewEventSyncMapperTest {

    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    @Test
    void periodMonthFromDowntimeStartEvenInLaterMonth() {
        WeeklyReviewRow row = row("2026-07-30 13:14:54", "2026-07-30 13:17:05");
        SlaReviewEvent event = ReviewEventSyncMapper.fromRow(row, SH);
        assertNotNull(event);
        assertEquals("202607", event.getPeriodMonth());
        assertEquals(LocalDateTime.of(2026, 7, 30, 13, 14, 54), event.getDownStart());
        assertEquals(LocalDateTime.of(2026, 7, 30, 13, 17, 5), event.getDownEnd());
        assertEquals("卓松林", event.getOwnerName());
        assertNull(getFaultCategoryIfPresent(event));
    }

    @Test
    void unrecoveredEndUsesSentinel() {
        WeeklyReviewRow blank = row("2026-08-01 10:00:00", "");
        assertEquals(ReviewEventSyncMapper.UNRECOVERED_END,
                ReviewEventSyncMapper.fromRow(blank, SH).getDownEnd());
        WeeklyReviewRow text = row("2026-08-01 10:00:00", "未恢复");
        assertEquals(ReviewEventSyncMapper.UNRECOVERED_END,
                ReviewEventSyncMapper.fromRow(text, SH).getDownEnd());
        assertEquals("202608", ReviewEventSyncMapper.fromRow(text, SH).getPeriodMonth());
    }

    @Test
    void unparseableStartReturnsNull() {
        assertNull(ReviewEventSyncMapper.fromRow(row("bad-time", "2026-08-01 11:00:00"), SH));
    }

    @Test
    void insertSqlDoesNotTouchCategoryColumns() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(
                "src/main/resources/mapper/SlaReviewEventMapper.xml"));
        String xml = new String(bytes, StandardCharsets.UTF_8);
        String insert = xml.substring(xml.indexOf("<insert id=\"insertIgnore\">"),
                xml.indexOf("</insert>") + "</insert>".length());
        assertFalse(insert.contains("fault_category"));
        assertFalse(insert.contains("fault_subcategory"));
        assertFalse(insert.contains("fault_desc"));
        assertFalse(insert.contains("count_sla"));
        assertFalse(insert.contains("resolution_process"));
        assertFalse(insert.contains("root_cause"));
        assertFalse(insert.contains("improvement"));
        assertFalse(insert.contains("exclude_time"));
        assertFalse(xml.contains("AND down_end = #{downEnd}"));
    }

    @Test
    void duplicateInsertIsSkippedWithoutSecondWrite() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.insertIgnore(any(SlaReviewEvent.class))).thenReturn(1, 0);
        WeeklyReviewDataService data = new WeeklyReviewDataService(null, null, null,
                new WeeklyReviewProperties()) {
            @Override
            public java.util.List<WeeklyReviewRow> collect(LocalDateTime triggerTime,
                                                           boolean includeExcluded) {
                return Collections.singletonList(row("2026-07-30 13:14:54", "2026-07-30 13:17:05"));
            }
        };
        com.sla.notifier.service.LowSliPreWindowBackfillService preWindow =
                mock(com.sla.notifier.service.LowSliPreWindowBackfillService.class);
        when(preWindow.collectPreWindowRows()).thenReturn(Collections.emptyList());
        ReviewEventSyncService service = new ReviewEventSyncService(
                data, mapper, mock(com.sla.notifier.service.ExcludedDowntimeWriteService.class),
                mock(com.sla.notifier.service.MonitorEditionBackfillService.class),
                new ReviewSyncProperties(), new WeeklyReviewProperties(), preWindow);
        assertEquals(1, service.run(LocalDateTime.of(2026, 8, 22, 17, 0)).getInserted());
        com.sla.notifier.model.ReviewEventSyncResult second =
                service.run(LocalDateTime.of(2026, 8, 22, 17, 0));
        assertEquals(0, second.getInserted());
        assertEquals(1, second.getSkippedExisting());
    }

    private static WeeklyReviewRow row(String start, String end) {
        return new WeeklyReviewRow("子系统", "山西大同工程交易系统门户",
                start, end, "卓松林", "13800000000", true);
    }

    private static String getFaultCategoryIfPresent(SlaReviewEvent event) {
        try {
            java.lang.reflect.Method m = event.getClass().getMethod("getFaultCategory");
            Object v = m.invoke(event);
            return v == null ? null : String.valueOf(v);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
