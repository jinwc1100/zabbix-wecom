package com.sla.notifier.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeeklyReviewNamingTest {

    @Test
    void formatsDocumentTitleWithoutDateHyphens() {
        LocalDateTime trigger = LocalDateTime.of(2026, 8, 21, 11, 0);

        assertEquals("复盘明细-20260821", WeeklyReviewNaming.documentTitle(trigger));
        assertEquals("复盘明细-20260821.xlsx", WeeklyReviewNaming.workbookFilename(trigger));
    }
}
