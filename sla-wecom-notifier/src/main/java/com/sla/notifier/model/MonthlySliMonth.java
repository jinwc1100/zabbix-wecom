package com.sla.notifier.model;

import java.util.ArrayList;
import java.util.List;

public class MonthlySliMonth {

    private List<SlaLowSliItem> parents = new ArrayList<SlaLowSliItem>();
    private List<SlaLowSliItem> children = new ArrayList<SlaLowSliItem>();
    private String parentStatus = "empty";
    private String childStatus = "empty";

    public List<SlaLowSliItem> getParents() {
        return parents;
    }

    public void setParents(List<SlaLowSliItem> parents) {
        this.parents = parents;
    }

    public List<SlaLowSliItem> getChildren() {
        return children;
    }

    public void setChildren(List<SlaLowSliItem> children) {
        this.children = children;
    }

    public String getParentStatus() {
        return parentStatus;
    }

    public void setParentStatus(String parentStatus) {
        this.parentStatus = parentStatus;
    }

    public String getChildStatus() {
        return childStatus;
    }

    public void setChildStatus(String childStatus) {
        this.childStatus = childStatus;
    }
}
