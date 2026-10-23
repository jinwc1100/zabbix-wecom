package com.sla.notifier.service;

import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.model.DowntimeNotifyItem;
import com.sla.notifier.model.NotifyResult;
import com.sla.notifier.model.SlaLowSliItem;
import com.sla.notifier.model.SlaSystemOwner;
import com.sla.notifier.wecom.WeComWebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SlaReviewNotifyService {

    private static final Logger log = LoggerFactory.getLogger(SlaReviewNotifyService.class);

    private final GrafanaClient grafanaClient;
    private final AnnualSliService annualSliService;
    private final DowntimeQueryService downtimeQueryService;
    private final DowntimeMatchService downtimeMatchService;
    private final OwnerRuleResolver ownerRuleResolver;
    private final SlaMarkdownFormatter markdownFormatter;
    private final WeComWebhookClient weComWebhookClient;

    public SlaReviewNotifyService(GrafanaClient grafanaClient,
                                  AnnualSliService annualSliService,
                                  DowntimeQueryService downtimeQueryService,
                                  DowntimeMatchService downtimeMatchService,
                                  OwnerRuleResolver ownerRuleResolver,
                                  SlaMarkdownFormatter markdownFormatter,
                                  WeComWebhookClient weComWebhookClient) {
        this.grafanaClient = grafanaClient;
        this.annualSliService = annualSliService;
        this.downtimeQueryService = downtimeQueryService;
        this.downtimeMatchService = downtimeMatchService;
        this.ownerRuleResolver = ownerRuleResolver;
        this.markdownFormatter = markdownFormatter;
        this.weComWebhookClient = weComWebhookClient;
    }

    public NotifyResult preview() {
        NotifyResult result = buildReport(false);
        result.setSuccess(true);
        int skipped = countSkipped(result.getItems());
        result.setMessage("预览成功，未推送企业微信；本月已复盘将跳过推送 "
                + skipped + " 个（preview 仍展示）");
        return result;
    }

    public NotifyResult notifyNow() {
        NotifyResult result = buildReport(true);
        Set<String> names = new LinkedHashSet<String>();
        Set<String> mobiles = new LinkedHashSet<String>();
        int pushCount = 0;
        for (DowntimeNotifyItem item : result.getItems()) {
            if (item.isSkipWecomNotify()) {
                continue;
            }
            pushCount++;
            if (item.isOwnerMatched() && item.getOwnerMobile() != null && !item.getOwnerMobile().trim().isEmpty()) {
                mobiles.add(item.getOwnerMobile().trim());
                if (item.getOwnerName() != null) {
                    names.add(item.getOwnerName());
                }
            }
        }
        weComWebhookClient.sendMarkdown(result.getWecomMarkdown(), names, mobiles);
        int skipped = countSkipped(result.getItems());
        result.setSuccess(true);
        result.setMessage("已推送企业微信（精简清单+" + mobiles.size() + "人@），推送系统 "
                + pushCount + " 个，本月已复盘跳过 " + skipped + " 个");
        log.info(result.getMessage());
        return result;
    }

    /**
     * @param forWecomNotify true 时 wecomMarkdown 仅含未跳过系统；items/detail 始终全量
     */
    private NotifyResult buildReport(boolean forWecomNotify) {
        GrafanaClient.Session session = grafanaClient.login();
        List<SlaLowSliItem> lowSliItems = annualSliService.collectLowSliItems(session);
        Map<String, List<DowntimeEvent>> leafIndex = downtimeQueryService.loadLeafIndex(session);
        Map<String, List<DowntimeEvent>> parentIndex = downtimeQueryService.loadParentIndex(session);
        List<DowntimeNotifyItem> items = downtimeMatchService.match(lowSliItems, leafIndex, parentIndex);
        Map<String, SlaSystemOwner> owners = ownerRuleResolver.loadSnapshot();

        for (DowntimeNotifyItem item : items) {
            OwnerRuleResolver.ResolvedOwner owner = ownerRuleResolver.resolve(
                    item.getSystemLevel(), item.getSystemName(), owners);
            item.setOwnerName(owner.getName());
            item.setOwnerMobile(owner.getMobile());
            item.setOwnerMatched(owner.isMatched());
        }

        NotifyResult result = new NotifyResult();
        result.setItems(items);
        result.setTotalSystemCount(items.size());

        int recentCount = 0;
        int parentCount = 0;
        int subCount = 0;
        Map<String, List<DowntimeNotifyItem>> grouped = new LinkedHashMap<String, List<DowntimeNotifyItem>>();
        for (DowntimeNotifyItem item : items) {
            recentCount += item.getRecentCount();
            if ("父系统".equals(item.getSystemLevel())) {
                parentCount++;
            } else if ("子系统".equals(item.getSystemLevel())) {
                subCount++;
            }
            String key = item.getSystemLevel() == null ? "未知" : item.getSystemLevel();
            List<DowntimeNotifyItem> list = grouped.get(key);
            if (list == null) {
                list = new ArrayList<DowntimeNotifyItem>();
                grouped.put(key, list);
            }
            list.add(item);
        }
        result.setTotalRecentDowntimeCount(recentCount);
        result.setParentSystemCount(parentCount);
        result.setSubsystemCount(subCount);
        result.setGroupedByLevel(grouped);
        result.setMarkdown(markdownFormatter.formatDetail(result));

        if (forWecomNotify) {
            NotifyResult wecomView = copyForWecom(result, filterForWecom(items));
            result.setWecomMarkdown(markdownFormatter.formatWecomSummary(wecomView));
        } else {
            result.setWecomMarkdown(markdownFormatter.formatWecomSummary(result));
        }
        return result;
    }

    private static List<DowntimeNotifyItem> filterForWecom(List<DowntimeNotifyItem> items) {
        List<DowntimeNotifyItem> filtered = new ArrayList<DowntimeNotifyItem>();
        for (DowntimeNotifyItem item : items) {
            if (!item.isSkipWecomNotify()) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private static NotifyResult copyForWecom(NotifyResult full, List<DowntimeNotifyItem> filtered) {
        NotifyResult view = new NotifyResult();
        view.setItems(filtered);
        view.setTotalSystemCount(filtered.size());
        int parent = 0;
        int sub = 0;
        Map<String, List<DowntimeNotifyItem>> grouped = new LinkedHashMap<String, List<DowntimeNotifyItem>>();
        for (DowntimeNotifyItem item : filtered) {
            if ("父系统".equals(item.getSystemLevel())) {
                parent++;
            } else if ("子系统".equals(item.getSystemLevel())) {
                sub++;
            }
            String key = item.getSystemLevel() == null ? "未知" : item.getSystemLevel();
            List<DowntimeNotifyItem> list = grouped.get(key);
            if (list == null) {
                list = new ArrayList<DowntimeNotifyItem>();
                grouped.put(key, list);
            }
            list.add(item);
        }
        view.setParentSystemCount(parent);
        view.setSubsystemCount(sub);
        view.setGroupedByLevel(grouped);
        view.setTotalRecentDowntimeCount(full.getTotalRecentDowntimeCount());
        return view;
    }

    private static int countSkipped(List<DowntimeNotifyItem> items) {
        int n = 0;
        if (items == null) {
            return 0;
        }
        for (DowntimeNotifyItem item : items) {
            if (item.isSkipWecomNotify()) {
                n++;
            }
        }
        return n;
    }
}
