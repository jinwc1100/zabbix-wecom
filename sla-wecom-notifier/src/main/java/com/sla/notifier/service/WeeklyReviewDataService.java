package com.sla.notifier.service;

import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.model.SlaSystemOwner;
import com.sla.notifier.model.WeeklyReviewRow;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class WeeklyReviewDataService {

    private static final DateTimeFormatter WINDOW_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GrafanaClient grafanaClient;
    private final DowntimeQueryService downtimeQueryService;
    private final OwnerRuleResolver ownerRuleResolver;
    private final WeeklyReviewProperties properties;

    public WeeklyReviewDataService(GrafanaClient grafanaClient,
                                   DowntimeQueryService downtimeQueryService,
                                   OwnerRuleResolver ownerRuleResolver,
                                   WeeklyReviewProperties properties) {
        this.grafanaClient = grafanaClient;
        this.downtimeQueryService = downtimeQueryService;
        this.ownerRuleResolver = ownerRuleResolver;
        this.properties = properties;
    }

    public List<WeeklyReviewRow> collect(LocalDateTime triggerTime) {
        return collect(triggerTime, false);
    }

    /**
     * @param includeExcluded when true, in-window rows of any exclusion status are collected
     *                       (review-event sync). Weekly review Excel still uses false.
     */
    public List<WeeklyReviewRow> collect(LocalDateTime triggerTime, boolean includeExcluded) {
        if (triggerTime == null) {
            throw new IllegalArgumentException("triggerTime 不能为空");
        }
        LocalDateTime windowStart = parseWindowStart(properties.getWindowStart());
        ZoneId zone = resolveZone();
        String from = String.valueOf(windowStart.atZone(zone).toInstant().toEpochMilli());
        String to = String.valueOf(triggerTime.atZone(zone).toInstant().toEpochMilli());
        GrafanaClient.Session session = grafanaClient.login();
        Map<String, SlaSystemOwner> owners = ownerRuleResolver.loadSnapshot();
        List<WeeklyReviewRow> rows = new ArrayList<WeeklyReviewRow>();
        appendEligible(rows, downtimeQueryService.loadLeafIndex(session, from, to),
                windowStart, triggerTime, owners, includeExcluded);
        appendEligible(rows, downtimeQueryService.loadParentIndex(session, from, to),
                windowStart, triggerTime, owners, includeExcluded);
        Collections.sort(rows, new Comparator<WeeklyReviewRow>() {
            @Override
            public int compare(WeeklyReviewRow left, WeeklyReviewRow right) {
                int byStart = safe(left.getDowntimeStart()).compareTo(safe(right.getDowntimeStart()));
                if (byStart != 0) {
                    return byStart;
                }
                int byLevel = safe(left.getSystemLevel()).compareTo(safe(right.getSystemLevel()));
                if (byLevel != 0) {
                    return byLevel;
                }
                return safe(left.getSystemName()).compareTo(safe(right.getSystemName()));
            }
        });
        return rows;
    }

    private void appendEligible(List<WeeklyReviewRow> output,
                                Map<String, List<DowntimeEvent>> index,
                                LocalDateTime windowStart,
                                LocalDateTime triggerTime,
                                Map<String, SlaSystemOwner> owners,
                                boolean includeExcluded) {
        for (List<DowntimeEvent> events : index.values()) {
            if (events == null) {
                continue;
            }
            for (DowntimeEvent event : events) {
                if (!isEligible(event, windowStart, triggerTime, includeExcluded)) {
                    continue;
                }
                OwnerRuleResolver.ResolvedOwner owner = ownerRuleResolver.resolve(
                        event.getSystemLevel(), event.getSystemName(), owners);
                output.add(new WeeklyReviewRow(
                        event.getSystemLevel(),
                        event.getSystemName(),
                        safe(event.getStartDisplay()),
                        safe(event.getEndDisplay()),
                        safe(owner.getName()),
                        owner.getMobile(),
                        owner.isMatched(),
                        event.getExclusionStatus()));
            }
        }
    }

    static boolean isEligible(DowntimeEvent event,
                              LocalDateTime windowStart,
                              LocalDateTime triggerTime) {
        return isEligible(event, windowStart, triggerTime, false);
    }

    static boolean isEligible(DowntimeEvent event,
                              LocalDateTime windowStart,
                              LocalDateTime triggerTime,
                              boolean includeExcluded) {
        if (event == null || event.getStartTime() == null
                || windowStart == null || triggerTime == null) {
            return false;
        }
        LocalDateTime start = event.getStartTime();
        boolean inWindow = !start.isBefore(windowStart) && !start.isAfter(triggerTime);
        if (!inWindow) {
            return false;
        }
        return includeExcluded || isUnexcluded(event.getExclusionStatus());
    }

    /**
     * Blank status (tests / Excel rows) may insert. Grafana excluded statuses may not.
     */
    static boolean allowsInsert(String exclusionStatus) {
        if (exclusionStatus == null || exclusionStatus.trim().isEmpty()) {
            return true;
        }
        return isUnexcluded(exclusionStatus);
    }

    static boolean isUnexcluded(String status) {
        if (status == null) {
            return false;
        }
        String value = status.trim();
        return "未排除".equals(value) || "-".equals(value);
    }

    static LocalDateTime parseWindowStart(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("weekly-review.window-start 不能为空");
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed, WINDOW_FORMAT);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException e) {
                throw new IllegalStateException(
                        "weekly-review.window-start 格式须为 yyyy-MM-dd HH:mm:ss", e);
            }
        }
    }

    private ZoneId resolveZone() {
        String zone = properties.getZone();
        return ZoneId.of(zone == null || zone.trim().isEmpty() ? "Asia/Shanghai" : zone.trim());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
