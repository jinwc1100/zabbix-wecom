package com.sla.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.CountedSlaDowntime;
import com.sla.notifier.util.ReviewStatusUtil;
import com.sla.notifier.util.TimeFormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CountedSlaDowntimeService {

    private static final Logger log = LoggerFactory.getLogger(CountedSlaDowntimeService.class);

    private static final String COUNTED_WHERE =
            "t.`排除状态` IN ('未排除', '-')"
                    + " OR (t.`排除状态` = '部分排除' AND t.`排除故障秒数` = 1)";

    private final GrafanaClient grafanaClient;
    private final GrafanaProperties grafanaProperties;

    public CountedSlaDowntimeService(GrafanaClient grafanaClient,
                                     GrafanaProperties grafanaProperties) {
        this.grafanaClient = grafanaClient;
        this.grafanaProperties = grafanaProperties;
    }

    public List<CountedSlaDowntime> collect(GrafanaClient.Session session) {
        Map<String, CountedSlaDowntime> dedupe =
                new LinkedHashMap<String, CountedSlaDowntime>();
        String from = reportYearStartMillis();
        String to = "now";
        collectSource(session, grafanaProperties.getDowntimeLeaf(), dedupe, from, to);
        collectSource(session, grafanaProperties.getDowntimeParent(), dedupe, from, to);
        return new ArrayList<CountedSlaDowntime>(dedupe.values());
    }

    private void collectSource(GrafanaClient.Session session,
                               GrafanaProperties.DowntimeSourceConfig source,
                               Map<String, CountedSlaDowntime> dedupe,
                               String from, String to) {
        if (source == null || source.getDashboardUid() == null
                || source.getDashboardUid().trim().isEmpty()) {
            return;
        }
        JsonNode dashboard = grafanaClient.fetchDashboard(session, source.getDashboardUid());
        for (GrafanaProperties.PanelConfig panel : source.getPanels()) {
            GrafanaClient.PanelQueryMeta meta =
                    grafanaClient.resolvePanelMeta(dashboard, panel.getId());
            String sql = "SELECT * FROM (\n" + stripTrailingSemicolon(meta.getRawSql())
                    + "\n) AS t WHERE " + COUNTED_WHERE;
            List<Map<String, String>> rows =
                    grafanaClient.queryTable(session, meta, sql, from, to);
            log.info("计入 SLA 宕机看板 [{}] 面板 [{}] 命中 {} 条",
                    source.getDashboardUid(), panel.getName(), rows.size());
            for (Map<String, String> row : rows) {
                CountedSlaDowntime item = mapRow(row, source);
                if (item != null) {
                    dedupe.put(item.dedupeKey(), item);
                }
            }
        }
    }

    private CountedSlaDowntime mapRow(Map<String, String> row,
                                      GrafanaProperties.DowntimeSourceConfig source) {
        String name = trim(row.get(source.getNameColumn()));
        if (name == null) {
            return null;
        }
        CountedSlaDowntime item = new CountedSlaDowntime();
        item.setSystemLevel(source.getSystemLevel());
        item.setSystemName(name);
        item.setDownStart(TimeFormatUtil.formatDisplay(row.get(source.getStartColumn())));
        item.setDownEnd(TimeFormatUtil.formatDisplay(row.get(source.getEndColumn())));

        String exclusionCol = source.getExclusionColumn();
        if (exclusionCol == null || exclusionCol.trim().isEmpty()) {
            exclusionCol = "排除状态";
        }
        String reasonColumn = source.getReasonColumn();
        if (reasonColumn == null || reasonColumn.trim().isEmpty()) {
            reasonColumn = "排除原因";
        }
        String status = trim(row.get(exclusionCol));
        String reason = trim(row.get(reasonColumn));
        if (ReviewStatusUtil.isUnreviewed(status, reason)) {
            item.setReviewDesc(null);
        } else {
            item.setReviewDesc(reason);
        }
        return item;
    }

    private static String reportYearStartMillis() {
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .withMonth(1).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        return String.valueOf(start.toInstant().toEpochMilli());
    }

    private static String trim(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    static String stripTrailingSemicolon(String rawSql) {
        if (rawSql == null) {
            return "";
        }
        String sql = rawSql.trim();
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }
}
