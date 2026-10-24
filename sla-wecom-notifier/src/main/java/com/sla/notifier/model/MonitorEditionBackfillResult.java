package com.sla.notifier.model;

public class MonitorEditionBackfillResult {

    private int totalRows;
    private int rowsUpdated;
    private int collisionCount;
    private boolean grafanaDiscoveryOk;

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getRowsUpdated() {
        return rowsUpdated;
    }

    public void setRowsUpdated(int rowsUpdated) {
        this.rowsUpdated = rowsUpdated;
    }

    public int getCollisionCount() {
        return collisionCount;
    }

    public void setCollisionCount(int collisionCount) {
        this.collisionCount = collisionCount;
    }

    public boolean isGrafanaDiscoveryOk() {
        return grafanaDiscoveryOk;
    }

    public void setGrafanaDiscoveryOk(boolean grafanaDiscoveryOk) {
        this.grafanaDiscoveryOk = grafanaDiscoveryOk;
    }
}
