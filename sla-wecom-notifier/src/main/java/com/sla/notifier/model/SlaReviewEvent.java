package com.sla.notifier.model;

import java.time.LocalDateTime;

public class SlaReviewEvent {

    private Long id;
    private String periodMonth;
    private String systemLevel;
    private String systemName;
    private LocalDateTime downStart;
    private LocalDateTime downEnd;
    private LocalDateTime excludeTime;
    private String ownerName;
    private String faultCategory;
    private String faultSubcategory;
    private String faultDesc;
    private String countSla;
    private String resolutionProcess;
    private String rootCause;
    private String improvement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(String periodMonth) {
        this.periodMonth = periodMonth;
    }

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

    public LocalDateTime getDownStart() {
        return downStart;
    }

    public void setDownStart(LocalDateTime downStart) {
        this.downStart = downStart;
    }

    public LocalDateTime getDownEnd() {
        return downEnd;
    }

    public void setDownEnd(LocalDateTime downEnd) {
        this.downEnd = downEnd;
    }

    public LocalDateTime getExcludeTime() {
        return excludeTime;
    }

    public void setExcludeTime(LocalDateTime excludeTime) {
        this.excludeTime = excludeTime;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getFaultCategory() {
        return faultCategory;
    }

    public void setFaultCategory(String faultCategory) {
        this.faultCategory = faultCategory;
    }

    public String getFaultSubcategory() {
        return faultSubcategory;
    }

    public void setFaultSubcategory(String faultSubcategory) {
        this.faultSubcategory = faultSubcategory;
    }

    public String getFaultDesc() {
        return faultDesc;
    }

    public void setFaultDesc(String faultDesc) {
        this.faultDesc = faultDesc;
    }

    public String getCountSla() {
        return countSla;
    }

    public void setCountSla(String countSla) {
        this.countSla = countSla;
    }

    public String getResolutionProcess() {
        return resolutionProcess;
    }

    public void setResolutionProcess(String resolutionProcess) {
        this.resolutionProcess = resolutionProcess;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getImprovement() {
        return improvement;
    }

    public void setImprovement(String improvement) {
        this.improvement = improvement;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
