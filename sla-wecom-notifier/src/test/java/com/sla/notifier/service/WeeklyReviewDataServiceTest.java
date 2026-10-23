package com.sla.notifier.service;

import com.sla.notifier.model.DowntimeEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyReviewDataServiceTest {

    private static final LocalDateTime WINDOW_START =
            LocalDateTime.of(2026, 7, 30, 0, 0);
    private static final LocalDateTime TRIGGER =
            LocalDateTime.of(2026, 8, 21, 11, 0);

    @Test
    void includesClosedWindowBoundaries() {
        assertTrue(WeeklyReviewDataService.isEligible(
                event(WINDOW_START, "未排除"), WINDOW_START, TRIGGER));
        assertTrue(WeeklyReviewDataService.isEligible(
                event(TRIGGER, "-"), WINDOW_START, TRIGGER));
    }

    @Test
    void excludesOutsideWindowAndUnparseableStart() {
        assertFalse(WeeklyReviewDataService.isEligible(
                event(WINDOW_START.minusSeconds(1), "未排除"), WINDOW_START, TRIGGER));
        assertFalse(WeeklyReviewDataService.isEligible(
                event(TRIGGER.plusSeconds(1), "未排除"), WINDOW_START, TRIGGER));
        assertFalse(WeeklyReviewDataService.isEligible(
                event(null, "未排除"), WINDOW_START, TRIGGER));
    }

    @Test
    void acceptsOnlyUnexcludedStatuses() {
        assertTrue(WeeklyReviewDataService.isUnexcluded(" 未排除 "));
        assertTrue(WeeklyReviewDataService.isUnexcluded(" - "));
        assertFalse(WeeklyReviewDataService.isUnexcluded("部分排除"));
        assertFalse(WeeklyReviewDataService.isUnexcluded(null));
        assertTrue(WeeklyReviewDataService.allowsInsert(null));
        assertFalse(WeeklyReviewDataService.allowsInsert("部分排除"));
    }

    @Test
    void includeExcludedCollectsPartialExclusionInWindow() {
        assertTrue(WeeklyReviewDataService.isEligible(
                event(WINDOW_START, "部分排除"), WINDOW_START, TRIGGER, true));
        assertFalse(WeeklyReviewDataService.isEligible(
                event(WINDOW_START, "部分排除"), WINDOW_START, TRIGGER, false));
    }

    @Test
    void parsesConfiguredWindowStart() {
        assertTrue(WINDOW_START.equals(
                WeeklyReviewDataService.parseWindowStart("2026-07-30 00:00:00")));
        assertThrows(IllegalStateException.class,
                () -> WeeklyReviewDataService.parseWindowStart("2026/07/30"));
    }

    private DowntimeEvent event(LocalDateTime start, String status) {
        DowntimeEvent event = new DowntimeEvent();
        event.setStartTime(start);
        event.setExclusionStatus(status);
        return event;
    }
}
