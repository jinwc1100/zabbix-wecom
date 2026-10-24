package com.sla.notifier.service;

import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.model.SlaLowSliItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Low-SLI downtime expansion aligned with review Excel sheet 1. */
public final class ReviewLowSliDowntimeSupport {

    private ReviewLowSliDowntimeSupport() {
    }

    public static boolean isUnexcluded(String status) {
        return WeeklyReviewDataService.isUnexcluded(status);
    }

    public static List<SlaLowSliItem> filterLowSli(List<SlaLowSliItem> inventory, double threshold) {
        List<SlaLowSliItem> low = new ArrayList<SlaLowSliItem>();
        if (inventory == null) {
            return low;
        }
        for (SlaLowSliItem item : inventory) {
            if (item.getAnnualSliValue() < threshold) {
                low.add(item);
            }
        }
        return low;
    }

    /**
     * One entry per unexcluded downtime row for the given systems (no time filter).
     */
    public static List<LowSliDowntimeRow> expandUnexcludedDowntimes(
            List<SlaLowSliItem> systems,
            Map<String, List<DowntimeEvent>> leafIndex,
            Map<String, List<DowntimeEvent>> parentIndex) {
        List<LowSliDowntimeRow> rows = new ArrayList<LowSliDowntimeRow>();
        if (systems == null) {
            return rows;
        }
        for (SlaLowSliItem item : systems) {
            String name = item.getSystemName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            String level = item.getSourceLevel();
            List<DowntimeEvent> events;
            if ("父系统".equals(level)) {
                events = parentIndex == null ? null : parentIndex.get(name);
            } else {
                events = leafIndex == null ? null : leafIndex.get(name);
            }
            if (events == null || events.isEmpty()) {
                continue;
            }
            String category = level == null ? "" : level;
            for (DowntimeEvent event : events) {
                if (!isUnexcluded(event.getExclusionStatus())) {
                    continue;
                }
                rows.add(new LowSliDowntimeRow(
                        name,
                        category,
                        nullToEmpty(event.getStartDisplay()),
                        nullToEmpty(event.getEndDisplay()),
                        event.getExclusionStatus()));
            }
        }
        return rows;
    }

    /** Stable key for Excel row alignment tests. */
    public static String rowKey(LowSliDowntimeRow row) {
        return row.getSystemName() + "|" + row.getSystemLevel() + "|" + row.getStartDisplay();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static final class LowSliDowntimeRow {
        private final String systemName;
        private final String systemLevel;
        private final String startDisplay;
        private final String endDisplay;
        private final String exclusionStatus;

        public LowSliDowntimeRow(String systemName, String systemLevel,
                                 String startDisplay, String endDisplay,
                                 String exclusionStatus) {
            this.systemName = systemName;
            this.systemLevel = systemLevel;
            this.startDisplay = startDisplay;
            this.endDisplay = endDisplay;
            this.exclusionStatus = exclusionStatus;
        }

        public String getSystemName() {
            return systemName;
        }

        public String getSystemLevel() {
            return systemLevel;
        }

        public String getStartDisplay() {
            return startDisplay;
        }

        public String getEndDisplay() {
            return endDisplay;
        }

        public String getExclusionStatus() {
            return exclusionStatus;
        }
    }
}
