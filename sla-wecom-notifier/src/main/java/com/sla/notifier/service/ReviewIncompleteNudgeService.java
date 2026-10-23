package com.sla.notifier.service;

import com.sla.notifier.config.ReviewSyncProperties;
import com.sla.notifier.mapper.SlaReviewEventMapper;
import com.sla.notifier.mapper.SlaSystemOwnerMapper;
import com.sla.notifier.model.OwnerIncompleteCount;
import com.sla.notifier.wecom.WeComWebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReviewIncompleteNudgeService {

    private static final Logger log = LoggerFactory.getLogger(ReviewIncompleteNudgeService.class);
    private static final int SEND_TIMES = 3;
    private static final long SEND_INTERVAL_MS = 400L;
    private static final String LINE_PREFIX = "重要的内容说3遍";
    private static final String LINE_BODY =
            "SLA 复盘催办：以下责任人尚有宕机明细未复盘。请打开复盘页完成填写并保存。";
    static final String UNSPECIFIED_OWNER = "待指定责任人";

    private final SlaReviewEventMapper mapper;
    private final SlaSystemOwnerMapper systemOwnerMapper;
    private final WeComWebhookClient weComWebhookClient;
    private final ReviewSyncProperties reviewSyncProperties;

    public ReviewIncompleteNudgeService(SlaReviewEventMapper mapper,
                                        SlaSystemOwnerMapper systemOwnerMapper,
                                        WeComWebhookClient weComWebhookClient,
                                        ReviewSyncProperties reviewSyncProperties) {
        this.mapper = mapper;
        this.systemOwnerMapper = systemOwnerMapper;
        this.weComWebhookClient = weComWebhookClient;
        this.reviewSyncProperties = reviewSyncProperties;
    }

    public void nudgeAfterSync() {
        long count = mapper.countIncompleteReviewEvents();
        if (count <= 0) {
            log.info("sla_review_event 未复盘条数为 0，跳过企微催办");
            return;
        }
        List<String> ownerNames = mapper.listIncompleteOwnerNames();
        Set<String> mentionNames = new LinkedHashSet<String>();
        Set<String> mentionMobiles = new LinkedHashSet<String>();
        if (ownerNames != null) {
            for (String raw : ownerNames) {
                if (raw == null) {
                    continue;
                }
                String name = raw.trim();
                if (name.isEmpty()) {
                    continue;
                }
                if (UNSPECIFIED_OWNER.equals(name)) {
                    continue;
                }
                String mobile = lookupMobile(name);
                if (mobile != null) {
                    mentionNames.add(name);
                    mentionMobiles.add(mobile);
                }
            }
        }
        String markdown = buildMarkdown(mapper.countIncompleteByOwner());
        for (int i = 0; i < SEND_TIMES; i++) {
            weComWebhookClient.sendMarkdown(markdown, mentionNames, mentionMobiles);
            if (i < SEND_TIMES - 1) {
                pauseBetweenSends();
            }
        }
        log.info("未复盘催办已连续发送 {} 次，条数={}，@人数={}",
                Integer.valueOf(SEND_TIMES), Long.valueOf(count),
                Integer.valueOf(mentionMobiles.size()));
    }

    String buildMarkdown(List<OwnerIncompleteCount> byOwner) {
        StringBuilder sb = new StringBuilder();
        sb.append(LINE_PREFIX).append('\n').append(LINE_BODY);
        for (OwnerIncompleteCount row : namedOwnerCounts(byOwner)) {
            sb.append('\n').append("- ").append(row.getOwnerName().trim())
                    .append("：").append(row.getCount()).append(" 条");
        }
        String pageUrl = reviewSyncProperties.getPageUrl();
        if (pageUrl != null) {
            String trimmed = pageUrl.trim();
            if (!trimmed.isEmpty()) {
                sb.append('\n').append("[打开复盘页](").append(trimmed).append(')');
            }
        }
        return sb.toString();
    }

    static List<OwnerIncompleteCount> namedOwnerCounts(List<OwnerIncompleteCount> byOwner) {
        if (byOwner == null || byOwner.isEmpty()) {
            return Collections.emptyList();
        }
        List<OwnerIncompleteCount> named = new ArrayList<OwnerIncompleteCount>();
        for (OwnerIncompleteCount row : byOwner) {
            if (row == null || row.getOwnerName() == null) {
                continue;
            }
            if (row.getOwnerName().trim().isEmpty()) {
                continue;
            }
            named.add(row);
        }
        return named;
    }

    protected void pauseBetweenSends() {
        try {
            Thread.sleep(SEND_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("企微催办连续发送被中断", e);
        }
    }

    String lookupMobile(String ownerName) {
        String mobile = systemOwnerMapper.selectMobileByOwnerName(ownerName);
        if (mobile == null || mobile.trim().isEmpty()) {
            return null;
        }
        return mobile.trim();
    }
}
