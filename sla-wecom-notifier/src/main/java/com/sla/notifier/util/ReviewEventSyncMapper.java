package com.sla.notifier.util;

import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.WeeklyReviewRow;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class ReviewEventSyncMapper {

    public static final LocalDateTime UNRECOVERED_END =
            LocalDateTime.of(9999, 12, 31, 0, 0, 0);

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("yyyyMM");

    private ReviewEventSyncMapper() {
    }

    /**
     * @return null when start time cannot be parsed
     */
    public static SlaReviewEvent fromRow(WeeklyReviewRow row, ZoneId zone) {
        if (row == null) {
            return null;
        }
        LocalDateTime start = parseStart(row.getDowntimeStart());
        if (start == null) {
            return null;
        }
        SlaReviewEvent event = new SlaReviewEvent();
        event.setPeriodMonth(MONTH.format(start));
        event.setSystemLevel(trimToEmpty(row.getSystemLevel()));
        event.setSystemName(trimToEmpty(row.getSystemName()));
        event.setDownStart(start);
        event.setDownEnd(mapEnd(row.getDowntimeEnd()));
        event.setOwnerName(trimToNull(row.getOwnerName()));
        return event;
    }

    public static LocalDateTime parseStart(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DISPLAY);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(value.trim());
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    static LocalDateTime mapEnd(String value) {
        if (value == null) {
            return UNRECOVERED_END;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "未恢复".equals(trimmed) || "-".equals(trimmed)) {
            return UNRECOVERED_END;
        }
        try {
            return LocalDateTime.parse(trimmed, DISPLAY);
        } catch (DateTimeParseException e) {
            return UNRECOVERED_END;
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
