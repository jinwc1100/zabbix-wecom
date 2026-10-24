package com.sla.notifier.model;

public class ReviewEventSyncResult {

    private int collected;
    private int inserted;
    private int skippedExisting;
    private int skippedBadStart;
    private int ownerUpdated;
    private int skippedNotFound;
    private int excludeTimeUpdated;
    private int promoted;
    private int endUpdated;
    private int folded;
    private int exclusionPeriodToUpdated;
    private int routingRowsUpdated;
    private int routingCollisionCount;
    private boolean routingGrafanaDiscoveryOk;
    private int preWindowCollected;
    private int preWindowInserted;
    private int preWindowSkippedExisting;
    private int preWindowSkippedBadStart;
    private String triggerTime;
    private String message;

    public int getCollected() {
        return collected;
    }

    public void setCollected(int collected) {
        this.collected = collected;
    }

    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }

    public int getSkippedExisting() {
        return skippedExisting;
    }

    public void setSkippedExisting(int skippedExisting) {
        this.skippedExisting = skippedExisting;
    }

    public int getSkippedBadStart() {
        return skippedBadStart;
    }

    public void setSkippedBadStart(int skippedBadStart) {
        this.skippedBadStart = skippedBadStart;
    }

    public int getOwnerUpdated() {
        return ownerUpdated;
    }

    public void setOwnerUpdated(int ownerUpdated) {
        this.ownerUpdated = ownerUpdated;
    }

    public int getSkippedNotFound() {
        return skippedNotFound;
    }

    public void setSkippedNotFound(int skippedNotFound) {
        this.skippedNotFound = skippedNotFound;
    }

    public int getExcludeTimeUpdated() {
        return excludeTimeUpdated;
    }

    public void setExcludeTimeUpdated(int excludeTimeUpdated) {
        this.excludeTimeUpdated = excludeTimeUpdated;
    }

    public int getPromoted() {
        return promoted;
    }

    public void setPromoted(int promoted) {
        this.promoted = promoted;
    }

    public int getEndUpdated() {
        return endUpdated;
    }

    public void setEndUpdated(int endUpdated) {
        this.endUpdated = endUpdated;
    }

    public int getFolded() {
        return folded;
    }

    public void setFolded(int folded) {
        this.folded = folded;
    }

    public int getExclusionPeriodToUpdated() {
        return exclusionPeriodToUpdated;
    }

    public void setExclusionPeriodToUpdated(int exclusionPeriodToUpdated) {
        this.exclusionPeriodToUpdated = exclusionPeriodToUpdated;
    }

    public int getRoutingRowsUpdated() {
        return routingRowsUpdated;
    }

    public void setRoutingRowsUpdated(int routingRowsUpdated) {
        this.routingRowsUpdated = routingRowsUpdated;
    }

    public int getRoutingCollisionCount() {
        return routingCollisionCount;
    }

    public void setRoutingCollisionCount(int routingCollisionCount) {
        this.routingCollisionCount = routingCollisionCount;
    }

    public boolean isRoutingGrafanaDiscoveryOk() {
        return routingGrafanaDiscoveryOk;
    }

    public void setRoutingGrafanaDiscoveryOk(boolean routingGrafanaDiscoveryOk) {
        this.routingGrafanaDiscoveryOk = routingGrafanaDiscoveryOk;
    }

    public int getPreWindowCollected() {
        return preWindowCollected;
    }

    public void setPreWindowCollected(int preWindowCollected) {
        this.preWindowCollected = preWindowCollected;
    }

    public int getPreWindowInserted() {
        return preWindowInserted;
    }

    public void setPreWindowInserted(int preWindowInserted) {
        this.preWindowInserted = preWindowInserted;
    }

    public int getPreWindowSkippedExisting() {
        return preWindowSkippedExisting;
    }

    public void setPreWindowSkippedExisting(int preWindowSkippedExisting) {
        this.preWindowSkippedExisting = preWindowSkippedExisting;
    }

    public int getPreWindowSkippedBadStart() {
        return preWindowSkippedBadStart;
    }

    public void setPreWindowSkippedBadStart(int preWindowSkippedBadStart) {
        this.preWindowSkippedBadStart = preWindowSkippedBadStart;
    }

    public String getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(String triggerTime) {
        this.triggerTime = triggerTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
