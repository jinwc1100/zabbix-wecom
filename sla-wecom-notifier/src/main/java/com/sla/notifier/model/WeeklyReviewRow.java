package com.sla.notifier.model;

public class WeeklyReviewRow {

    private final String systemLevel;
    private final String systemName;
    private final String downtimeStart;
    private final String downtimeEnd;
    private final String ownerName;
    private final String ownerMobile;
    private final boolean ownerMatched;
    private final String exclusionStatus;

    public WeeklyReviewRow(String systemLevel, String systemName,
                           String downtimeStart, String downtimeEnd,
                           String ownerName, String ownerMobile,
                           boolean ownerMatched) {
        this(systemLevel, systemName, downtimeStart, downtimeEnd,
                ownerName, ownerMobile, ownerMatched, null);
    }

    public WeeklyReviewRow(String systemLevel, String systemName,
                           String downtimeStart, String downtimeEnd,
                           String ownerName, String ownerMobile,
                           boolean ownerMatched, String exclusionStatus) {
        this.systemLevel = systemLevel;
        this.systemName = systemName;
        this.downtimeStart = downtimeStart;
        this.downtimeEnd = downtimeEnd;
        this.ownerName = ownerName;
        this.ownerMobile = ownerMobile;
        this.ownerMatched = ownerMatched;
        this.exclusionStatus = exclusionStatus;
    }

    public String getSystemLevel() {
        return systemLevel;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getDowntimeStart() {
        return downtimeStart;
    }

    public String getDowntimeEnd() {
        return downtimeEnd;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerMobile() {
        return ownerMobile;
    }

    public boolean isOwnerMatched() {
        return ownerMatched;
    }

    public String getExclusionStatus() {
        return exclusionStatus;
    }
}
