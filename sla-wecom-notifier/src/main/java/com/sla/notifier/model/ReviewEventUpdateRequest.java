package com.sla.notifier.model;

public class ReviewEventUpdateRequest {

    private String faultCategory;
    private String faultSubcategory;
    private String faultDesc;
    private String resolutionProcess;
    private String rootCause;
    private String improvement;

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
}
