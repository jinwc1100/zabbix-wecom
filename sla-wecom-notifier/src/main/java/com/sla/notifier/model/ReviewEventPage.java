package com.sla.notifier.model;

import java.util.ArrayList;
import java.util.List;

public class ReviewEventPage {

    private long total;
    private int page;
    private int size;
    private List<SlaReviewEvent> items = new ArrayList<SlaReviewEvent>();

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<SlaReviewEvent> getItems() {
        return items;
    }

    public void setItems(List<SlaReviewEvent> items) {
        this.items = items == null ? new ArrayList<SlaReviewEvent>() : items;
    }
}
