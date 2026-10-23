package com.sla.notifier.model;

public class SlaSystemOwner {

    private Long id;
    private String systemLevel;
    private String systemName;
    private String ownerName;
    private String mobile;
    private String monitorEdition;
    private String writeJdbcKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getMonitorEdition() {
        return monitorEdition;
    }

    public void setMonitorEdition(String monitorEdition) {
        this.monitorEdition = monitorEdition;
    }

    public String getWriteJdbcKey() {
        return writeJdbcKey;
    }

    public void setWriteJdbcKey(String writeJdbcKey) {
        this.writeJdbcKey = writeJdbcKey;
    }
}
