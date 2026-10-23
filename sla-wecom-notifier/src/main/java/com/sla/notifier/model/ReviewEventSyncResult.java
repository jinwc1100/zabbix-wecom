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
