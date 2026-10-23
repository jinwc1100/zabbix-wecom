package com.sla.notifier.util;

import java.time.LocalDateTime;

public final class ExcludeTimes {

    private ExcludeTimes() {
    }

    /**
     * {@code 是}: {@code down_start + 1s} (not clamped to {@code down_end});
     * {@code 否}: {@code down_end}; blank/null: {@code null}.
     */
    public static LocalDateTime derive(String countSla, LocalDateTime downStart, LocalDateTime downEnd) {
        if (countSla == null) {
            return null;
        }
        String value = countSla.trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("否".equals(value)) {
            return downEnd;
        }
        if ("是".equals(value)) {
            if (downStart == null) {
                return downEnd;
            }
            return downStart.plusSeconds(1);
        }
        return null;
    }
}
