package com.sla.notifier.util;

import com.sla.notifier.model.DowntimeEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 当月宕机人工复盘判定：未复盘 = 排除状态为「未排除」或排除原因为「-」。
 */
public final class ReviewStatusUtil {

    private ReviewStatusUtil() {
    }

    /**
     * 单条未复盘：状态=未排除 或 原因=-；状态/原因缺省视为未复盘（保守推送）。
     */
    public static boolean isUnreviewed(DowntimeEvent event) {
        if (event == null) {
            return true;
        }
        return isUnreviewed(event.getExclusionStatus(), event.getExclusionReason());
    }

    public static boolean isUnreviewed(String exclusionStatus, String exclusionReason) {
        String status = exclusionStatus == null ? "" : exclusionStatus.trim();
        String reason = exclusionReason == null ? "" : exclusionReason.trim();
        if (status.isEmpty() || reason.isEmpty()) {
            return true;
        }
        if ("未排除".equals(status)) {
            return true;
        }
        if ("-".equals(reason)) {
            return true;
        }
        return false;
    }

    public static boolean isReviewed(DowntimeEvent event) {
        return !isUnreviewed(event);
    }

    public static boolean isInCurrentMonth(LocalDateTime startTime) {
        if (startTime == null) {
            return false;
        }
        YearMonth current = YearMonth.from(LocalDate.now(TimeFormatUtil.ZONE));
        return YearMonth.from(startTime).equals(current);
    }

    public static LocalDateTime currentMonthStart() {
        return YearMonth.from(LocalDate.now(TimeFormatUtil.ZONE)).atDay(1).atStartOfDay();
    }

    public static LocalDateTime nextMonthStart() {
        return YearMonth.from(LocalDate.now(TimeFormatUtil.ZONE)).plusMonths(1).atDay(1).atStartOfDay();
    }

    /**
     * 当月存在宕机且全部已复盘 → true（企微可跳过）。
     * 无当月宕机或任一条未复盘 → false（仍推送）。
     */
    public static boolean isMonthFullyReviewed(List<DowntimeEvent> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }
        int currentMonthCount = 0;
        for (DowntimeEvent event : events) {
            if (!isInCurrentMonth(event.getStartTime())) {
                continue;
            }
            currentMonthCount++;
            if (isUnreviewed(event)) {
                return false;
            }
        }
        return currentMonthCount > 0;
    }
}
