package com.sla.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.util.TimeFormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DowntimeQueryService {

    private static final Logger log = LoggerFactory.getLogger(DowntimeQueryService.class);

    private final GrafanaClient grafanaClient;
    private final GrafanaProperties grafanaProperties;

    public DowntimeQueryService(GrafanaClient grafanaClient, GrafanaProperties grafanaProperties) {
        this.grafanaClient = grafanaClient;
        this.grafanaProperties = grafanaProperties;
    }

    public Map<String, List<DowntimeEvent>> loadLeafIndex(GrafanaClient.Session session) {
        return loadIndex(session, grafanaProperties.getDowntimeLeaf(), null, null);
    }

    public Map<String, List<DowntimeEvent>> loadLeafIndex(GrafanaClient.Session session,
                                                         String from, String to) {
        return loadIndex(session, grafanaProperties.getDowntimeLeaf(), from, to);
    }

    public Map<String, List<DowntimeEvent>> loadParentIndex(GrafanaClient.Session session) {
        return loadIndex(session, grafanaProperties.getDowntimeParent(), null, null);
    }

    public Map<String, List<DowntimeEvent>> loadParentIndex(GrafanaClient.Session session,
                                                           String from, String to) {
        return loadIndex(session, grafanaProperties.getDowntimeParent(), from, to);
    }

    private Map<String, List<DowntimeEvent>> loadIndex(GrafanaClient.Session session,
                                                      GrafanaProperties.DowntimeSourceConfig source,
                                                      String from, String to) {
        Map<String, List<DowntimeEvent>> index = new LinkedHashMap<String, List<DowntimeEvent>>();
        if (source == null || source.getDashboardUid() == null) {
            return index;
        }
        JsonNode dashboard = grafanaClient.fetchDashboard(session, source.getDashboardUid());
        Map<String, DowntimeEvent> dedupe = new LinkedHashMap<String, DowntimeEvent>();
        boolean timed = from != null && to != null;

        for (GrafanaProperties.PanelConfig panel : source.getPanels()) {
            GrafanaClient.PanelQueryMeta meta = grafanaClient.resolvePanelMeta(dashboard, panel.getId());
            List<Map<String, String>> rows = timed
                    ? grafanaClient.queryTable(session, meta, meta.getRawSql(), from, to)
                    : grafanaClient.queryAllRows(session, meta);
            log.info("宕机看板 [{}] 面板 [{}] 原始行数={}", source.getDashboardUid(), panel.getName(), rows.size());

            for (Map<String, String> row : rows) {
                String name = row.get(source.getNameColumn());
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                name = name.trim();
                String startRaw = row.get(source.getStartColumn());
                String endRaw = row.get(source.getEndColumn());
                DowntimeEvent event = new DowntimeEvent();
                event.setSystemName(name);
                event.setSystemLevel(source.getSystemLevel());
                event.setStartRaw(startRaw);
                event.setEndRaw(endRaw);
                event.setStartDisplay(TimeFormatUtil.formatDisplay(startRaw));
                event.setEndDisplay(TimeFormatUtil.formatDisplay(endRaw));
                event.setStartTime(TimeFormatUtil.parseToLocalDateTime(startRaw));
                String exclusionCol = source.getExclusionColumn();
                if (exclusionCol == null || exclusionCol.trim().isEmpty()) {
                    exclusionCol = "排除状态";
                }
                String excl = row.get(exclusionCol);
                event.setExclusionStatus(excl == null ? null : excl.trim());
                String reasonCol = source.getReasonColumn();
                if (reasonCol == null || reasonCol.trim().isEmpty()) {
                    reasonCol = "排除原因";
                }
                String reason = row.get(reasonCol);
                event.setExclusionReason(reason == null ? null : reason.trim());
                dedupe.put(event.dedupeKey(), event);
            }
        }

        for (DowntimeEvent event : dedupe.values()) {
            List<DowntimeEvent> list = index.get(event.getSystemName());
            if (list == null) {
                list = new ArrayList<DowntimeEvent>();
                index.put(event.getSystemName(), list);
            }
            list.add(event);
        }
        log.info("宕机索引 [{}] 系统数={} 去重后事件数={}",
                source.getSystemLevel(), index.size(), dedupe.size());
        return index;
    }
}
