package com.sla.notifier.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReviewOverview {

    private double sliThreshold;
    private long incompleteParentCount;
    private long incompleteChildCount;
    private List<OwnerIncompleteCount> incompleteByOwner =
            new ArrayList<OwnerIncompleteCount>();
    private List<SlaLowSliItem> lowSliParents = new ArrayList<SlaLowSliItem>();
    private List<SlaLowSliItem> lowSliChildren = new ArrayList<SlaLowSliItem>();
    private List<SlaLowSliItem> lowMonthlySliParents = new ArrayList<SlaLowSliItem>();
    private List<SlaLowSliItem> lowMonthlySliChildren = new ArrayList<SlaLowSliItem>();
    private int monthlyReportYear;
    private int monthlyCurrentMonth;
    private Map<Integer, MonthlySliMonth> lowMonthlyByMonth =
            new LinkedHashMap<Integer, MonthlySliMonth>();
    private List<CountedSlaDowntime> countedSlaDowntimes =
            new ArrayList<CountedSlaDowntime>();
    private String countedDowntimeError;
    private String grafanaError;
    private String monthlyGrafanaError;

    public double getSliThreshold() {
        return sliThreshold;
    }

    public void setSliThreshold(double sliThreshold) {
        this.sliThreshold = sliThreshold;
    }

    public long getIncompleteParentCount() {
        return incompleteParentCount;
    }

    public void setIncompleteParentCount(long incompleteParentCount) {
        this.incompleteParentCount = incompleteParentCount;
    }

    public long getIncompleteChildCount() {
        return incompleteChildCount;
    }

    public void setIncompleteChildCount(long incompleteChildCount) {
        this.incompleteChildCount = incompleteChildCount;
    }

    public List<OwnerIncompleteCount> getIncompleteByOwner() {
        return incompleteByOwner;
    }

    public void setIncompleteByOwner(List<OwnerIncompleteCount> incompleteByOwner) {
        this.incompleteByOwner = incompleteByOwner;
    }

    public List<SlaLowSliItem> getLowSliParents() {
        return lowSliParents;
    }

    public void setLowSliParents(List<SlaLowSliItem> lowSliParents) {
        this.lowSliParents = lowSliParents;
    }

    public List<SlaLowSliItem> getLowSliChildren() {
        return lowSliChildren;
    }

    public void setLowSliChildren(List<SlaLowSliItem> lowSliChildren) {
        this.lowSliChildren = lowSliChildren;
    }

    public List<SlaLowSliItem> getLowMonthlySliParents() {
        return lowMonthlySliParents;
    }

    public void setLowMonthlySliParents(List<SlaLowSliItem> lowMonthlySliParents) {
        this.lowMonthlySliParents = lowMonthlySliParents;
    }

    public List<SlaLowSliItem> getLowMonthlySliChildren() {
        return lowMonthlySliChildren;
    }

    public void setLowMonthlySliChildren(List<SlaLowSliItem> lowMonthlySliChildren) {
        this.lowMonthlySliChildren = lowMonthlySliChildren;
    }

    public int getMonthlyReportYear() {
        return monthlyReportYear;
    }

    public void setMonthlyReportYear(int monthlyReportYear) {
        this.monthlyReportYear = monthlyReportYear;
    }

    public int getMonthlyCurrentMonth() {
        return monthlyCurrentMonth;
    }

    public void setMonthlyCurrentMonth(int monthlyCurrentMonth) {
        this.monthlyCurrentMonth = monthlyCurrentMonth;
    }

    public Map<Integer, MonthlySliMonth> getLowMonthlyByMonth() {
        return lowMonthlyByMonth;
    }

    public void setLowMonthlyByMonth(Map<Integer, MonthlySliMonth> lowMonthlyByMonth) {
        this.lowMonthlyByMonth = lowMonthlyByMonth;
    }

    public List<CountedSlaDowntime> getCountedSlaDowntimes() {
        return countedSlaDowntimes;
    }

    public void setCountedSlaDowntimes(List<CountedSlaDowntime> countedSlaDowntimes) {
        this.countedSlaDowntimes = countedSlaDowntimes;
    }

    public String getCountedDowntimeError() {
        return countedDowntimeError;
    }

    public void setCountedDowntimeError(String countedDowntimeError) {
        this.countedDowntimeError = countedDowntimeError;
    }

    public String getGrafanaError() {
        return grafanaError;
    }

    public void setGrafanaError(String grafanaError) {
        this.grafanaError = grafanaError;
    }

    public String getMonthlyGrafanaError() {
        return monthlyGrafanaError;
    }

    public void setMonthlyGrafanaError(String monthlyGrafanaError) {
        this.monthlyGrafanaError = monthlyGrafanaError;
    }
}
