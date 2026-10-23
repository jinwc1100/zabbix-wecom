package com.sla.notifier.model;

public class CountedSlaDowntime {

    private String systemLevel;
    private String systemName;
    private String downStart;
    private String downEnd;
    private String reviewDesc;

    public String getSystemLevel() {
        return systemLevel;
    }

    public void setSystemLevel(String systemLevel) {
        this.systemLevel = systemLevel;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getDownStart() {
        return downStart;
    }

    public void setDownStart(String downStart) {
        this.downStart = downStart;
    }

    public String getDownEnd() {
        return downEnd;
    }

    public void setDownEnd(String downEnd) {
        this.downEnd = downEnd;
    }

    public String getReviewDesc() {
        return reviewDesc;
    }

    public void setReviewDesc(String reviewDesc) {
        this.reviewDesc = reviewDesc;
    }

    public String dedupeKey() {
        return String.valueOf(systemLevel) + "|" + String.valueOf(systemName)
                + "|" + String.valueOf(downStart) + "|" + String.valueOf(downEnd);
    }
}
