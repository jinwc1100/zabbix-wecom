package com.sla.notifier.service;

import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.OwnerIncompleteCount;
import com.sla.notifier.wecom.WeComWebhookClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewIncompleteNudgeServiceTest {

    @Test
    void countAndMentionTwoOwners() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteReviewEvents()).thenReturn(3L);
        when(mapper.listIncompleteOwnerNames()).thenReturn(Arrays.asList("王华聪", "刘彩霞", "王华聪"));
        when(mapper.countIncompleteByOwner()).thenReturn(Arrays.asList(
                ownerCount("王华聪", 2), ownerCount("刘彩霞", 1)));
        SlaSystemOwnerMapper owners = dict("王华聪", "13500000001", "刘彩霞", "13500000002");
        WeComWebhookClient wecom = mock(WeComWebhookClient.class);

        service(mapper, owners, wecom).nudgeAfterSync();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(wecom, times(3)).sendMarkdown(
                md.capture(),
                argThat(names -> names.size() == 2),
                argThat(mobiles -> mobiles.contains("13500000001")
                        && mobiles.contains("13500000002")));
        assertEquals(3, md.getAllValues().size());
        String body = md.getAllValues().get(0);
        assertTrue(body.startsWith("重要的内容说3遍\n"));
        assertTrue(body.contains("SLA 复盘催办：以下责任人尚有宕机明细未复盘。请打开复盘页完成填写并保存。"));
        assertTrue(body.contains("- 王华聪：2 条"));
        assertTrue(body.contains("- 刘彩霞：1 条"));
        assertTrue(body.indexOf("- 王华聪：2 条") < body.indexOf("- 刘彩霞：1 条"));
        assertTrue(body.contains("[打开复盘页](http://10.32.201.201:8086/review/index.html)"));
        assertFalse(body.contains("count_sla"));
        assertFalse(body.contains("条未保存"));
    }

    @Test
    void blankPageUrlOmitsLink() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteReviewEvents()).thenReturn(1L);
        when(mapper.listIncompleteOwnerNames()).thenReturn(Collections.singletonList("王华聪"));
        when(mapper.countIncompleteByOwner()).thenReturn(Collections.singletonList(ownerCount("王华聪", 1)));
        SlaSystemOwnerMapper owners = dict("王华聪", "13500000001", "刘彩霞", "13500000002");
        WeComWebhookClient wecom = mock(WeComWebhookClient.class);
        ReviewSyncProperties sync = new ReviewSyncProperties();
        sync.setPageUrl("  ");

        service(mapper, owners, wecom, sync).nudgeAfterSync();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(wecom, times(3)).sendMarkdown(md.capture(), any(), any());
        String body = md.getAllValues().get(0);
        assertFalse(body.contains("[打开复盘页]"));
        assertTrue(body.contains("SLA 复盘催办：以下责任人尚有宕机明细未复盘。请打开复盘页完成填写并保存。"));
    }

    @Test
    void unspecifiedOwnerCountedNotMentioned() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteReviewEvents()).thenReturn(1L);
        when(mapper.listIncompleteOwnerNames()).thenReturn(Collections.singletonList("待指定责任人"));
        when(mapper.countIncompleteByOwner()).thenReturn(Collections.singletonList(ownerCount("待指定责任人", 1)));
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);
        WeComWebhookClient wecom = mock(WeComWebhookClient.class);

        service(mapper, owners, wecom).nudgeAfterSync();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(wecom, times(3)).sendMarkdown(
                md.capture(), any(), argThat(mobiles -> mobiles.isEmpty()));
        assertTrue(md.getValue().contains("- 待指定责任人：1 条"));
        verify(owners, never()).selectMobileByOwnerName(anyString());
    }

    @Test
    void blankOwnerOmittedFromMarkdownList() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteReviewEvents()).thenReturn(3L);
        when(mapper.listIncompleteOwnerNames()).thenReturn(Arrays.asList("卓松林", "", null));
        when(mapper.countIncompleteByOwner()).thenReturn(Arrays.asList(
                ownerCount("卓松林", 2), ownerCount("", 1)));
        SlaSystemOwnerMapper owners = dict("卓松林", "13500000003", "刘彩霞", "13500000002");
        WeComWebhookClient wecom = mock(WeComWebhookClient.class);

        service(mapper, owners, wecom).nudgeAfterSync();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(wecom, times(3)).sendMarkdown(md.capture(), any(), any());
        String body = md.getValue();
        assertTrue(body.contains("- 卓松林：2 条"));
        assertFalse(body.contains("- ：1 条"));
        assertFalse(body.contains("- ："));
    }

    @Test
    void zeroIncompleteDoesNotSend() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countIncompleteReviewEvents()).thenReturn(0L);
        WeComWebhookClient wecom = mock(WeComWebhookClient.class);
        SlaSystemOwnerMapper owners = mock(SlaSystemOwnerMapper.class);

        service(mapper, owners, wecom).nudgeAfterSync();

        verify(wecom, never()).sendMarkdown(anyString(), any(), any());
        verify(mapper, never()).listIncompleteOwnerNames();
    }

    @Test
    void lookupMobileUsesDictionaryNotSystemName() {
        SlaSystemOwnerMapper owners = dict("王华聪", "13900000000", "刘彩霞", "13800000000");
        ReviewIncompleteNudgeService svc = service(
                mock(SlaReviewEventMapper.class), owners, mock(WeComWebhookClient.class),
                new ReviewSyncProperties());
        assertEquals("13900000000", svc.lookupMobile("王华聪"));
        assertEquals(null, svc.lookupMobile("雄安工程交易系统"));
    }

    private static ReviewIncompleteNudgeService service(SlaReviewEventMapper mapper,
                                                         SlaSystemOwnerMapper owners,
                                                         WeComWebhookClient wecom) {
        return service(mapper, owners, wecom, new ReviewSyncProperties());
    }

    private static ReviewIncompleteNudgeService service(SlaReviewEventMapper mapper,
                                                         SlaSystemOwnerMapper owners,
                                                         WeComWebhookClient wecom,
                                                         ReviewSyncProperties sync) {
        return new ReviewIncompleteNudgeService(mapper, owners, wecom, sync) {
            @Override
            protected void pauseBetweenSends() {
                // No delay in unit tests.
            }
        };
    }

    private static OwnerIncompleteCount ownerCount(String name, long count) {
        OwnerIncompleteCount row = new OwnerIncompleteCount();
        row.setOwnerName(name);
        row.setCount(count);
        return row;
    }

    private static SlaSystemOwnerMapper dict(String n1, String m1, String n2, String m2) {
        SlaSystemOwnerMapper mapper = mock(SlaSystemOwnerMapper.class);
        when(mapper.selectMobileByOwnerName(n1)).thenReturn(m1);
        when(mapper.selectMobileByOwnerName(n2)).thenReturn(m2);
        return mapper;
    }
}
