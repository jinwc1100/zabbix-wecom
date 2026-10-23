package com.sla.notifier.schedule;

import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.service.ReviewEventSyncService;
import com.sla.notifier.service.ReviewIncompleteNudgeService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReviewEventSyncSchedulerTest {

    @Test
    void seventeenHourStartSyncsThenNudges() {
        ReviewSyncProperties props = new ReviewSyncProperties();
        props.setEnabled(true);
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);

        new ReviewEventSyncScheduler(props, sync, nudge)
                .runAt(LocalDateTime.of(2026, 9, 17, 17, 0, 0));

        verify(sync).runNow();
        verify(nudge).nudgeAfterSync();
    }

    @Test
    void nonSeventeenHourSyncsWithoutNudge() {
        ReviewSyncProperties props = new ReviewSyncProperties();
        props.setEnabled(true);
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);

        new ReviewEventSyncScheduler(props, sync, nudge)
                .runAt(LocalDateTime.of(2026, 9, 17, 10, 0, 0));

        verify(sync).runNow();
        verify(nudge, never()).nudgeAfterSync();
    }

    @Test
    void syncFailureSkipsNudge() {
        ReviewSyncProperties props = new ReviewSyncProperties();
        props.setEnabled(true);
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        doThrow(new RuntimeException("grafana down")).when(sync).runNow();
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);

        new ReviewEventSyncScheduler(props, sync, nudge)
                .runAt(LocalDateTime.of(2026, 9, 17, 17, 0, 0));

        verify(nudge, never()).nudgeAfterSync();
    }

    @Test
    void disabledSkipsSyncAndNudge() {
        ReviewSyncProperties props = new ReviewSyncProperties();
        props.setEnabled(false);
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);

        new ReviewEventSyncScheduler(props, sync, nudge)
                .runAt(LocalDateTime.of(2026, 9, 17, 17, 0, 0));

        verify(sync, never()).runNow();
        verify(nudge, never()).nudgeAfterSync();
    }
}
