package com.sla.notifier.service;

import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.model.MonitorEditionBackfillResult;
import com.sla.notifier.model.ReviewEventSyncResult;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.WeeklyReviewRow;
import com.sla.notifier.util.ReviewEventSyncMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReviewEventSyncService {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventSyncService.class);
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WeeklyReviewDataService dataService;
    private final SlaReviewEventMapper reviewEventMapper;
    private final ExcludedDowntimeWriteService excludedDowntimeWriteService;
    private final MonitorEditionBackfillService monitorEditionBackfillService;
    private final ReviewSyncProperties reviewSyncProperties;
    private final WeeklyReviewProperties weeklyReviewProperties;
    private final LowSliPreWindowBackfillService preWindowBackfillService;

    public ReviewEventSyncService(WeeklyReviewDataService dataService,
                                  SlaReviewEventMapper reviewEventMapper,
                                  ExcludedDowntimeWriteService excludedDowntimeWriteService,
                                  MonitorEditionBackfillService monitorEditionBackfillService,
                                  ReviewSyncProperties reviewSyncProperties,
                                  WeeklyReviewProperties weeklyReviewProperties,
                                  LowSliPreWindowBackfillService preWindowBackfillService) {
        this.dataService = dataService;
        this.reviewEventMapper = reviewEventMapper;
        this.excludedDowntimeWriteService = excludedDowntimeWriteService;
        this.monitorEditionBackfillService = monitorEditionBackfillService;
        this.reviewSyncProperties = reviewSyncProperties;
        this.weeklyReviewProperties = weeklyReviewProperties;
        this.preWindowBackfillService = preWindowBackfillService;
    }

    public ReviewEventSyncResult runNow() {
        return runNow(false, false);
    }

    public ReviewEventSyncResult runNow(boolean refreshOwner) {
        return runNow(refreshOwner, false);
    }

    public ReviewEventSyncResult runNow(boolean refreshOwner, boolean refreshExcludeTime) {
        return runNow(refreshOwner, refreshExcludeTime, false);
    }

    public ReviewEventSyncResult runNow(boolean refreshOwner, boolean refreshExcludeTime,
                                        boolean refreshRouting) {
        return run(LocalDateTime.now(resolveZone()), refreshOwner, refreshExcludeTime, refreshRouting);
    }

    public ReviewEventSyncResult run(LocalDateTime triggerTime) {
        return run(triggerTime, false, false);
    }

    public ReviewEventSyncResult run(LocalDateTime triggerTime, boolean refreshOwner) {
        return run(triggerTime, refreshOwner, false);
    }

    public ReviewEventSyncResult run(LocalDateTime triggerTime, boolean refreshOwner,
                                     boolean refreshExcludeTime) {
        return run(triggerTime, refreshOwner, refreshExcludeTime, false);
    }

    public ReviewEventSyncResult run(LocalDateTime triggerTime, boolean refreshOwner,
                                     boolean refreshExcludeTime, boolean refreshRouting) {
        ReviewEventSyncResult result = new ReviewEventSyncResult();
        result.setTriggerTime(DISPLAY.format(triggerTime));
        if (refreshRouting) {
            applyBackfillResult(result, monitorEditionBackfillService.backfill());
        }
        if (refreshExcludeTime) {
            result.setExcludeTimeUpdated(reviewEventMapper.refreshExcludeTime());
        }
        if (refreshOwner) {
            applyGrafanaPass(triggerTime, true, result);
        } else if (!refreshExcludeTime && !refreshRouting) {
            applyGrafanaPass(triggerTime, false, result);
        } else if (refreshExcludeTime) {
            result.setCollected(0);
            result.setMessage("刷新 sla_review_event exclude_time：更新 "
                    + result.getExcludeTimeUpdated() + " 行" + routingMessageSuffix(result, refreshRouting));
            log.info("{}", result.getMessage());
            return result;
        } else {
            result.setCollected(0);
            result.setMessage(routingOnlyMessage(result));
            log.info("{}", result.getMessage());
            return result;
        }
        if (refreshOwner) {
            String msg = "刷新 sla_review_event 责任人：采集 " + result.getCollected()
                    + "，更新 " + result.getOwnerUpdated()
                    + "，库中无此行跳过 " + result.getSkippedNotFound()
                    + "，开始时间无效 " + result.getSkippedBadStart();
            if (refreshExcludeTime) {
                msg += "；exclude_time 更新 " + result.getExcludeTimeUpdated() + " 行";
            }
            result.setMessage(msg + routingMessageSuffix(result, refreshRouting));
        } else {
            result.setMessage("同步 sla_review_event：730前低SLI 采集 " + result.getPreWindowCollected()
                    + "，新增 " + result.getPreWindowInserted()
                    + "，已存在跳过 " + result.getPreWindowSkippedExisting()
                    + "，开始时间无效 " + result.getPreWindowSkippedBadStart()
                    + "；窗口内采集 " + result.getCollected()
                    + "，新增 " + result.getInserted()
                    + "，已存在跳过 " + result.getSkippedExisting()
                    + "，更新结束 " + result.getEndUpdated()
                    + "，折叠 " + result.getFolded()
                    + "，排除 period_to " + result.getExclusionPeriodToUpdated()
                    + "，开始时间无效 " + result.getSkippedBadStart());
        }
        log.info("{}", result.getMessage());
        return result;
    }

    private static void applyBackfillResult(ReviewEventSyncResult result,
                                            MonitorEditionBackfillResult backfill) {
        result.setRoutingRowsUpdated(backfill.getRowsUpdated());
        result.setRoutingCollisionCount(backfill.getCollisionCount());
        result.setRoutingGrafanaDiscoveryOk(backfill.isGrafanaDiscoveryOk());
    }

    private static String routingOnlyMessage(ReviewEventSyncResult result) {
        return "刷新 sla_system_owner 监控路由：更新 " + result.getRoutingRowsUpdated()
                + " 行，冲突 " + result.getRoutingCollisionCount() + " 个"
                + grafanaDiscoveryNote(result);
    }

    private static String routingMessageSuffix(ReviewEventSyncResult result, boolean refreshRouting) {
        if (!refreshRouting) {
            return "";
        }
        return "；监控路由更新 " + result.getRoutingRowsUpdated() + " 行"
                + grafanaDiscoveryNote(result);
    }

    private static String grafanaDiscoveryNote(ReviewEventSyncResult result) {
        if (result.isRoutingGrafanaDiscoveryOk()) {
            return "";
        }
        return "（Grafana 发现失败，未匹配行回退老版/zabbix6）";
    }

    private void applyGrafanaPass(LocalDateTime triggerTime, boolean refreshOwner,
                                  ReviewEventSyncResult result) {
        if (!refreshOwner) {
            applyPreWindowPass(result);
        }
        List<WeeklyReviewRow> rows = dataService.collect(triggerTime, true);
        int inserted = 0;
        int skippedExisting = 0;
        int skippedBadStart = 0;
        int ownerUpdated = 0;
        int skippedNotFound = 0;
        int endUpdated = 0;
        int folded = 0;
        int exclusionPeriodToUpdated = 0;
        for (WeeklyReviewRow row : rows) {
            SlaReviewEvent event = ReviewEventSyncMapper.fromRow(row, resolveZone());
            if (event == null) {
                skippedBadStart++;
                log.warn("跳过无法解析开始时间的宕机：level={} name={} start={}",
                        row.getSystemLevel(), row.getSystemName(), row.getDowntimeStart());
                continue;
            }
            if (refreshOwner) {
                int n = reviewEventMapper.updateOwnerByIdentity(event);
                if (n > 0) {
                    ownerUpdated++;
                } else {
                    skippedNotFound++;
                }
            } else {
                boolean allowInsert = WeeklyReviewDataService.allowsInsert(row.getExclusionStatus());
                if (isSentinelEnd(event.getDownEnd())) {
                    if (!allowInsert) {
                        continue;
                    }
                    int n = reviewEventMapper.insertIgnore(event);
                    if (n > 0) {
                        inserted++;
                    } else {
                        skippedExisting++;
                    }
                } else {
                    AlignCounts counts = alignEnd(event, allowInsert);
                    inserted += counts.inserted;
                    skippedExisting += counts.skippedExisting;
                    endUpdated += counts.endUpdated;
                    folded += counts.folded;
                    exclusionPeriodToUpdated += counts.exclusionPeriodToUpdated;
                }
            }
        }
        result.setCollected(rows.size());
        result.setInserted(inserted);
        result.setSkippedExisting(skippedExisting);
        result.setSkippedBadStart(skippedBadStart);
        result.setOwnerUpdated(ownerUpdated);
        result.setSkippedNotFound(skippedNotFound);
        result.setEndUpdated(endUpdated);
        result.setPromoted(endUpdated);
        result.setFolded(folded);
        result.setExclusionPeriodToUpdated(exclusionPeriodToUpdated);
    }

    /** Pass B: low-SLI pre-window insert-only (no end overwrite). */
    private void applyPreWindowPass(ReviewEventSyncResult result) {
        List<WeeklyReviewRow> rows = preWindowBackfillService.collectPreWindowRows();
        int inserted = 0;
        int skippedExisting = 0;
        int skippedBadStart = 0;
        ZoneId zone = resolveZone();
        for (WeeklyReviewRow row : rows) {
            SlaReviewEvent event = ReviewEventSyncMapper.fromRow(row, zone);
            if (event == null) {
                skippedBadStart++;
                log.warn("730前低SLI跳过无法解析开始时间：level={} name={} start={}",
                        row.getSystemLevel(), row.getSystemName(), row.getDowntimeStart());
                continue;
            }
            int n = reviewEventMapper.insertIgnore(event);
            if (n > 0) {
                inserted++;
            } else {
                skippedExisting++;
            }
        }
        result.setPreWindowCollected(rows.size());
        result.setPreWindowInserted(inserted);
        result.setPreWindowSkippedExisting(skippedExisting);
        result.setPreWindowSkippedBadStart(skippedBadStart);
    }

    /**
     * Align existing start-identity rows to the collected parseable end.
     * Never INSERT a second end for an existing start.
     */
    private AlignCounts alignEnd(SlaReviewEvent collected, boolean allowInsert) {
        AlignCounts counts = new AlignCounts();
        List<SlaReviewEvent> existing = reviewEventMapper.listByStartIdentity(collected);
        if (existing == null || existing.isEmpty()) {
            if (!allowInsert) {
                return counts;
            }
            int n = reviewEventMapper.insertIgnore(collected);
            if (n > 0) {
                counts.inserted = 1;
            } else {
                counts.skippedExisting = 1;
            }
            return counts;
        }

        SlaReviewEvent keep = pickKeep(existing, collected.getDownEnd());
        boolean alreadyMatched = collected.getDownEnd().equals(keep.getDownEnd());
        if (!alreadyMatched) {
            reviewEventMapper.updateAlignedDownEnd(keep.getId(), collected.getDownEnd());
            keep.setDownEnd(collected.getDownEnd());
            if (isNoSla(keep.getCountSla())) {
                keep.setExcludeTime(collected.getDownEnd());
            }
            counts.endUpdated = 1;
        }

        boolean copied = false;
        for (SlaReviewEvent row : existing) {
            if (row.getId().equals(keep.getId())) {
                continue;
            }
            copyBlankHumanFields(keep, row);
            copied = true;
            reviewEventMapper.deleteById(row.getId());
            counts.folded++;
        }
        if (copied) {
            reviewEventMapper.updateReviewFields(keep);
        }

        if (counts.endUpdated == 0 && counts.folded == 0) {
            counts.skippedExisting = 1;
        }

        if (counts.endUpdated > 0 && isNoSla(keep.getCountSla()) && keep.getExcludeTime() != null) {
            counts.exclusionPeriodToUpdated =
                    excludedDowntimeWriteService.updatePeriodToAfterSync(keep);
        }
        return counts;
    }

    static SlaReviewEvent pickKeep(List<SlaReviewEvent> existing, LocalDateTime collectedEnd) {
        SlaReviewEvent matching = null;
        SlaReviewEvent preferred = null;
        for (SlaReviewEvent row : existing) {
            if (collectedEnd != null && collectedEnd.equals(row.getDownEnd())) {
                matching = row;
            }
            if (preferred == null) {
                preferred = row;
                continue;
            }
            boolean rowReviewed = !isBlank(row.getCountSla());
            boolean prefReviewed = !isBlank(preferred.getCountSla());
            if (rowReviewed && !prefReviewed) {
                preferred = row;
            } else if (rowReviewed == prefReviewed
                    && row.getId() != null && preferred.getId() != null
                    && row.getId().longValue() < preferred.getId().longValue()) {
                preferred = row;
            }
        }
        return matching != null ? matching : preferred;
    }

    private static void copyBlankHumanFields(SlaReviewEvent dest, SlaReviewEvent src) {
        if (isBlank(dest.getFaultCategory())) {
            dest.setFaultCategory(src.getFaultCategory());
        }
        if (isBlank(dest.getFaultSubcategory())) {
            dest.setFaultSubcategory(src.getFaultSubcategory());
        }
        if (isBlank(dest.getFaultDesc())) {
            dest.setFaultDesc(src.getFaultDesc());
        }
        if (isBlank(dest.getCountSla())) {
            dest.setCountSla(src.getCountSla());
        }
        if (isBlank(dest.getResolutionProcess())) {
            dest.setResolutionProcess(src.getResolutionProcess());
        }
        if (isBlank(dest.getRootCause())) {
            dest.setRootCause(src.getRootCause());
        }
        if (isBlank(dest.getImprovement())) {
            dest.setImprovement(src.getImprovement());
        }
    }

    private static boolean isSentinelEnd(LocalDateTime end) {
        return end == null || ReviewEventSyncMapper.UNRECOVERED_END.equals(end);
    }

    private static boolean isNoSla(String countSla) {
        return countSla != null && "否".equals(countSla.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ZoneId resolveZone() {
        String zone = reviewSyncProperties.getZone();
        if (zone == null || zone.trim().isEmpty()) {
            zone = weeklyReviewProperties.getZone();
        }
        return ZoneId.of(zone == null || zone.trim().isEmpty()
                ? "Asia/Shanghai" : zone.trim());
    }

    private static final class AlignCounts {
        private int inserted;
        private int skippedExisting;
        private int endUpdated;
        private int folded;
        private int exclusionPeriodToUpdated;
    }
}
