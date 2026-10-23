package com.sla.notifier.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class TimeFormatUtil {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern DIGITS = Pattern.compile("^\\d+$");

    private TimeFormatUtil() {
    }

    /**
     * 将 Grafana 返回的时间（毫秒/秒时间戳或已格式化字符串）转为展示字符串。
     * 「未恢复」等非时间文案原样返回。
     */
    public static String formatDisplay(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if ("未恢复".equals(trimmed) || trimmed.contains("未恢复")) {
            return trimmed;
        }
        LocalDateTime ldt = parseToLocalDateTime(trimmed);
        if (ldt == null) {
            return trimmed;
        }
        return OUT.format(ldt);
    }

    public static LocalDateTime parseToLocalDateTime(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "未恢复".equals(trimmed) || trimmed.contains("未恢复")) {
            return null;
        }
        if (DIGITS.matcher(trimmed).matches()) {
            try {
                long value = Long.parseLong(trimmed);
                // 13+ 位按毫秒，否则按秒
                if (trimmed.length() >= 13) {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZONE);
                }
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZONE);
            } catch (Exception e) {
                return null;
            }
        }
        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.S",
                "yyyy-MM-dd HH:mm:ss.SS",
                "yyyy-MM-dd HH:mm:ss.SSS"
        };
        for (String p : patterns) {
            try {
                return LocalDateTime.parse(trimmed.length() > 19 && !trimmed.contains("T")
                        ? trimmed.substring(0, 19) : trimmed, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        try {
            if (trimmed.length() >= 19) {
                return LocalDateTime.parse(trimmed.substring(0, 19), OUT);
            }
        } catch (DateTimeParseException ignored) {
            // fallthrough
        }
        return null;
    }
}
