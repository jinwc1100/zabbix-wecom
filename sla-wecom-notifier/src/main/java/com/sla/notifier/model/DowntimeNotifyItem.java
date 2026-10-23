package com.sla.notifier.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个低 SLI 系统的近窗宕机汇总（含历史备注）。
 */
public class DowntimeNotifyItem {

    private String systemName;
    private String annualSli;
    private double annualSliValue;
    private String systemLevel;
    /** SLI 面板名：新版监控 / 老版监控（企微精简用） */
    private String monitorSystem;
    private String ownerName;
    private String ownerMobile;
    private boolean ownerMatched;
    private String remark;
    private int historicalCount;
    private int recentCount;
    /** 当月宕机全部已复盘（企微推送应跳过；preview 仍展示） */
    private boolean monthFullyReviewed;
    private List<DowntimePeriod> recentPeriods = new ArrayList<DowntimePeriod>();

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getAnnualSli() {
        return annualSli;
    }

    public void setAnnualSli(String annualSli) {
        this.annualSli = annualSli;
    }

    public double getAnnualSliValue() {
        return annualSliValue;
    }

    public void setAnnualSliValue(double annualSliValue) {
        this.annualSliValue = annualSliValue;
    }

    public String getSystemLevel() {
        return systemLevel;
    }

    public void setSystemLevel(String systemLevel) {
        this.systemLevel = systemLevel;
    }

    public String getMonitorSystem() {
        return monitorSystem;
    }

    public void setMonitorSystem(String monitorSystem) {
        this.monitorSystem = monitorSystem;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerMobile() {
        return ownerMobile;
    }

    public void setOwnerMobile(String ownerMobile) {
        this.ownerMobile = ownerMobile;
    }

    public boolean isOwnerMatched() {
        return ownerMatched;
    }

    public void setOwnerMatched(boolean ownerMatched) {
        this.ownerMatched = ownerMatched;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getHistoricalCount() {
        return historicalCount;
    }

    public void setHistoricalCount(int historicalCount) {
        this.historicalCount = historicalCount;
    }

    public int getRecentCount() {
        return recentCount;
    }

    public void setRecentCount(int recentCount) {
        this.recentCount = recentCount;
    }

    public boolean isMonthFullyReviewed() {
        return monthFullyReviewed;
    }

    public void setMonthFullyReviewed(boolean monthFullyReviewed) {
        this.monthFullyReviewed = monthFullyReviewed;
    }

    /** 企微推送是否应跳过（与 monthFullyReviewed 同义） */
    public boolean isSkipWecomNotify() {
        return monthFullyReviewed;
    }

    public List<DowntimePeriod> getRecentPeriods() {
        return recentPeriods;
    }

    public void setRecentPeriods(List<DowntimePeriod> recentPeriods) {
        this.recentPeriods = recentPeriods;
    }

    public static class DowntimePeriod {
        private String downtimeStart;
        private String downtimeEnd;

        public DowntimePeriod() {
        }

        public DowntimePeriod(String downtimeStart, String downtimeEnd) {
            this.downtimeStart = downtimeStart;
            this.downtimeEnd = downtimeEnd;
        }

        public String getDowntimeStart() {
            return downtimeStart;
        }

        public void setDowntimeStart(String downtimeStart) {
            this.downtimeStart = downtimeStart;
        }

        public String getDowntimeEnd() {
            return downtimeEnd;
        }

        public void setDowntimeEnd(String downtimeEnd) {
            this.downtimeEnd = downtimeEnd;
        }
    }
}
