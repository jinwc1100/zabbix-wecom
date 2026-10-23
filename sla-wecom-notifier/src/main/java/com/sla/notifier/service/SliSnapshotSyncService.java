package com.sla.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.mapper.SlaSliSnapshotMapper;
import com.sla.notifier.model.SlaSliSnapshot;
import com.sla.notifier.model.SliSnapshotSyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SliSnapshotSyncService {

    private static final Logger log = LoggerFactory.getLogger(SliSnapshotSyncService.class);
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final List<String> MONTH_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月"));

    private final GrafanaClient grafanaClient;
    private final GrafanaProperties grafanaProperties;
    private final SlaSliSnapshotMapper snapshotMapper;

    public SliSnapshotSyncService(GrafanaClient grafanaClient,
                                  GrafanaProperties grafanaProperties,
                                  SlaSliSnapshotMapper snapshotMapper) {
        this.grafanaClient = grafanaClient;
        this.grafanaProperties = grafanaProperties;
        this.snapshotMapper = snapshotMapper;
    }

    public SliSnapshotSyncResult runNow() {
        SliSnapshotSyncResult result = new SliSnapshotSyncResult();
        ZonedDateTime now = ZonedDateTime.now(SHANGHAI);
        int year = now.getYear();
        LocalDateTime syncedAt = now.toLocalDateTime();
        GrafanaClient.Session session = grafanaClient.login();
        List<SlaSliSnapshot> rows = collect(session, year, syncedAt);
        for (SlaSliSnapshot row : rows) {
            snapshotMapper.upsert(row);
        }
        result.setSuccess(true);
        result.setUpserted(rows.size());
        log.info("sla_sli_snapshot 同步完成 year={} upserted={}", year, rows.size());
        return result;
    }

    List<SlaSliSnapshot> collect(GrafanaClient.Session session, int reportYear,
                                 LocalDateTime syncedAt) {
        List<SlaSliSnapshot> out = new ArrayList<SlaSliSnapshot>();
        for (GrafanaProperties.SliSourceConfig source : grafanaProperties.resolveMonthlySliSources()) {
            if (source.getDashboardUid() == null || source.getDashboardUid().trim().isEmpty()) {
                continue;
            }
            List<GrafanaProperties.PanelConfig> panels = source.getPanels();
            if (panels == null || panels.isEmpty()) {
                continue;
            }
            JsonNode dashboard = grafanaClient.fetchDashboard(session, source.getDashboardUid());
            for (GrafanaProperties.PanelConfig panel : panels) {
                GrafanaClient.PanelQueryMeta meta =
                        grafanaClient.resolvePanelMeta(dashboard, panel.getId());
                String panelName = panel.getName() != null ? panel.getName() : meta.getTitle();
                List<Map<String, String>> table = grafanaClient.queryAllRows(session, meta);
                for (Map<String, String> row : table) {
                    SlaSliSnapshot snap = mapRow(row, source, panel.getId(), panelName,
                            reportYear, syncedAt);
                    if (snap != null) {
                        out.add(snap);
                    }
                }
            }
        }
        return out;
    }

    private SlaSliSnapshot mapRow(Map<String, String> row,
                                  GrafanaProperties.SliSourceConfig source,
                                  int panelId, String panelName,
                                  int reportYear, LocalDateTime syncedAt) {
        String name = firstNonBlank(row, source.getNameColumns());
        if (name == null) {
            return null;
        }
        SlaSliSnapshot snap = new SlaSliSnapshot();
        snap.setReportYear(reportYear);
        snap.setSystemLevel(source.getSystemLevel());
        snap.setSystemName(name);
        snap.setPanelId(panelId);
        snap.setPanelName(panelName);
        snap.setSliAnnual(parseNumericSli(row.get("年度SLI")));
        snap.setSliM01(parseNumericSli(row.get(MONTH_COLUMNS.get(0))));
        snap.setSliM02(parseNumericSli(row.get(MONTH_COLUMNS.get(1))));
        snap.setSliM03(parseNumericSli(row.get(MONTH_COLUMNS.get(2))));
        snap.setSliM04(parseNumericSli(row.get(MONTH_COLUMNS.get(3))));
        snap.setSliM05(parseNumericSli(row.get(MONTH_COLUMNS.get(4))));
        snap.setSliM06(parseNumericSli(row.get(MONTH_COLUMNS.get(5))));
        snap.setSliM07(parseNumericSli(row.get(MONTH_COLUMNS.get(6))));
        snap.setSliM08(parseNumericSli(row.get(MONTH_COLUMNS.get(7))));
        snap.setSliM09(parseNumericSli(row.get(MONTH_COLUMNS.get(8))));
        snap.setSliM10(parseNumericSli(row.get(MONTH_COLUMNS.get(9))));
        snap.setSliM11(parseNumericSli(row.get(MONTH_COLUMNS.get(10))));
        snap.setSliM12(parseNumericSli(row.get(MONTH_COLUMNS.get(11))));
        snap.setSyncedAt(syncedAt);
        return snap;
    }

    private static String firstNonBlank(Map<String, String> row, List<String> keys) {
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String value = row.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    static Double parseNumericSli(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace("%", "").trim();
        if (cleaned.isEmpty() || "-".equals(cleaned) || "--".equals(cleaned)) {
            return null;
        }
        try {
            return Double.valueOf(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
