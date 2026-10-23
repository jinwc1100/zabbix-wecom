package com.sla.notifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "grafana")
public class GrafanaProperties {

    private String baseUrl;
    private String username;
    private String password;
    /** @deprecated use sliSources; kept for fallback */
    private String dashboardUid;
    private double sliThreshold = 99.9;
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 180000;
    /** @deprecated use sliSources; kept for fallback */
    private List<PanelConfig> panels = new ArrayList<PanelConfig>();
    private List<SliSourceConfig> sliSources = new ArrayList<SliSourceConfig>();
    private List<SliSourceConfig> monthlySliSources = new ArrayList<SliSourceConfig>();
    private DowntimeSourceConfig downtimeLeaf = new DowntimeSourceConfig();
    private DowntimeSourceConfig downtimeParent = new DowntimeSourceConfig();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDashboardUid() {
        return dashboardUid;
    }

    public void setDashboardUid(String dashboardUid) {
        this.dashboardUid = dashboardUid;
    }

    public double getSliThreshold() {
        return sliThreshold;
    }

    public void setSliThreshold(double sliThreshold) {
        this.sliThreshold = sliThreshold;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public List<PanelConfig> getPanels() {
        return panels;
    }

    public void setPanels(List<PanelConfig> panels) {
        this.panels = panels;
    }

    public List<SliSourceConfig> getSliSources() {
        return sliSources;
    }

    public void setSliSources(List<SliSourceConfig> sliSources) {
        this.sliSources = sliSources;
    }

    public List<SliSourceConfig> getMonthlySliSources() {
        return monthlySliSources;
    }

    public void setMonthlySliSources(List<SliSourceConfig> monthlySliSources) {
        this.monthlySliSources = monthlySliSources;
    }

    /**
     * Resolve active SLI sources; fall back to legacy dashboardUid + panels when sli-sources empty.
     */
    public List<SliSourceConfig> resolveSliSources() {
        if (sliSources != null && !sliSources.isEmpty()) {
            return sliSources;
        }
        SliSourceConfig legacy = new SliSourceConfig();
        legacy.setName("legacy");
        legacy.setDashboardUid(dashboardUid);
        legacy.setSystemLevel("子系统");
        legacy.setNameColumns(Arrays.asList("子系统名称", "SLA名称", "服务名称", "子系统", "名称"));
        legacy.setPanels(panels);
        List<SliSourceConfig> one = new ArrayList<SliSourceConfig>();
        one.add(legacy);
        return one;
    }

    public List<SliSourceConfig> resolveMonthlySliSources() {
        if (monthlySliSources == null) {
            return new ArrayList<SliSourceConfig>();
        }
        return monthlySliSources;
    }

    public DowntimeSourceConfig getDowntimeLeaf() {
        return downtimeLeaf;
    }

    public void setDowntimeLeaf(DowntimeSourceConfig downtimeLeaf) {
        this.downtimeLeaf = downtimeLeaf;
    }

    public DowntimeSourceConfig getDowntimeParent() {
        return downtimeParent;
    }

    public void setDowntimeParent(DowntimeSourceConfig downtimeParent) {
        this.downtimeParent = downtimeParent;
    }

    public static class PanelConfig {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SliSourceConfig {
        private String name;
        private String dashboardUid;
        private String systemLevel;
        private String sliColumn = "年度SLI";
        private List<String> nameColumns = new ArrayList<String>();
        private List<PanelConfig> panels = new ArrayList<PanelConfig>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDashboardUid() {
            return dashboardUid;
        }

        public void setDashboardUid(String dashboardUid) {
            this.dashboardUid = dashboardUid;
        }

        public String getSystemLevel() {
            return systemLevel;
        }

        public void setSystemLevel(String systemLevel) {
            this.systemLevel = systemLevel;
        }

        public String getSliColumn() {
            return sliColumn;
        }

        public void setSliColumn(String sliColumn) {
            this.sliColumn = sliColumn;
        }

        public List<String> getNameColumns() {
            return nameColumns;
        }

        public void setNameColumns(List<String> nameColumns) {
            this.nameColumns = nameColumns;
        }

        public List<PanelConfig> getPanels() {
            return panels;
        }

        public void setPanels(List<PanelConfig> panels) {
            this.panels = panels;
        }
    }

    public static class DowntimeSourceConfig {
        private String dashboardUid;
        private String systemLevel;
        private String nameColumn;
        private String startColumn;
        private String endColumn;
        /** 排除状态列名，默认「排除状态」 */
        private String exclusionColumn = "排除状态";
        /** 排除原因列名，默认「排除原因」 */
        private String reasonColumn = "排除原因";
        private List<PanelConfig> panels = new ArrayList<PanelConfig>();

        public String getDashboardUid() {
            return dashboardUid;
        }

        public void setDashboardUid(String dashboardUid) {
            this.dashboardUid = dashboardUid;
        }

        public String getSystemLevel() {
            return systemLevel;
        }

        public void setSystemLevel(String systemLevel) {
            this.systemLevel = systemLevel;
        }

        public String getNameColumn() {
            return nameColumn;
        }

        public void setNameColumn(String nameColumn) {
            this.nameColumn = nameColumn;
        }

        public String getStartColumn() {
            return startColumn;
        }

        public void setStartColumn(String startColumn) {
            this.startColumn = startColumn;
        }

        public String getEndColumn() {
            return endColumn;
        }

        public void setEndColumn(String endColumn) {
            this.endColumn = endColumn;
        }

        public String getExclusionColumn() {
            return exclusionColumn;
        }

        public void setExclusionColumn(String exclusionColumn) {
            this.exclusionColumn = exclusionColumn;
        }

        public String getReasonColumn() {
            return reasonColumn;
        }

        public void setReasonColumn(String reasonColumn) {
            this.reasonColumn = reasonColumn;
        }

        public List<PanelConfig> getPanels() {
            return panels;
        }

        public void setPanels(List<PanelConfig> panels) {
            this.panels = panels;
        }
    }
}
