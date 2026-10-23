package com.sla.notifier.model;

public class SliSnapshotSyncResult {

    private boolean success;
    private int upserted;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getUpserted() {
        return upserted;
    }

    public void setUpserted(int upserted) {
        this.upserted = upserted;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
