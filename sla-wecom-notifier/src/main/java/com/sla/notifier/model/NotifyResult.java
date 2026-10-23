package com.sla.notifier.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotifyResult {

    private boolean success;
    private String message;
    /** 完整明细 Markdown（供 preview） */
    private String markdown;
    /** 企微精简 Markdown：仅系统名称 / SLI / 父|子系统 */
    private String wecomMarkdown;
    private int totalSystemCount;
    private int totalRecentDowntimeCount;
    private int parentSystemCount;
    private int subsystemCount;
    private List<DowntimeNotifyItem> items = new ArrayList<DowntimeNotifyItem>();
    private Map<String, List<DowntimeNotifyItem>> groupedByLevel = new LinkedHashMap<String, List<DowntimeNotifyItem>>();

    /** @deprecated 兼容旧字段名，等同 totalSystemCount */
    public int getTotalCount() {
        return totalSystemCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalSystemCount = totalCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public String getWecomMarkdown() {
        return wecomMarkdown;
    }

    public void setWecomMarkdown(String wecomMarkdown) {
        this.wecomMarkdown = wecomMarkdown;
    }

    public int getTotalSystemCount() {
        return totalSystemCount;
    }

    public void setTotalSystemCount(int totalSystemCount) {
        this.totalSystemCount = totalSystemCount;
    }

    public int getTotalRecentDowntimeCount() {
        return totalRecentDowntimeCount;
    }

    public void setTotalRecentDowntimeCount(int totalRecentDowntimeCount) {
        this.totalRecentDowntimeCount = totalRecentDowntimeCount;
    }

    public int getParentSystemCount() {
        return parentSystemCount;
    }

    public void setParentSystemCount(int parentSystemCount) {
        this.parentSystemCount = parentSystemCount;
    }

    public int getSubsystemCount() {
        return subsystemCount;
    }

    public void setSubsystemCount(int subsystemCount) {
        this.subsystemCount = subsystemCount;
    }

    public List<DowntimeNotifyItem> getItems() {
        return items;
    }

    public void setItems(List<DowntimeNotifyItem> items) {
        this.items = items;
    }

    public Map<String, List<DowntimeNotifyItem>> getGroupedByLevel() {
        return groupedByLevel;
    }

    public void setGroupedByLevel(Map<String, List<DowntimeNotifyItem>> groupedByLevel) {
        this.groupedByLevel = groupedByLevel;
    }
}
