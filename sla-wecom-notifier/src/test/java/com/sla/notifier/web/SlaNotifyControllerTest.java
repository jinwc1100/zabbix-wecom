package com.sla.notifier.web;

import com.sla.notifier.model.ReviewEventSyncResult;
import com.sla.notifier.model.SliSnapshotSyncResult;
import com.sla.notifier.service.ReviewEventSyncService;
import com.sla.notifier.service.ReviewExcelService;
import com.sla.notifier.service.ReviewIncompleteNudgeService;
import com.sla.notifier.service.SlaReviewNotifyService;
import com.sla.notifier.service.SliSnapshotSyncService;
import com.sla.notifier.service.WeeklyReviewDocService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlaNotifyControllerTest {

    @Test
    void manualSyncNudgesAndReturnsSyncResult() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        ReviewEventSyncResult expected = new ReviewEventSyncResult();
        when(sync.runNow(false, false)).thenReturn(expected);

        ReviewEventSyncResult actual = controller(sync, nudge).reviewEventSync(true, false, false);

        assertSame(expected, actual);
        verify(nudge).nudgeAfterSync();
    }

    @Test
    void manualSyncDefaultDoesNotNudge() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        ReviewEventSyncResult expected = new ReviewEventSyncResult();
        when(sync.runNow(false, false)).thenReturn(expected);

        ReviewEventSyncResult actual = controller(sync, nudge).reviewEventSync(false, false, false);

        assertSame(expected, actual);
        verify(nudge, never()).nudgeAfterSync();
    }

    @Test
    void nudgeFailureStillReturnsSyncResult() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        ReviewEventSyncResult expected = new ReviewEventSyncResult();
        when(sync.runNow(false, false)).thenReturn(expected);
        doThrow(new RuntimeException("wecom down")).when(nudge).nudgeAfterSync();

        assertSame(expected, controller(sync, nudge).reviewEventSync(true, false, false));
    }

    @Test
    void syncFailureDoesNotNudge() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        doThrow(new RuntimeException("grafana down")).when(sync).runNow(false, false);

        assertThrows(RuntimeException.class,
                () -> controller(sync, nudge).reviewEventSync(true, false, false));
        verify(nudge, never()).nudgeAfterSync();
    }

    @Test
    void refreshOwnerPassesTrueAndDoesNotNudgeByDefault() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        ReviewEventSyncResult expected = new ReviewEventSyncResult();
        when(sync.runNow(true, false)).thenReturn(expected);

        ReviewEventSyncResult actual = controller(sync, nudge).reviewEventSync(false, true, false);

        assertSame(expected, actual);
        verify(sync).runNow(true, false);
        verify(nudge, never()).nudgeAfterSync();
    }

    @Test
    void refreshExcludeTimePassesFlagWithoutNudge() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        ReviewEventSyncResult expected = new ReviewEventSyncResult();
        when(sync.runNow(false, true)).thenReturn(expected);

        ReviewEventSyncResult actual = controller(sync, nudge).reviewEventSync(false, false, true);

        assertSame(expected, actual);
        verify(sync).runNow(false, true);
        verify(nudge, never()).nudgeAfterSync();
    }

    @Test
    void sliSnapshotSyncDoesNotNudge() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        SliSnapshotSyncService snapshot = mock(SliSnapshotSyncService.class);
        SliSnapshotSyncResult expected = new SliSnapshotSyncResult();
        expected.setSuccess(true);
        expected.setUpserted(3);
        when(snapshot.runNow()).thenReturn(expected);

        SliSnapshotSyncResult actual = new SlaNotifyController(
                mock(SlaReviewNotifyService.class),
                mock(ReviewExcelService.class),
                mock(WeeklyReviewDocService.class),
                sync,
                nudge,
                snapshot,
                mock(com.sla.notifier.service.ExcludedDowntimeBackfillService.class),
                mock(JdbcTemplate.class)).sliSnapshotSync();

        assertSame(expected, actual);
        verify(nudge, never()).nudgeAfterSync();
        verify(sync, never()).runNow(org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void annualNotifyDoesNotUseIncompleteNudge() {
        ReviewEventSyncService sync = mock(ReviewEventSyncService.class);
        ReviewIncompleteNudgeService nudge = mock(ReviewIncompleteNudgeService.class);
        SlaReviewNotifyService notify = mock(SlaReviewNotifyService.class);

        controller(sync, nudge, notify).notifyNow();

        verify(notify).notifyNow();
        verify(nudge, never()).nudgeAfterSync();
    }

    private static SlaNotifyController controller(ReviewEventSyncService sync,
                                                   ReviewIncompleteNudgeService nudge) {
        return controller(sync, nudge, mock(SlaReviewNotifyService.class));
    }

    private static SlaNotifyController controller(ReviewEventSyncService sync,
                                                   ReviewIncompleteNudgeService nudge,
                                                   SlaReviewNotifyService notify) {
        return new SlaNotifyController(
                notify,
                mock(ReviewExcelService.class),
                mock(WeeklyReviewDocService.class),
                sync,
                nudge,
                mock(SliSnapshotSyncService.class),
                mock(com.sla.notifier.service.ExcludedDowntimeBackfillService.class),
                mock(JdbcTemplate.class));
    }
}
