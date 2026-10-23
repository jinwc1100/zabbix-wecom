package com.sla.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.SlaLowSliItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnnualSliService {

    private static final Logger log = LoggerFactory.getLogger(AnnualSliService.class);
    private static final List<String> MONTH_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月"));

    private final GrafanaClient grafanaClient;
    private final GrafanaProperties grafanaProperties;

    public AnnualSliService(GrafanaClient grafanaClient, GrafanaProperties grafanaProperties) {
        this.grafanaClient = grafanaClient;
        this.grafanaProperties = grafanaProperties;
    }

    public List<SlaLowSliItem> collectLowSliItems(GrafanaClient.Session session) {
        return collectAndSort(session, grafanaProperties.resolveSliSources());
    }

    public List<SlaLowSliItem> collectMonthlyLowSliItems(GrafanaClient.Session session) {
        int currentMonth = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).getMonthValue();
        return collectMonthlySliData(session).getLowItemsByMonth().get(currentMonth);
    }

    public MonthlySliCollection collectMonthlySliData(GrafanaClient.Session session) {
        MonthlySliCollection result = new MonthlySliCollection();
        double threshold = grafanaProperties.getSliThreshold();

        for (GrafanaProperties.SliSourceConfig source : grafanaProperties.resolveMonthlySliSources()) {
            if (source.getDashboardUid() == null || source.getDashboardUid().trim().isEmpty()) {
                log.warn("月度 SLI 源 [{}] 未配置 dashboard-uid，跳过", source.getName());
                continue;
            }
            JsonNode dashboard = grafanaClient.fetchDashboard(session, source.getDashboardUid());
            List<GrafanaProperties.PanelConfig> panels = source.getPanels();
            if (panels == null || panels.isEmpty()) {
                log.warn("月度 SLI 源 [{}] 未配置 panels，跳过", source.getName());
                continue;
            }
            for (GrafanaProperties.PanelConfig panel : panels) {
                GrafanaClient.PanelQueryMeta meta = grafanaClient.resolvePanelMeta(dashboard, panel.getId());
                String panelName = panel.getName() != null ? panel.getName() : meta.getTitle();
                List<Map<String, String>> rows = grafanaClient.queryAllRows(session, meta);
                for (Map<String, String> row : rows) {
                    String systemName = firstNonBlank(row, source.getNameColumns());
                    for (int i = 0; i < MONTH_COLUMNS.size(); i++) {
                        int month = i + 1;
                        Double value = parseNumericSli(row.get(MONTH_COLUMNS.get(i)));
                        if (value == null) {
                            continue;
                        }
                        result.incrementNumeric(month, source.getSystemLevel());
                        if (value < threshold) {
                            String raw = row.get(MONTH_COLUMNS.get(i));
                            SlaLowSliItem item = new SlaLowSliItem(
                                    panelName, panel.getId(), systemName, raw, value, null);
                            item.setSourceLevel(source.getSystemLevel());
                            item.setSourceName(source.getName());
                            result.getLowItemsByMonth().get(month).add(item);
                        }
                    }
                }
            }
        }

        for (List<SlaLowSliItem> items : result.getLowItemsByMonth().values()) {
            Collections.sort(items, new Comparator<SlaLowSliItem>() {
                @Override
                public int compare(SlaLowSliItem a, SlaLowSliItem b) {
                    return Double.compare(a.getAnnualSliValue(), b.getAnnualSliValue());
                }
            });
        }
        return result;
    }

    /**
     * 年度清单全量（含 SLI ≥ 阈值），带 sourceLevel；不过滤阈值。
     */
    public List<SlaLowSliItem> collectAllInventoryItems(GrafanaClient.Session session) {
        List<SlaLowSliItem> all = collectInventory(session, grafanaProperties.resolveSliSources(),
                false, 0D);
        Collections.sort(all, new Comparator<SlaLowSliItem>() {
            @Override
            public int compare(SlaLowSliItem a, SlaLowSliItem b) {
                return Double.compare(a.getAnnualSliValue(), b.getAnnualSliValue());
            }
        });
        log.info("双源合并后年度清单全量共 {} 条", all.size());
        return all;
    }

    private List<SlaLowSliItem> collectAndSort(GrafanaClient.Session session,
                                              List<GrafanaProperties.SliSourceConfig> sources) {
        double threshold = grafanaProperties.getSliThreshold();
        List<SlaLowSliItem> all = collectInventory(session, sources, true, threshold);
        Collections.sort(all, new Comparator<SlaLowSliItem>() {
            @Override
            public int compare(SlaLowSliItem a, SlaLowSliItem b) {
                return Double.compare(a.getAnnualSliValue(), b.getAnnualSliValue());
            }
        });
        log.info("低 SLI 合并共 {} 条", all.size());
        return all;
    }

    private List<SlaLowSliItem> collectInventory(GrafanaClient.Session session,
                                                 List<GrafanaProperties.SliSourceConfig> sources,
                                                 boolean filterLowSli,
                                                 double threshold) {
        List<SlaLowSliItem> all = new ArrayList<SlaLowSliItem>();
        if (sources == null) {
            return all;
        }

        for (GrafanaProperties.SliSourceConfig source : sources) {
            if (source.getDashboardUid() == null || source.getDashboardUid().trim().isEmpty()) {
                log.warn("SLI 源 [{}] 未配置 dashboard-uid，跳过", source.getName());
                continue;
            }
            JsonNode dashboard = grafanaClient.fetchDashboard(session, source.getDashboardUid());
            List<GrafanaProperties.PanelConfig> panels = source.getPanels();
            if (panels == null || panels.isEmpty()) {
                log.warn("SLI 源 [{}] 未配置 panels，跳过", source.getName());
                continue;
            }
            int sourceCount = 0;
            for (GrafanaProperties.PanelConfig panel : panels) {
                GrafanaClient.PanelQueryMeta meta = grafanaClient.resolvePanelMeta(dashboard, panel.getId());
                String panelName = panel.getName() != null ? panel.getName() : meta.getTitle();
                List<Map<String, String>> rows;
                if (filterLowSli) {
                    rows = grafanaClient.queryLowSliRows(
                            session, meta, threshold, source.getSliColumn());
                    log.info("SLI 源 [{}] 面板 [{}] 年度SLI < {} 共 {} 条",
                            source.getName(), panelName, threshold, rows.size());
                } else {
                    rows = grafanaClient.queryAllRows(session, meta);
                    log.info("SLI 源 [{}] 面板 [{}] 年度清单全量共 {} 条",
                            source.getName(), panelName, rows.size());
                }

                for (Map<String, String> row : rows) {
                    String systemName = firstNonBlank(row, source.getNameColumns());
                    String annualSli = firstNonBlank(row, Collections.singletonList(
                            source.getSliColumn() == null ? "年度SLI" : source.getSliColumn()));
                    double value = parseSli(annualSli);
                    SlaLowSliItem item = new SlaLowSliItem(panelName, panel.getId(), systemName, annualSli, value, null);
                    item.setSourceLevel(source.getSystemLevel());
                    item.setSourceName(source.getName());
                    all.add(item);
                    sourceCount++;
                }
            }
            log.info("SLI 源 [{}] 合计 {} 条，层级={}", source.getName(), sourceCount, source.getSystemLevel());
        }
        return all;
    }

    private String firstNonBlank(Map<String, String> row, List<String> keys) {
        if (keys != null) {
            for (String key : keys) {
                if (key == null) {
                    continue;
                }
                String value = row.get(key);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return "未知系统";
    }

    private double parseSli(String annualSli) {
        if (annualSli == null) {
            return 0D;
        }
        String cleaned = annualSli.replace("%", "").trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private Double parseNumericSli(String value) {
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

    public static class MonthlySliCollection {
        private final Map<Integer, List<SlaLowSliItem>> lowItemsByMonth =
                new LinkedHashMap<Integer, List<SlaLowSliItem>>();
        private final Map<Integer, Map<String, Integer>> numericCountsByMonth =
                new LinkedHashMap<Integer, Map<String, Integer>>();

        public MonthlySliCollection() {
            for (int month = 1; month <= 12; month++) {
                lowItemsByMonth.put(month, new ArrayList<SlaLowSliItem>());
                numericCountsByMonth.put(month, new LinkedHashMap<String, Integer>());
            }
        }

        public Map<Integer, List<SlaLowSliItem>> getLowItemsByMonth() {
            return lowItemsByMonth;
        }

        public int getNumericCount(int month, String level) {
            Integer count = numericCountsByMonth.get(month).get(level);
            return count == null ? 0 : count;
        }

        private void incrementNumeric(int month, String level) {
            Map<String, Integer> counts = numericCountsByMonth.get(month);
            Integer current = counts.get(level);
            counts.put(level, current == null ? 1 : current + 1);
        }
    }
}
