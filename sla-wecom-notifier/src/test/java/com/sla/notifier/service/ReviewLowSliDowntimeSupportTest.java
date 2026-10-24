package com.sla.notifier.service;

import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.model.SlaLowSliItem;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewLowSliDowntimeSupportTest {

    @Test
    void expandMatchesExcelStyleKeysAndFiltersExclusion() {
        SlaLowSliItem low = item("子系统", "SysA", 99.0);
        SlaLowSliItem parentSystem = item("父系统", "ParentB", 98.5);

        DowntimeEvent included = event("SysA", "子系统", "2026-06-01 10:00:00", "未排除");
        DowntimeEvent hyphen = event("SysA", "子系统", "2026-06-02 11:00:00", "-");
        DowntimeEvent partial = event("SysA", "子系统", "2026-06-03 12:00:00", "部分排除");
        DowntimeEvent parentRow = event("ParentB", "父系统", "2026-05-01 09:00:00", "未排除");

        Map<String, List<DowntimeEvent>> leaf = new HashMap<String, List<DowntimeEvent>>();
        leaf.put("SysA", Arrays.asList(included, hyphen, partial));
        Map<String, List<DowntimeEvent>> parentIndex = new HashMap<String, List<DowntimeEvent>>();
        parentIndex.put("ParentB", Collections.singletonList(parentRow));

        List<ReviewLowSliDowntimeSupport.LowSliDowntimeRow> expanded =
                ReviewLowSliDowntimeSupport.expandUnexcludedDowntimes(
                        Arrays.asList(low, parentSystem), leaf, parentIndex);

        Set<String> keys = new HashSet<String>();
        for (ReviewLowSliDowntimeSupport.LowSliDowntimeRow row : expanded) {
            keys.add(ReviewLowSliDowntimeSupport.rowKey(row));
        }
        assertEquals(3, keys.size());
        assertTrue(keys.contains("SysA|子系统|2026-06-01 10:00:00"));
        assertTrue(keys.contains("SysA|子系统|2026-06-02 11:00:00"));
        assertTrue(keys.contains("ParentB|父系统|2026-05-01 09:00:00"));
        assertFalse(keys.contains("SysA|子系统|2026-06-03 12:00:00"));
    }

    @Test
    void filterLowSliUsesThreshold() {
        SlaLowSliItem below = item("子系统", "A", 99.89);
        SlaLowSliItem at = item("子系统", "B", 99.9);
        List<SlaLowSliItem> low = ReviewLowSliDowntimeSupport.filterLowSli(
                Arrays.asList(below, at), 99.9);
        assertEquals(1, low.size());
        assertEquals("A", low.get(0).getSystemName());
    }

    private static SlaLowSliItem item(String level, String name, double sli) {
        SlaLowSliItem item = new SlaLowSliItem();
        item.setSourceLevel(level);
        item.setSystemName(name);
        item.setAnnualSliValue(sli);
        return item;
    }

    private static DowntimeEvent event(String name, String level, String start, String exclusion) {
        DowntimeEvent event = new DowntimeEvent();
        event.setSystemName(name);
        event.setSystemLevel(level);
        event.setStartDisplay(start);
        event.setEndDisplay("2026-06-01 12:00:00");
        event.setExclusionStatus(exclusion);
        return event;
    }
}
