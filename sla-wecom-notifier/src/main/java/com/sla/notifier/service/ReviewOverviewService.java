package com.sla.notifier.service;

import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.model.CountedSlaDowntime;
import com.sla.notifier.model.MonthlySliMonth;
import com.sla.notifier.model.ReviewOverview;
import com.sla.notifier.model.SlaLowSliItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewOverviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewOverviewService.class);

    private final SlaReviewEventMapper mapper;
    private final GrafanaClient grafanaClient;
    private final AnnualSliService annualSliService;
    private final CountedSlaDowntimeService countedDowntimeService;
    private final GrafanaProperties grafanaProperties;

    public ReviewOverviewService(SlaReviewEventMapper mapper,
                                 GrafanaClient grafanaClient,
                                 AnnualSliService annualSliService,
                                 CountedSlaDowntimeService countedDowntimeService,
                                 GrafanaProperties grafanaProperties) {
        this.mapper = mapper;
        this.grafanaClient = grafanaClient;
        this.annualSliService = annualSliService;
        this.countedDowntimeService = countedDowntimeService;
        this.grafanaProperties = grafanaProperties;
    }

    public ReviewOverview getOverview() {
        ReviewOverview result = new ReviewOverview();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        result.setMonthlyReportYear(now.getYear());
        result.setMonthlyCurrentMonth(now.getMonthValue());
        result.setSliThreshold(grafanaProperties.getSliThreshold());
        result.setIncompleteParentCount(mapper.countIncompleteBySystemLevel("父系统"));
        result.setIncompleteChildCount(mapper.countIncompleteBySystemLevel("子系统"));
        result.setIncompleteByOwner(mapper.countIncompleteByOwner());

        GrafanaClient.Session session;
        try {
            session = grafanaClient.login();
        } catch (RuntimeException e) {
            log.warn("复盘总览 Grafana 登录失败", e);
            String message = messageOf(e);
            result.setGrafanaError(message);
            result.setMonthlyGrafanaError(message);
            result.setCountedDowntimeError(message);
            return result;
        }

        try {
            splitAnnual(result, annualSliService.collectLowSliItems(session));
        } catch (RuntimeException e) {
            log.warn("复盘总览 Grafana 年度 SLI 加载失败", e);
            result.setGrafanaError(messageOf(e));
        }

        try {
            populateMonthly(result, annualSliService.collectMonthlySliData(session));
        } catch (RuntimeException e) {
            log.warn("复盘总览 Grafana 月度 SLI 加载失败", e);
            result.setMonthlyGrafanaError(messageOf(e));
        }

        try {
            List<CountedSlaDowntime> downtimes = countedDowntimeService.collect(session);
            result.setCountedSlaDowntimes(downtimes);
        } catch (RuntimeException e) {
            log.warn("复盘总览计入 SLA 宕机加载失败", e);
            result.setCountedDowntimeError(messageOf(e));
            result.getCountedSlaDowntimes().clear();
        }
        return result;
    }

    private static void splitAnnual(ReviewOverview result, List<SlaLowSliItem> all) {
        List<SlaLowSliItem> parents = new ArrayList<SlaLowSliItem>();
        List<SlaLowSliItem> children = new ArrayList<SlaLowSliItem>();
        splitByLevel(all, parents, children);
        result.setLowSliParents(parents);
        result.setLowSliChildren(children);
    }

    private static void populateMonthly(ReviewOverview result,
                                        AnnualSliService.MonthlySliCollection collection) {
        Map<Integer, MonthlySliMonth> byMonth =
                new LinkedHashMap<Integer, MonthlySliMonth>();
        for (int month = 1; month <= 12; month++) {
            List<SlaLowSliItem> parents = new ArrayList<SlaLowSliItem>();
            List<SlaLowSliItem> children = new ArrayList<SlaLowSliItem>();
            splitByLevel(collection.getLowItemsByMonth().get(month), parents, children);

            MonthlySliMonth data = new MonthlySliMonth();
            data.setParents(parents);
            data.setChildren(children);
            data.setParentStatus(statusOf(
                    parents, collection.getNumericCount(month, "父系统")));
            data.setChildStatus(statusOf(
                    children, collection.getNumericCount(month, "子系统")));
            byMonth.put(month, data);
        }
        result.setLowMonthlyByMonth(byMonth);

        MonthlySliMonth current = byMonth.get(result.getMonthlyCurrentMonth());
        result.setLowMonthlySliParents(current.getParents());
        result.setLowMonthlySliChildren(current.getChildren());
    }

    private static String statusOf(List<SlaLowSliItem> lowItems, int numericCount) {
        if (!lowItems.isEmpty()) {
            return "low";
        }
        return numericCount > 0 ? "ok" : "empty";
    }

    private static void splitByLevel(List<SlaLowSliItem> all,
                                     List<SlaLowSliItem> parents,
                                     List<SlaLowSliItem> children) {
        for (SlaLowSliItem item : all) {
            if ("父系统".equals(item.getSourceLevel())) {
                parents.add(item);
            } else if ("子系统".equals(item.getSourceLevel())) {
                children.add(item);
            }
        }
    }

    private static String messageOf(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Grafana 年度 SLI 查询失败"
                : message.trim();
    }
}
