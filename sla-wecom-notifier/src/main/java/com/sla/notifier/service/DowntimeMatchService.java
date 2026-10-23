package com.sla.notifier.service;

import com.sla.notifier.config.SlaDowntimeProperties;
import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.model.DowntimeNotifyItem;
import com.sla.notifier.model.SlaLowSliItem;
import com.sla.notifier.util.ReviewStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DowntimeMatchService {

    private static final Logger log = LoggerFactory.getLogger(DowntimeMatchService.class);

    private final SlaDowntimeProperties downtimeProperties;

    public DowntimeMatchService(SlaDowntimeProperties downtimeProperties) {
        this.downtimeProperties = downtimeProperties;
    }

    public List<DowntimeNotifyItem> match(List<SlaLowSliItem> lowSliItems,
                                         Map<String, List<DowntimeEvent>> leafIndex,
                                         Map<String, List<DowntimeEvent>> parentIndex) {
        LocalDateTime windowStart = LocalDateTime.now(com.sla.notifier.util.TimeFormatUtil.ZONE)
                .minusDays(Math.max(1, downtimeProperties.getRecentWindowDays()));

        List<DowntimeNotifyItem> result = new ArrayList<DowntimeNotifyItem>();
        for (SlaLowSliItem low : lowSliItems) {
            String name = low.getSystemName();
            String level = resolveSourceLevel(low);

            List<DowntimeEvent> chosen = null;
            if ("父系统".equals(level)) {
                chosen = parentIndex.get(name);
            } else if ("子系统".equals(level)) {
                chosen = leafIndex.get(name);
            } else {
                // 无明确来源时回退旧逻辑（仅 legacy 回退可能用到）
                List<DowntimeEvent> parentHits = parentIndex.get(name);
                List<DowntimeEvent> leafHits = leafIndex.get(name);
                boolean hasParent = parentHits != null && !parentHits.isEmpty();
                boolean hasLeaf = leafHits != null && !leafHits.isEmpty();
                if (hasParent && hasLeaf) {
                    log.warn("系统 [{}] 无 sourceLevel 且同时命中父/子宕机，按优先级 {}",
                            name, downtimeProperties.getLevelPriority());
                    if (downtimeProperties.isParentFirst()) {
                        chosen = parentHits;
                        level = "父系统";
                    } else {
                        chosen = leafHits;
                        level = "子系统";
                    }
                } else if (hasParent) {
                    chosen = parentHits;
                    level = "父系统";
                } else if (hasLeaf) {
                    chosen = leafHits;
                    level = "子系统";
                } else {
                    level = "未知";
                }
            }

            if (chosen == null || chosen.isEmpty()) {
                if (!downtimeProperties.keepUnmatched()) {
                    continue;
                }
                DowntimeNotifyItem item = baseItem(low, level);
                item.setRemark(downtimeProperties.getRemarkNone());
                item.setMonthFullyReviewed(false);
                result.add(item);
                continue;
            }

            List<DowntimeEvent> recent = new ArrayList<DowntimeEvent>();
            List<DowntimeEvent> historical = new ArrayList<DowntimeEvent>();
            for (DowntimeEvent event : chosen) {
                if (event.getStartTime() == null) {
                    historical.add(event);
                    continue;
                }
                if (!event.getStartTime().isBefore(windowStart)) {
                    recent.add(event);
                } else {
                    historical.add(event);
                }
            }

            Collections.sort(recent, new Comparator<DowntimeEvent>() {
                @Override
                public int compare(DowntimeEvent a, DowntimeEvent b) {
                    if (a.getStartTime() == null && b.getStartTime() == null) {
                        return 0;
                    }
                    if (a.getStartTime() == null) {
                        return 1;
                    }
                    if (b.getStartTime() == null) {
                        return -1;
                    }
                    return a.getStartTime().compareTo(b.getStartTime());
                }
            });

            DowntimeNotifyItem item = baseItem(low, level);
            item.setHistoricalCount(historical.size());
            item.setRecentCount(recent.size());
            item.setMonthFullyReviewed(ReviewStatusUtil.isMonthFullyReviewed(chosen));
            for (DowntimeEvent event : recent) {
                item.getRecentPeriods().add(new DowntimeNotifyItem.DowntimePeriod(
                        event.getStartDisplay(), event.getEndDisplay()));
            }

            if (recent.size() > 0 && historical.size() > 0) {
                item.setRemark(downtimeProperties.formatRemark(
                        downtimeProperties.getRemarkWithRecent(), historical.size()));
            } else if (recent.size() == 0 && historical.size() > 0) {
                item.setRemark(downtimeProperties.formatRemark(
                        downtimeProperties.getRemarkOnlyHistorical(), historical.size()));
            } else if (recent.size() == 0 && historical.size() == 0) {
                item.setRemark(downtimeProperties.getRemarkNone());
            }

            result.add(item);
        }

        Collections.sort(result, new Comparator<DowntimeNotifyItem>() {
            @Override
            public int compare(DowntimeNotifyItem a, DowntimeNotifyItem b) {
                int levelCmp = levelOrder(a.getSystemLevel()) - levelOrder(b.getSystemLevel());
                if (levelCmp != 0) {
                    return levelCmp;
                }
                return Double.compare(a.getAnnualSliValue(), b.getAnnualSliValue());
            }
        });
        return result;
    }

    private String resolveSourceLevel(SlaLowSliItem low) {
        if (low.getSourceLevel() != null && !low.getSourceLevel().trim().isEmpty()) {
            return low.getSourceLevel().trim();
        }
        return null;
    }

    private DowntimeNotifyItem baseItem(SlaLowSliItem low, String level) {
        DowntimeNotifyItem item = new DowntimeNotifyItem();
        item.setSystemName(low.getSystemName());
        item.setAnnualSli(low.getAnnualSli());
        item.setAnnualSliValue(low.getAnnualSliValue());
        item.setSystemLevel(level);
        item.setMonitorSystem(low.getPanelName());
        return item;
    }

    private int levelOrder(String level) {
        if ("父系统".equals(level)) {
            return 0;
        }
        if ("子系统".equals(level)) {
            return 1;
        }
        return 2;
    }
}
