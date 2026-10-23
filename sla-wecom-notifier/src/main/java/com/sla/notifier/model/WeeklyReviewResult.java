package com.sla.notifier.model;

import java.util.ArrayList;
import java.util.List;

public class WeeklyReviewResult {

    private boolean success;
    private int rowCount;
    private String documentTitle;
    private String documentUrl;
    private String localPath;
    private String triggerTime;
    private String message;
    private List<String> ownerNames = new ArrayList<String>();
    private List<String> ownerMobiles = new ArrayList<String>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public List<String> getOwnerNames() {
        return ownerNames;
    }

    public void setOwnerNames(List<String> ownerNames) {
        this.ownerNames = ownerNames;
    }

    public List<String> getOwnerMobiles() {
        return ownerMobiles;
    }

    public void setOwnerMobiles(List<String> ownerMobiles) {
        this.ownerMobiles = ownerMobiles;
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
