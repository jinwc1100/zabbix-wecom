package com.sla.notifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sla.downtime")
public class SlaDowntimeProperties {

    private int recentWindowDays = 30;

    /** parent_first | leaf_first */
    private String levelPriority = "parent_first";

    /** keep | omit */
    private String unmatchedPolicy = "keep";

    private String remarkWithRecent = "此前月份亦有宕机明细（共 {count} 条未展开）";

    private String remarkOnlyHistorical = "近 1 个月无宕机记录，此前月份有宕机明细（共 {count} 条）";

    private String remarkNone = "无宕机明细";

    public int getRecentWindowDays() {
        return recentWindowDays;
    }

    public void setRecentWindowDays(int recentWindowDays) {
        this.recentWindowDays = recentWindowDays;
    }

    public String getLevelPriority() {
        return levelPriority;
    }

    public void setLevelPriority(String levelPriority) {
        this.levelPriority = levelPriority;
    }

    public String getUnmatchedPolicy() {
        return unmatchedPolicy;
    }

    public void setUnmatchedPolicy(String unmatchedPolicy) {
        this.unmatchedPolicy = unmatchedPolicy;
    }

    public String getRemarkWithRecent() {
        return remarkWithRecent;
    }

    public void setRemarkWithRecent(String remarkWithRecent) {
        this.remarkWithRecent = remarkWithRecent;
    }

    public String getRemarkOnlyHistorical() {
        return remarkOnlyHistorical;
    }

    public void setRemarkOnlyHistorical(String remarkOnlyHistorical) {
        this.remarkOnlyHistorical = remarkOnlyHistorical;
    }

    public String getRemarkNone() {
        return remarkNone;
    }

    public void setRemarkNone(String remarkNone) {
        this.remarkNone = remarkNone;
    }

    public boolean isParentFirst() {
        return levelPriority == null || !"leaf_first".equalsIgnoreCase(levelPriority);
    }

    public boolean keepUnmatched() {
        return unmatchedPolicy == null || !"omit".equalsIgnoreCase(unmatchedPolicy);
    }

    public String formatRemark(String template, int count) {
        if (template == null) {
            return null;
        }
        return template.replace("{count}", String.valueOf(count));
    }
}
