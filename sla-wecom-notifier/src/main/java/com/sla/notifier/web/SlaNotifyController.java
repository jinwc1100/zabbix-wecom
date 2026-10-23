package com.sla.notifier.web;

import com.sla.notifier.model.ExcludedDowntimeBackfillResult;
import com.sla.notifier.model.NotifyResult;
import com.sla.notifier.model.ReviewEventSyncResult;
import com.sla.notifier.model.WeeklyReviewResult;
import com.sla.notifier.model.SliSnapshotSyncResult;
import com.sla.notifier.service.ExcludedDowntimeBackfillService;
import com.sla.notifier.service.ReviewEventSyncService;
import com.sla.notifier.service.ReviewExcelService;
import com.sla.notifier.service.ReviewIncompleteNudgeService;
import com.sla.notifier.service.SlaReviewNotifyService;
import com.sla.notifier.service.SliSnapshotSyncService;
import com.sla.notifier.service.WeeklyReviewDocService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sla")
public class SlaNotifyController {

    private static final Logger log = LoggerFactory.getLogger(SlaNotifyController.class);
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SlaReviewNotifyService notifyService;
    private final ReviewExcelService reviewExcelService;
    private final WeeklyReviewDocService weeklyReviewDocService;
    private final ReviewEventSyncService reviewEventSyncService;
    private final ReviewIncompleteNudgeService reviewIncompleteNudgeService;
    private final SliSnapshotSyncService sliSnapshotSyncService;
    private final ExcludedDowntimeBackfillService excludedDowntimeBackfillService;
    private final JdbcTemplate jdbcTemplate;

    public SlaNotifyController(SlaReviewNotifyService notifyService,
                               ReviewExcelService reviewExcelService,
                               WeeklyReviewDocService weeklyReviewDocService,
                               ReviewEventSyncService reviewEventSyncService,
                               ReviewIncompleteNudgeService reviewIncompleteNudgeService,
                               SliSnapshotSyncService sliSnapshotSyncService,
                               ExcludedDowntimeBackfillService excludedDowntimeBackfillService,
                               JdbcTemplate jdbcTemplate) {
        this.notifyService = notifyService;
        this.reviewExcelService = reviewExcelService;
        this.weeklyReviewDocService = weeklyReviewDocService;
        this.reviewEventSyncService = reviewEventSyncService;
        this.reviewIncompleteNudgeService = reviewIncompleteNudgeService;
        this.sliSnapshotSyncService = sliSnapshotSyncService;
        this.excludedDowntimeBackfillService = excludedDowntimeBackfillService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("status", "UP");
        map.put("app", "sla-wecom-notifier");
        return map;
    }

    /**
     * 测试 MySQL 数据源连通性。
     */
    @GetMapping("/db-ping")
    public Map<String, Object> dbPing() {
        Map<String, Object> map = new HashMap<String, Object>();
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        map.put("status", Integer.valueOf(1).equals(one) ? "UP" : "DOWN");
        map.put("select1", one);
        map.put("database", "zabbix");
        return map;
    }

    /**
     * 仅查询过滤结果，不推送企微
     */
    @GetMapping("/preview")
    public NotifyResult preview() {
        return notifyService.preview();
    }

    /**
     * 查询并立即推送企业微信
     */
    @PostMapping("/notify")
    public NotifyResult notifyNow() {
        return notifyService.notifyNow();
    }

    /**
     * 手工同步未排除宕机到 sla_review_event；不受 review-sync.enabled 影响。
     * query {@code notify} 默认 false：仅写库。为 true 且本次成功后才发未复盘企微催办。
     * query {@code refreshOwner} 默认 false：INSERT IGNORE 新增。为 true 时只按唯一键更新 owner_name，不插入。
     * query {@code refreshExcludeTime} 默认 false：为 true 时按表内 count_sla 重算 exclude_time，不采集 Grafana、不插入。
     */
    @PostMapping("/review-event-sync")
    public ReviewEventSyncResult reviewEventSync(
            @RequestParam(value = "notify", defaultValue = "false") boolean notify,
            @RequestParam(value = "refreshOwner", defaultValue = "false") boolean refreshOwner,
            @RequestParam(value = "refreshExcludeTime", defaultValue = "false") boolean refreshExcludeTime) {
        ReviewEventSyncResult result = reviewEventSyncService.runNow(refreshOwner, refreshExcludeTime);
        if (!notify) {
            return result;
        }
        try {
            reviewIncompleteNudgeService.nudgeAfterSync();
        } catch (Exception e) {
            log.error("手工同步成功，但 sla_review_event 未复盘催办失败", e);
        }
        return result;
    }

    /**
     * 手工补写已复盘但仍未排除的 sla_excluded_downtime。默认 dryRun=true 只返回清单。
     */
    @PostMapping("/excluded-downtime-backfill")
    public ExcludedDowntimeBackfillResult excludedDowntimeBackfill(
            @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun) {
        return excludedDowntimeBackfillService.run(dryRun);
    }

    /**
     * 手工同步父子 SLA 看板到 sla_sli_snapshot；不受 sli-snapshot-sync.enabled 影响；不发企微。
     */
    @PostMapping("/sli-snapshot-sync")
    public SliSnapshotSyncResult sliSnapshotSync() {
        try {
            return sliSnapshotSyncService.runNow();
        } catch (RuntimeException e) {
            log.error("sla_sli_snapshot 手工同步失败", e);
            SliSnapshotSyncResult failed = new SliSnapshotSyncResult();
            failed.setSuccess(false);
            failed.setError(e.getMessage() == null ? "同步失败" : e.getMessage());
            return failed;
        }
    }

    /**
     * 手工生成周度复盘在线文档并推送企微。
     */
    @PostMapping("/weekly-review-doc")
    public WeeklyReviewResult weeklyReviewDoc() {
        return weeklyReviewDocService.runNow();
    }

    /**
     * MCP 导入腾讯文档成功后，用文档地址推送企微并 @ 责任人。
     */
    @PostMapping("/weekly-review-wecom")
    public WeeklyReviewResult weeklyReviewWecom(@RequestParam("url") String url) {
        return weeklyReviewDocService.notifyWecom(url);
    }

    /**
     * 下载复盘 Excel（双页签）。month 仅用于页签名/文件名，不按宕机开始月过滤。
     */
    @GetMapping("/review-excel")
    public ResponseEntity<byte[]> reviewExcel(
            @RequestParam(value = "month", required = false) String month) {
        String monthLabel = reviewExcelService.resolveMonthLabel(month);
        byte[] body = reviewExcelService.exportWorkbook(monthLabel);
        String filename = reviewExcelService.buildFilename(monthLabel);
        String disposition = "attachment; filename=\"" + asciiFilename(filename)
                + "\"; filename*=UTF-8''" + urlEncode(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(XLSX)
                .body(body);
    }

    private static String asciiFilename(String filename) {
        return filename.replaceAll("[^\\x20-\\x7E]", "_");
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
