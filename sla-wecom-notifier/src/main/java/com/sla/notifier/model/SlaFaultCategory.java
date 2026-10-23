package com.sla.notifier.model;

public class SlaFaultCategory {

    private Long id;
    private String faultCategory;
    private String faultSubcategory;
    private String faultDesc;
    private String countSla;
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
