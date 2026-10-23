package com.sla.notifier.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class WeeklyReviewNaming {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private WeeklyReviewNaming() {
    }

    public static String documentTitle(LocalDateTime triggerTime) {
        if (triggerTime == null) {
            throw new IllegalArgumentException("triggerTime 不能为空");
        }
        return "复盘明细-" + DATE_FORMAT.format(triggerTime);
    }

    public static String workbookFilename(LocalDateTime triggerTime) {
        return documentTitle(triggerTime) + ".xlsx";
    }
}
