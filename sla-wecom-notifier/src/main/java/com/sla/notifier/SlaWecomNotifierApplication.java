package com.sla.notifier;

import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.config.SliSnapshotSyncProperties;
import com.sla.notifier.config.SlaDowntimeProperties;
import com.sla.notifier.config.TencentDocsProperties;
import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.config.WeComProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.sla.notifier.mapper")
@EnableConfigurationProperties({
        GrafanaProperties.class,
        WeComProperties.class,
        SlaDowntimeProperties.class,
        WeeklyReviewProperties.class,
        TencentDocsProperties.class,
        ReviewSyncProperties.class,
        SliSnapshotSyncProperties.class
})
public class SlaWecomNotifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlaWecomNotifierApplication.class, args);
    }
}
