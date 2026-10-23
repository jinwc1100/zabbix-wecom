package com.sla.notifier.model;

import java.time.LocalDateTime;

/** 原始宕机事件（去重前/后索引用）。 */
public class DowntimeEvent {

    private String systemName;
    private String systemLevel;
    private String startRaw;
    private String endRaw;
    private String startDisplay;
    private String endDisplay;
    private LocalDateTime startTime;
    /** 看板「排除状态」原文，如 未排除 / - / 部分排除 */
    private String exclusionStatus;
    /** 看板「排除原因」原文 */
    private String exclusionReason;

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemLevel() {
        return systemLevel;
    }

    public void setSystemLevel(String systemLevel) {
        this.systemLevel = systemLevel;
    }

    public String getStartRaw() {
        return startRaw;
    }

    public void setStartRaw(String startRaw) {
        this.startRaw = startRaw;
    }

    public String getEndRaw() {
        return endRaw;
    }

    public void setEndRaw(String endRaw) {
        this.endRaw = endRaw;
    }

    public String getStartDisplay() {
        return startDisplay;
    }

    public void setStartDisplay(String startDisplay) {
        this.startDisplay = startDisplay;
    }

    public String getEndDisplay() {
        return endDisplay;
    }

    public void setEndDisplay(String endDisplay) {
        this.endDisplay = endDisplay;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getExclusionStatus() {
        return exclusionStatus;
    }

    public void setExclusionStatus(String exclusionStatus) {
        this.exclusionStatus = exclusionStatus;
    }

    public String getExclusionReason() {
        return exclusionReason;
    }

    public void setExclusionReason(String exclusionReason) {
        this.exclusionReason = exclusionReason;
    }

    public String dedupeKey() {
        return String.valueOf(systemName) + "|" + String.valueOf(startDisplay) + "|" + String.valueOf(endDisplay);
    }
}
