package com.sla.notifier.service;

import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.model.SlaLowSliItem;
import com.sla.notifier.model.SlaSystemOwner;
import com.sla.notifier.model.WeeklyReviewRow;
import com.sla.notifier.service.ReviewLowSliDowntimeSupport.LowSliDowntimeRow;
import com.sla.notifier.util.ReviewEventSyncMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class LowSliPreWindowBackfillService {

    private final GrafanaClient grafanaClient;
    private final GrafanaProperties grafanaProperties;
    private final AnnualSliService annualSliService;
    private final DowntimeQueryService downtimeQueryService;
    private final OwnerRuleResolver ownerRuleResolver;
    private final WeeklyReviewProperties weeklyReviewProperties;

    public LowSliPreWindowBackfillService(GrafanaClient grafanaClient,
                                          GrafanaProperties grafanaProperties,
                                          AnnualSliService annualSliService,
                                          DowntimeQueryService downtimeQueryService,
                                          OwnerRuleResolver ownerRuleResolver,
                                          WeeklyReviewProperties weeklyReviewProperties) {
        this.grafanaClient = grafanaClient;
        this.grafanaProperties = grafanaProperties;
        this.annualSliService = annualSliService;
        this.downtimeQueryService = downtimeQueryService;
        this.ownerRuleResolver = ownerRuleResolver;
        this.weeklyReviewProperties = weeklyReviewProperties;
    }

    /**
     * Pre-window candidates for Pass B (same Grafana index as review Excel, start &lt; window-start).
     */
    public List<WeeklyReviewRow> collectPreWindowRows() {
        LocalDateTime windowStart = WeeklyReviewDataService.parseWindowStart(
                weeklyReviewProperties.getWindowStart());
        GrafanaClient.Session session = grafanaClient.login();
        List<SlaLowSliItem> inventory = annualSliService.collectAllInventoryItems(session);
        List<SlaLowSliItem> lowSli = ReviewLowSliDowntimeSupport.filterLowSli(
                inventory, grafanaProperties.getSliThreshold());
        Map<String, List<DowntimeEvent>> leafIndex = downtimeQueryService.loadLeafIndex(session);
        Map<String, List<DowntimeEvent>> parentIndex = downtimeQueryService.loadParentIndex(session);
        List<LowSliDowntimeRow> expanded = ReviewLowSliDowntimeSupport.expandUnexcludedDowntimes(
                lowSli, leafIndex, parentIndex);
        Map<String, SlaSystemOwner> owners = ownerRuleResolver.loadSnapshot();
        List<WeeklyReviewRow> rows = new ArrayList<WeeklyReviewRow>();
        for (LowSliDowntimeRow row : expanded) {
            WeeklyReviewRow weekly = toWeeklyReviewRow(row, owners);
            if (!isStrictlyBeforeWindow(weekly, windowStart)) {
                continue;
            }
            rows.add(weekly);
        }
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

    private static boolean isStrictlyBeforeWindow(WeeklyReviewRow row, LocalDateTime windowStart) {
        if (row == null || windowStart == null) {
            return false;
        }
        LocalDateTime start = ReviewEventSyncMapper.parseStart(row.getDowntimeStart());
        return start != null && start.isBefore(windowStart);
    }

    private WeeklyReviewRow toWeeklyReviewRow(LowSliDowntimeRow row, Map<String, SlaSystemOwner> owners) {
        OwnerRuleResolver.ResolvedOwner owner = ownerRuleResolver.resolve(
                row.getSystemLevel(), row.getSystemName(), owners);
        return new WeeklyReviewRow(
                row.getSystemLevel(),
                row.getSystemName(),
                row.getStartDisplay(),
                row.getEndDisplay(),
                safe(owner.getName()),
                owner.getMobile(),
                owner.isMatched(),
                row.getExclusionStatus());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
