package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.ExcludedDowntimeBackfillItem;
import com.sla.notifier.model.ExcludedDowntimeBackfillResult;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.model.SlaSystemOwner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcludedDowntimeBackfillService {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SlaReviewEventMapper reviewMapper;
    private final SlaSystemOwnerMapper ownerMapper;
    private final ExcludedDowntimeWriteService writeService;

    public ExcludedDowntimeBackfillService(SlaReviewEventMapper reviewMapper,
                                           SlaSystemOwnerMapper ownerMapper,
                                           ExcludedDowntimeWriteService writeService) {
        this.reviewMapper = reviewMapper;
        this.ownerMapper = ownerMapper;
        this.writeService = writeService;
    }

    public ExcludedDowntimeBackfillResult run(boolean dryRun) {
        ExcludedDowntimeBackfillResult result = new ExcludedDowntimeBackfillResult();
        result.setDryRun(dryRun);
        List<SlaReviewEvent> rows = reviewMapper.listReviewedEvents();
        result.setReviewed(rows == null ? 0 : rows.size());
        if (rows == null) {
            return result;
        }
        for (SlaReviewEvent event : rows) {
            classify(event, dryRun, result);
        }
        return result;
    }

    private void classify(SlaReviewEvent event, boolean dryRun,
                          ExcludedDowntimeBackfillResult result) {
        String level = trim(event.getSystemLevel());
        String name = trim(event.getSystemName());
        SlaSystemOwner owner = ownerMapper.selectByLevelAndName(level, name);
        if (owner == null || isBlank(owner.getWriteJdbcKey())) {
            result.getSkippedNoSla().add(item(event, null, "未找到系统写库标记"));
            return;
        }
        String key = owner.getWriteJdbcKey().trim();
        JdbcTemplate jdbc;
        try {
            jdbc = writeService.jdbcFor(key);
        } catch (ResponseStatusException e) {
            result.getSkippedNoSla().add(item(event, key, "写库标记无效"));
            return;
        }
        Long slaid = writeService.findSlaid(jdbc, name);
        if (slaid == null) {
            result.getSkippedNoSla().add(item(event, key, "目标库无 sla.name 匹配"));
            return;
        }
        if (event.getDownStart() == null || event.getDownEnd() == null) {
            result.getSkippedNoSla().add(item(event, key, "宕机时间为空"));
            return;
        }
        int from = ExcludedDowntimeWriteService.toUnix(event.getDownStart());
        int to = ExcludedDowntimeWriteService.toUnix(event.getDownEnd());
        if (writeService.hasGrafanaOverlap(jdbc, slaid.longValue(), from, to)) {
            result.setSkippedAlreadyExcluded(result.getSkippedAlreadyExcluded() + 1);
            return;
        }
        ExcludedDowntimeBackfillItem row = item(event, key, null);
        result.getWouldWrite().add(row);
        if (dryRun) {
            return;
        }
        try {
            writeService.writeAfterSave(event);
            result.getWritten().add(row);
        } catch (RuntimeException e) {
            ExcludedDowntimeBackfillItem fail = item(event, key,
                    e.getMessage() == null ? "写入失败" : e.getMessage());
            result.getFailed().add(fail);
        }
    }

    private static ExcludedDowntimeBackfillItem item(SlaReviewEvent event, String key, String reason) {
        ExcludedDowntimeBackfillItem item = new ExcludedDowntimeBackfillItem();
        item.setId(event.getId());
        item.setSystemLevel(event.getSystemLevel());
        item.setSystemName(event.getSystemName());
        item.setDownStart(fmt(event.getDownStart()));
        item.setDownEnd(fmt(event.getDownEnd()));
        item.setCountSla(event.getCountSla());
        item.setWriteJdbcKey(key);
        item.setReason(reason);
        return item;
    }

    private static String fmt(LocalDateTime value) {
        return value == null ? "" : TIME.format(value);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
