package com.sla.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.MonitorEditionBackfillResult;
import com.sla.notifier.model.SlaSystemOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MonitorEditionBackfillService {

    static final String OLD_EDITION = "老版监控";
    static final String NEW_EDITION = "新版监控";
    static final String ZABBIX6 = "zabbix6";
    static final String ZABBIX7 = "zabbix7";

    private static final Logger log = LoggerFactory.getLogger(MonitorEditionBackfillService.class);

    private final SlaSystemOwnerMapper ownerMapper;
    private final GrafanaClient grafanaClient;
    private final GrafanaProperties grafanaProperties;

    public MonitorEditionBackfillService(SlaSystemOwnerMapper ownerMapper,
                                         GrafanaClient grafanaClient,
                                         GrafanaProperties grafanaProperties) {
        this.ownerMapper = ownerMapper;
        this.grafanaClient = grafanaClient;
        this.grafanaProperties = grafanaProperties;
    }

    public MonitorEditionBackfillResult backfill() {
        Map<String, RoutingValue> discovered = new HashMap<String, RoutingValue>();
        Set<String> collisions = new HashSet<String>();
        boolean grafanaAvailable = discover(discovered, collisions);

        List<SlaSystemOwner> owners = ownerMapper.selectAll();
        if (owners == null) {
            owners = Collections.emptyList();
        }
        int updated = 0;
        for (SlaSystemOwner owner : owners) {
            String key = key(owner.getSystemLevel(), owner.getSystemName());
            if (collisions.contains(key)) {
                log.error("系统同时出现在新老监控面板，跳过路由回填: level={}, name={}",
                        owner.getSystemLevel(), owner.getSystemName());
                continue;
            }
            RoutingValue routing = grafanaAvailable ? discovered.get(key) : null;
            if (routing == null) {
                routing = RoutingValue.OLD;
            }
            if (!routing.edition.equals(owner.getMonitorEdition())
                    || !routing.jdbcKey.equals(owner.getWriteJdbcKey())) {
                updated += ownerMapper.updateMonitorRouting(
                        owner.getSystemLevel(), owner.getSystemName(), routing.edition, routing.jdbcKey);
            }
        }
        log.info("sla_system_owner 监控版本回填完成：总行数={}，更新={}，冲突={}",
                Integer.valueOf(owners.size()), Integer.valueOf(updated), Integer.valueOf(collisions.size()));
        MonitorEditionBackfillResult result = new MonitorEditionBackfillResult();
        result.setTotalRows(owners.size());
        result.setRowsUpdated(updated);
        result.setCollisionCount(collisions.size());
        result.setGrafanaDiscoveryOk(grafanaAvailable);
        return result;
    }

    private boolean discover(Map<String, RoutingValue> discovered, Set<String> collisions) {
        try {
            GrafanaClient.Session session = grafanaClient.login();
            for (GrafanaProperties.SliSourceConfig source : grafanaProperties.resolveSliSources()) {
                if (source == null || isBlank(source.getDashboardUid())) {
                    continue;
                }
                JsonNode dashboard = grafanaClient.fetchDashboard(session, source.getDashboardUid());
                List<GrafanaProperties.PanelConfig> panels = source.getPanels();
                if (panels == null) {
                    continue;
                }
                for (GrafanaProperties.PanelConfig panel : panels) {
                    int panelId = panel == null ? -1 : panel.getId();
                    RoutingValue routing = routingForPanel(panelId);
                    if (routing == null) {
                        continue;
                    }
                    GrafanaClient.PanelQueryMeta meta =
                            grafanaClient.resolvePanelMeta(dashboard, panelId);
                    List<Map<String, String>> rows = grafanaClient.queryAllRows(session, meta);
                    collectRows(source, rows, routing, discovered, collisions);
                }
            }
            return true;
        } catch (Exception e) {
            discovered.clear();
            collisions.clear();
            log.warn("Grafana 监控版本发现失败，字典行回退为老版监控/zabbix6: {}", e.getMessage());
            return false;
        }
    }

    static void collectRows(GrafanaProperties.SliSourceConfig source,
                            List<Map<String, String>> rows,
                            RoutingValue routing,
                            Map<String, RoutingValue> discovered,
                            Set<String> collisions) {
        if (rows == null) {
            return;
        }
        for (Map<String, String> row : rows) {
            String name = firstNonBlank(row, source.getNameColumns());
            if (isBlank(name)) {
                continue;
            }
            String key = key(source.getSystemLevel(), name);
            RoutingValue previous = discovered.get(key);
            if (previous != null && previous != routing) {
                collisions.add(key);
                discovered.remove(key);
            } else if (!collisions.contains(key)) {
                discovered.put(key, routing);
            }
        }
    }

    private static String firstNonBlank(Map<String, String> row, List<String> columns) {
        if (row == null || columns == null) {
            return null;
        }
        for (String column : columns) {
            String value = row.get(column);
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static RoutingValue routingForPanel(int panelId) {
        if (panelId == 4) {
            return RoutingValue.OLD;
        }
        if (panelId == 2) {
            return RoutingValue.NEW;
        }
        return null;
    }

    private static String key(String level, String name) {
        return trim(level) + '\0' + trim(name);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class RoutingValue {
        static final RoutingValue OLD = new RoutingValue(OLD_EDITION, ZABBIX6);
        static final RoutingValue NEW = new RoutingValue(NEW_EDITION, ZABBIX7);

        private final String edition;
        private final String jdbcKey;

        private RoutingValue(String edition, String jdbcKey) {
            this.edition = edition;
            this.jdbcKey = jdbcKey;
        }
    }
}
