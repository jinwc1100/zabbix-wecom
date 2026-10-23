package com.sla.notifier.schedule;

import com.sla.notifier.config.SliSnapshotSyncProperties;
import com.sla.notifier.service.SliSnapshotSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SliSnapshotSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SliSnapshotSyncScheduler.class);

    private final SliSnapshotSyncProperties properties;
    private final SliSnapshotSyncService syncService;

    public SliSnapshotSyncScheduler(SliSnapshotSyncProperties properties,
                                    SliSnapshotSyncService syncService) {
        this.properties = properties;
        this.syncService = syncService;
    }

    @Scheduled(cron = "${sli-snapshot-sync.cron:0 0 17 * * *}",
            zone = "${sli-snapshot-sync.zone:Asia/Shanghai}")
    public void runDaily() {
        if (!properties.isEnabled()) {
            log.debug("sla_sli_snapshot 日同步未启用，跳过");
            return;
        }
        try {
            syncService.runNow();
        } catch (Exception e) {
            log.error("sla_sli_snapshot 日同步失败", e);
        }
    }
}
