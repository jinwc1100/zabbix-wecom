package com.sla.notifier.schedule;

import com.sla.notifier.config.SliSnapshotSyncProperties;
import com.sla.notifier.service.SliSnapshotSyncService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SliSnapshotSyncSchedulerTest {

    @Test
    void enabledRunsSync() {
        SliSnapshotSyncProperties props = new SliSnapshotSyncProperties();
        props.setEnabled(true);
        SliSnapshotSyncService sync = mock(SliSnapshotSyncService.class);

        new SliSnapshotSyncScheduler(props, sync).runDaily();

        verify(sync).runNow();
    }

    @Test
    void disabledSkipsSync() {
        SliSnapshotSyncProperties props = new SliSnapshotSyncProperties();
        props.setEnabled(false);
        SliSnapshotSyncService sync = mock(SliSnapshotSyncService.class);

        new SliSnapshotSyncScheduler(props, sync).runDaily();

        verify(sync, never()).runNow();
    }
}
