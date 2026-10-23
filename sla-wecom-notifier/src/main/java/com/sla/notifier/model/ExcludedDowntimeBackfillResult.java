package com.sla.notifier.model;

import java.util.ArrayList;
import java.util.List;

public class ExcludedDowntimeBackfillResult {

    private boolean dryRun;
    private int reviewed;
    private int skippedAlreadyExcluded;
    private List<ExcludedDowntimeBackfillItem> wouldWrite = new ArrayList<ExcludedDowntimeBackfillItem>();
    private List<ExcludedDowntimeBackfillItem> written = new ArrayList<ExcludedDowntimeBackfillItem>();
    private List<ExcludedDowntimeBackfillItem> skippedNoSla = new ArrayList<ExcludedDowntimeBackfillItem>();
    private List<ExcludedDowntimeBackfillItem> failed = new ArrayList<ExcludedDowntimeBackfillItem>();

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getReviewed() {
        return reviewed;
    }

    public void setReviewed(int reviewed) {
        this.reviewed = reviewed;
    }

    public int getSkippedAlreadyExcluded() {
        return skippedAlreadyExcluded;
    }

    public void setSkippedAlreadyExcluded(int skippedAlreadyExcluded) {
        this.skippedAlreadyExcluded = skippedAlreadyExcluded;
    }

    public List<ExcludedDowntimeBackfillItem> getWouldWrite() {
        return wouldWrite;
    }

    public void setWouldWrite(List<ExcludedDowntimeBackfillItem> wouldWrite) {
        this.wouldWrite = wouldWrite;
    }

    public List<ExcludedDowntimeBackfillItem> getWritten() {
        return written;
    }

    public void setWritten(List<ExcludedDowntimeBackfillItem> written) {
        this.written = written;
    }

    public List<ExcludedDowntimeBackfillItem> getSkippedNoSla() {
        return skippedNoSla;
    }

    public void setSkippedNoSla(List<ExcludedDowntimeBackfillItem> skippedNoSla) {
        this.skippedNoSla = skippedNoSla;
    }

    public List<ExcludedDowntimeBackfillItem> getFailed() {
        return failed;
    }

    public void setFailed(List<ExcludedDowntimeBackfillItem> failed) {
        this.failed = failed;
    }
}
