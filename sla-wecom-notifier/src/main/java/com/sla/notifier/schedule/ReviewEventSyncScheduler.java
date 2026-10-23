package com.sla.notifier.schedule;

import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.service.ReviewEventSyncService;
import com.sla.notifier.service.ReviewIncompleteNudgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class ReviewEventSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventSyncScheduler.class);

    private final ReviewSyncProperties properties;
    private final ReviewEventSyncService syncService;
    private final ReviewIncompleteNudgeService nudgeService;

    public ReviewEventSyncScheduler(ReviewSyncProperties properties,
                                    ReviewEventSyncService syncService,
                                    ReviewIncompleteNudgeService nudgeService) {
        this.properties = properties;
        this.syncService = syncService;
        this.nudgeService = nudgeService;
    }

    @Scheduled(cron = "${review-sync.cron:0 0 * * * *}",
            zone = "${review-sync.zone:Asia/Shanghai}")
    public void runDaily() {
        runAt(LocalDateTime.now(resolveZone()));
    }

    void runAt(LocalDateTime start) {
        if (!properties.isEnabled()) {
            log.debug("sla_review_event 定时同步未启用，跳过");
            return;
        }
        try {
            syncService.runNow();
        } catch (Exception e) {
            log.error("sla_review_event 定时同步失败", e);
            return;
        }
        if (start == null || start.getHour() != 17) {
            return;
        }
        try {
            nudgeService.nudgeAfterSync();
        } catch (Exception e) {
            log.error("sla_review_event 未复盘催办失败", e);
        }
    }

    private ZoneId resolveZone() {
        String zone = properties.getZone();
        return ZoneId.of(zone == null || zone.trim().isEmpty() ? "Asia/Shanghai" : zone.trim());
    }
}
