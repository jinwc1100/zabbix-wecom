package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.model.ReviewEventPage;
import com.sla.notifier.model.ReviewEventQuery;
import com.sla.notifier.model.ReviewEventUpdateRequest;
import com.sla.notifier.model.SlaFaultCategory;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.util.TimeFormatUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReviewEventServiceTest {

    @Test
    void defaultMonthUsesShanghaiClockAndTrimsBlankOwner() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T04:00:00Z"), TimeFormatUtil.ZONE);

        new ReviewEventService(mapper, clock).list(" 大同 ", " ", null, null, null, null, null, null);

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(captor.capture());
        ReviewEventQuery query = captor.getValue();
        assertEquals("大同", query.getSystem());
        assertNull(query.getOwner());
        assertEquals("202608", query.getPeriodMonth());
        assertNull(query.getReviewStatus());
        assertNull(query.getCountSla());
        assertNull(query.getRangeStart());
        assertEquals(0, query.getOffset());
        assertEquals(50, query.getSize());
    }

    @Test
    void monthParamMapsToPeriodMonth() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());

        new ReviewEventService(mapper).list(null, "卓", "month", "2026-07", null, null, 1, 50);

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(captor.capture());
        assertEquals("202607", captor.getValue().getPeriodMonth());
        assertEquals("卓", captor.getValue().getOwner());
        assertNull(captor.getValue().getRangeStart());
    }

    @Test
    void inclusiveRangeUsesStartOfNextDayAsExclusiveEnd() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());

        new ReviewEventService(mapper).list(null, null, "range", "2026-07", "2026-07-01", "2026-07-31", 1, 100);

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(captor.capture());
        ReviewEventQuery query = captor.getValue();
        assertNull(query.getPeriodMonth());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), query.getRangeStart());
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), query.getRangeEnd());
        assertEquals(100, query.getSize());
    }

    @Test
    void invalidSizeAndInvertedRangeAreRejected() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        ReviewEventService service = new ReviewEventService(mapper);

        ResponseStatusException sizeError = assertThrows(ResponseStatusException.class,
                () -> service.list(null, null, "month", "2026-07", null, null, 1, 20));
        assertEquals(HttpStatus.BAD_REQUEST, sizeError.getStatus());

        ResponseStatusException rangeError = assertThrows(ResponseStatusException.class,
                () -> service.list(null, null, "range", null, "2026-08-01", "2026-07-01", 1, 50));
        assertEquals(HttpStatus.BAD_REQUEST, rangeError.getStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void pageTwoUsesOffsetAndKeepsTotal() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(86L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());

        ReviewEventPage page = new ReviewEventService(mapper)
                .list(null, null, "month", "2026-07", null, null, 2, 50);

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(captor.capture());
        assertEquals(50, captor.getValue().getOffset());
        assertEquals(86L, page.getTotal());
        assertEquals(2, page.getPage());
        assertEquals(50, page.getSize());
        assertEquals(0, page.getItems().size());
    }

    @Test
    void omittedAndAllReviewStatusSkipCountSlaFilter() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        ReviewEventService service = new ReviewEventService(mapper);

        service.list(null, null, "month", "2026-07", null, null, 1, 50, null);
        service.list(null, null, "month", "2026-07", null, null, 1, 50, " all ");

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper, org.mockito.Mockito.times(2)).listReviewEvents(captor.capture());
        assertNull(captor.getAllValues().get(0).getReviewStatus());
        assertNull(captor.getAllValues().get(1).getReviewStatus());
    }

    @Test
    void incompleteAndCompleteReviewStatusSetOnQuery() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        ReviewEventService service = new ReviewEventService(mapper);

        service.list("大同", null, "month", "2026-07", null, null, 1, 50, "incomplete");
        ArgumentCaptor<ReviewEventQuery> incomplete = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(incomplete.capture());
        assertEquals("incomplete", incomplete.getValue().getReviewStatus());
        assertEquals("大同", incomplete.getValue().getSystem());
        assertEquals("202607", incomplete.getValue().getPeriodMonth());

        org.mockito.Mockito.reset(mapper);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        service.list("大同", "卓", "month", "2026-07", null, null, 1, 50, "complete");
        ArgumentCaptor<ReviewEventQuery> complete = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(complete.capture());
        assertEquals("complete", complete.getValue().getReviewStatus());
        assertEquals("卓", complete.getValue().getOwner());
    }

    @Test
    void invalidReviewStatusIsRejected() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        ReviewEventService service = new ReviewEventService(mapper);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(null, null, "month", "2026-07", null, null, 1, 50, "pending"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void allTimeAndParentLevelAreAppliedWithoutMonth() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());

        new ReviewEventService(mapper).list(null, null, "all", null, null, null,
                1, 50, "incomplete", "父系统");

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(captor.capture());
        ReviewEventQuery query = captor.getValue();
        assertNull(query.getPeriodMonth());
        assertNull(query.getRangeStart());
        assertNull(query.getRangeEnd());
        assertEquals("incomplete", query.getReviewStatus());
        assertEquals("父系统", query.getSystemLevel());
    }

    @Test
    void emptyOwnerTokenAndInvalidSystemLevel() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        ReviewEventService service = new ReviewEventService(mapper);

        service.list(null, "__empty__", "all", null, null, null,
                1, 50, "incomplete", null);
        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(captor.capture());
        assertEquals(true, captor.getValue().isOwnerEmptyOnly());
        assertNull(captor.getValue().getOwner());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(null, null, "all", null, null, null,
                        1, 50, "incomplete", "未知"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void omittedAndAllCountSlaSkipEqualityFilter() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        ReviewEventService service = new ReviewEventService(mapper);

        service.list(null, null, "month", "2026-07", null, null, 1, 50, null, null, null);
        service.list(null, null, "month", "2026-07", null, null, 1, 50, null, null, " all ");

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper, org.mockito.Mockito.times(2)).listReviewEvents(captor.capture());
        assertNull(captor.getAllValues().get(0).getCountSla());
        assertNull(captor.getAllValues().get(1).getCountSla());
    }

    @Test
    void countSlaYesAndNoSetExactMatchOnQuery() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        ReviewEventService service = new ReviewEventService(mapper);

        service.list("大同", null, "month", "2026-07", null, null, 1, 50, null, null, "是");
        ArgumentCaptor<ReviewEventQuery> yes = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(yes.capture());
        assertEquals("是", yes.getValue().getCountSla());
        assertEquals("大同", yes.getValue().getSystem());
        assertEquals("202607", yes.getValue().getPeriodMonth());

        org.mockito.Mockito.reset(mapper);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());
        service.list("大同", null, "month", "2026-07", null, null, 1, 50, "complete", null, "否");
        ArgumentCaptor<ReviewEventQuery> no = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(no.capture());
        assertEquals("否", no.getValue().getCountSla());
        assertEquals("complete", no.getValue().getReviewStatus());
    }

    @Test
    void invalidCountSlaIsRejected() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        ReviewEventService service = new ReviewEventService(mapper);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(null, null, "month", "2026-07", null, null, 1, 50,
                        null, null, "true"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void incompleteReviewStatusAndCountSlaYesBothApplied() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        when(mapper.countReviewEvents(any(ReviewEventQuery.class))).thenReturn(0L);
        when(mapper.listReviewEvents(any(ReviewEventQuery.class)))
                .thenReturn(Collections.<SlaReviewEvent>emptyList());

        ReviewEventPage page = new ReviewEventService(mapper)
                .list(null, null, "month", "2026-07", null, null, 1, 50,
                        "incomplete", null, "是");

        ArgumentCaptor<ReviewEventQuery> captor = ArgumentCaptor.forClass(ReviewEventQuery.class);
        verify(mapper).listReviewEvents(captor.capture());
        assertEquals("incomplete", captor.getValue().getReviewStatus());
        assertEquals("是", captor.getValue().getCountSla());
        assertEquals(0L, page.getTotal());
        assertEquals(0, page.getItems().size());
    }

    @Test
    void missingRootCauseIsRejectedBeforeWriting() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        ReviewEventUpdateRequest request = completeRequest();
        request.setRootCause(" ");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new ReviewEventService(mapper).update(1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void unknownCategoryPairIsRejected() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        ReviewEventUpdateRequest request = completeRequest();
        when(mapper.findFaultCategory("直接性故障", "应用类故障")).thenReturn(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new ReviewEventService(mapper).update(1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void completeRowUsesDictionarySlaAndOnlyReviewUpdate() {
        SlaReviewEventMapper mapper = mock(SlaReviewEventMapper.class);
        SlaFaultCategory category = new SlaFaultCategory();
        category.setCountSla("是");
        when(mapper.findFaultCategory("直接性故障", "应用类故障")).thenReturn(category);
        when(mapper.updateReviewFields(any(SlaReviewEvent.class))).thenAnswer(invocation -> {
            SlaReviewEvent value = invocation.getArgument(0);
            assertEquals("是", value.getCountSla());
            assertEquals("处理完成", value.getResolutionProcess());
            return 1;
        });
        SlaReviewEvent saved = new SlaReviewEvent();
        saved.setId(9L);
        when(mapper.findById(9L)).thenReturn(saved);
        ExcludedDowntimeWriteService writer = mock(ExcludedDowntimeWriteService.class);

        assertEquals(9L, new ReviewEventService(mapper, java.time.Clock.system(TimeFormatUtil.ZONE), writer)
                .update(9L, completeRequest()).getId());
        verify(writer).writeAfterSave(saved);
    }

    private static ReviewEventUpdateRequest completeRequest() {
        ReviewEventUpdateRequest request = new ReviewEventUpdateRequest();
        request.setFaultCategory("直接性故障");
        request.setFaultSubcategory("应用类故障");
        request.setFaultDesc("应用异常");
        request.setResolutionProcess("处理完成");
        request.setRootCause("代码缺陷");
        request.setImprovement("增加测试");
        return request;
    }
}
