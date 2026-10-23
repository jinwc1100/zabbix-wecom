package com.sla.notifier.service;

import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.model.ReviewEventPage;
import com.sla.notifier.model.ReviewEventQuery;
import com.sla.notifier.model.ReviewEventUpdateRequest;
import com.sla.notifier.model.SlaFaultCategory;
import com.sla.notifier.model.SlaReviewEvent;
import com.sla.notifier.util.TimeFormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ReviewEventService {

    private final SlaReviewEventMapper mapper;
    private final Clock clock;
    private final ExcludedDowntimeWriteService excludedDowntimeWriteService;

    @Autowired
    public ReviewEventService(SlaReviewEventMapper mapper,
                              ExcludedDowntimeWriteService excludedDowntimeWriteService) {
        this(mapper, Clock.system(TimeFormatUtil.ZONE), excludedDowntimeWriteService);
    }

    ReviewEventService(SlaReviewEventMapper mapper) {
        this(mapper, Clock.system(TimeFormatUtil.ZONE), null);
    }

    ReviewEventService(SlaReviewEventMapper mapper, Clock clock) {
        this(mapper, clock, null);
    }

    ReviewEventService(SlaReviewEventMapper mapper, Clock clock,
                       ExcludedDowntimeWriteService excludedDowntimeWriteService) {
        this.mapper = mapper;
        this.clock = clock;
        this.excludedDowntimeWriteService = excludedDowntimeWriteService;
    }

    public List<SlaFaultCategory> listFaultCategories() {
        return mapper.listFaultCategories();
    }

    public ReviewEventPage list(String system, String owner, String timeMode, String month,
                                String from, String to, Integer page, Integer size) {
        return list(system, owner, timeMode, month, from, to, page, size, null);
    }

    public ReviewEventPage list(String system, String owner, String timeMode, String month,
                                String from, String to, Integer page, Integer size,
                                String reviewStatus) {
        return list(system, owner, timeMode, month, from, to, page, size,
                reviewStatus, null);
    }

    public ReviewEventPage list(String system, String owner, String timeMode, String month,
                                String from, String to, Integer page, Integer size,
                                String reviewStatus, String systemLevel) {
        return list(system, owner, timeMode, month, from, to, page, size,
                reviewStatus, systemLevel, null);
    }

    public ReviewEventPage list(String system, String owner, String timeMode, String month,
                                String from, String to, Integer page, Integer size,
                                String reviewStatus, String systemLevel, String countSla) {
        int resolvedPage = page == null ? 1 : page.intValue();
        int resolvedSize = size == null ? 50 : size.intValue();
        if (resolvedPage < 1) {
            throw badRequest("页码必须从 1 开始");
        }
        if (resolvedSize != 50 && resolvedSize != 100) {
            throw badRequest("每页条数只能是 50 或 100");
        }

        ReviewEventQuery query = new ReviewEventQuery();
        query.setSystem(trimToNull(system));
        String resolvedOwner = trimToNull(owner);
        if ("__empty__".equals(resolvedOwner)) {
            query.setOwnerEmptyOnly(true);
        } else {
            query.setOwner(resolvedOwner);
        }
        applySystemLevel(query, systemLevel);
        applyTimeFilter(query, timeMode, month, from, to);
        applyReviewStatus(query, reviewStatus);
        applyCountSla(query, countSla);
        query.setSize(resolvedSize);
        query.setOffset((resolvedPage - 1) * resolvedSize);

        ReviewEventPage result = new ReviewEventPage();
        result.setPage(resolvedPage);
        result.setSize(resolvedSize);
        result.setTotal(mapper.countReviewEvents(query));
        result.setItems(mapper.listReviewEvents(query));
        return result;
    }

    @Transactional
    public SlaReviewEvent update(Long id, ReviewEventUpdateRequest request) {
        if (request == null) {
            throw badRequest("请求内容不能为空");
        }
        String faultCategory = required(request.getFaultCategory(), "故障类别");
        String faultSubcategory = required(request.getFaultSubcategory(), "故障子类");
        String faultDesc = required(request.getFaultDesc(), "故障描述");
        String resolutionProcess = required(request.getResolutionProcess(), "解决过程");
        String rootCause = required(request.getRootCause(), "根本原因");
        String improvement = required(request.getImprovement(), "改进措施");

        SlaFaultCategory category = mapper.findFaultCategory(faultCategory, faultSubcategory);
        if (category == null) {
            throw badRequest("故障类别和故障子类不匹配");
        }

        SlaReviewEvent event = new SlaReviewEvent();
        event.setId(id);
        event.setFaultCategory(faultCategory);
        event.setFaultSubcategory(faultSubcategory);
        event.setFaultDesc(faultDesc);
        event.setCountSla(category.getCountSla());
        event.setResolutionProcess(resolutionProcess);
        event.setRootCause(rootCause);
        event.setImprovement(improvement);
        if (mapper.updateReviewFields(event) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "复盘记录不存在");
        }
        SlaReviewEvent saved = mapper.findById(id);
        if (saved == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "复盘记录不存在");
        }
        if (excludedDowntimeWriteService != null) {
            excludedDowntimeWriteService.writeAfterSave(saved);
        }
        return saved;
    }

    void applyTimeFilter(ReviewEventQuery query, String timeMode, String month, String from, String to) {
        String mode = trimToNull(timeMode);
        if (mode == null || "month".equalsIgnoreCase(mode)) {
            query.setPeriodMonth(resolveMonth(month));
            return;
        }
        if ("range".equalsIgnoreCase(mode)) {
            LocalDate start = parseDate(from, "开始日期");
            LocalDate end = parseDate(to, "结束日期");
            if (start.isAfter(end)) {
                throw badRequest("开始日期不能晚于结束日期");
            }
            query.setRangeStart(start.atStartOfDay());
            query.setRangeEnd(end.plusDays(1).atStartOfDay());
            return;
        }
        if ("all".equalsIgnoreCase(mode)) {
            return;
        }
        throw badRequest("时间方式只能是 month、range 或 all");
    }

    void applySystemLevel(ReviewEventQuery query, String systemLevel) {
        String value = trimToNull(systemLevel);
        if (value == null) {
            return;
        }
        if ("父系统".equals(value) || "子系统".equals(value)) {
            query.setSystemLevel(value);
            return;
        }
        throw badRequest("父/子系统只能是父系统或子系统");
    }

    void applyReviewStatus(ReviewEventQuery query, String reviewStatus) {
        String value = trimToNull(reviewStatus);
        if (value == null || "all".equalsIgnoreCase(value)) {
            query.setReviewStatus(null);
            return;
        }
        if ("incomplete".equalsIgnoreCase(value)) {
            query.setReviewStatus("incomplete");
            return;
        }
        if ("complete".equalsIgnoreCase(value)) {
            query.setReviewStatus("complete");
            return;
        }
        throw badRequest("复盘状态只能是 all、incomplete 或 complete");
    }

    void applyCountSla(ReviewEventQuery query, String countSla) {
        String value = trimToNull(countSla);
        if (value == null || "all".equalsIgnoreCase(value)) {
            query.setCountSla(null);
            return;
        }
        if ("是".equals(value) || "否".equals(value)) {
            query.setCountSla(value);
            return;
        }
        throw badRequest("是否计算SLA只能是 all、是 或 否");
    }

    private String resolveMonth(String month) {
        String value = trimToNull(month);
        if (value == null) {
            return YearMonth.now(clock).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        }
        if (value.matches("^\\d{4}-\\d{2}$")) {
            return value.substring(0, 4) + value.substring(5, 7);
        }
        if (value.matches("^\\d{6}$")) {
            return value;
        }
        throw badRequest("月份格式必须是 yyyy-MM");
    }

    private static LocalDate parseDate(String value, String label) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw badRequest(label + "不能为空");
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw badRequest(label + "格式必须是 yyyy-MM-dd");
        }
    }

    private static String required(String value, String label) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw badRequest(label + "不能为空");
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
