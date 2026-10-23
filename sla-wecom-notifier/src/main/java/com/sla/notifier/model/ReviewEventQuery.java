package com.sla.notifier.model;

import java.time.LocalDateTime;

public class ReviewEventQuery {

    private String system;
    private String owner;
    private String systemLevel;
    private boolean ownerEmptyOnly;
    private String periodMonth;
    private String reviewStatus;
    private String countSla;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private int offset;
    private int size;

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSystemLevel() {
        return systemLevel;
    }

    public void setSystemLevel(String systemLevel) {
        this.systemLevel = systemLevel;
    }

    public boolean isOwnerEmptyOnly() {
        return ownerEmptyOnly;
    }

    public void setOwnerEmptyOnly(boolean ownerEmptyOnly) {
        this.ownerEmptyOnly = ownerEmptyOnly;
    }

    public String getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(String periodMonth) {
        this.periodMonth = periodMonth;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getCountSla() {
        return countSla;
    }

    public void setCountSla(String countSla) {
        this.countSla = countSla;
    }

    public LocalDateTime getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(LocalDateTime rangeStart) {
        this.rangeStart = rangeStart;
    }

    public LocalDateTime getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(LocalDateTime rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
