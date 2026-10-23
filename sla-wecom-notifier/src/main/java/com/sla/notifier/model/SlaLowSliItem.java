package com.sla.notifier.model;

public class SlaLowSliItem {

    private String panelName;
    private int panelId;
    private String systemName;
    private String annualSli;
    private double annualSliValue;
    private String owner;
    /** Plan B: 父系统 / 子系统 from SLI dashboard source */
    private String sourceLevel;
    private String sourceName;

    public SlaLowSliItem() {
    }

    public SlaLowSliItem(String panelName, int panelId, String systemName,
                         String annualSli, double annualSliValue, String owner) {
        this.panelName = panelName;
        this.panelId = panelId;
        this.systemName = systemName;
        this.annualSli = annualSli;
        this.annualSliValue = annualSliValue;
        this.owner = owner;
    }

    public String getPanelName() {
        return panelName;
    }

    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }

    public int getPanelId() {
        return panelId;
    }

    public void setPanelId(int panelId) {
        this.panelId = panelId;
    }

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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSourceLevel() {
        return sourceLevel;
    }

    public void setSourceLevel(String sourceLevel) {
        this.sourceLevel = sourceLevel;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
}
