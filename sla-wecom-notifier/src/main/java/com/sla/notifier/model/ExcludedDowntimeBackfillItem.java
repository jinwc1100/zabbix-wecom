package com.sla.notifier.model;

public class ExcludedDowntimeBackfillItem {

    private Long id;
    private String systemLevel;
    private String systemName;
    private String downStart;
    private String downEnd;
    private String countSla;
    private String writeJdbcKey;
    private String reason;

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

    public String getCountSla() {
        return countSla;
    }

    public void setCountSla(String countSla) {
        this.countSla = countSla;
    }

    public String getWriteJdbcKey() {
        return writeJdbcKey;
    }

    public void setWriteJdbcKey(String writeJdbcKey) {
        this.writeJdbcKey = writeJdbcKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
