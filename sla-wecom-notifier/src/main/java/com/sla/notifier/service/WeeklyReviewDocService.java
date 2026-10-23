package com.sla.notifier.service;

import com.sla.notifier.config.TencentDocsProperties;
import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.model.WeeklyReviewResult;
import com.sla.notifier.model.WeeklyReviewRow;
import com.sla.notifier.tencent.TencentDocsClient;
import com.sla.notifier.util.WeeklyReviewNaming;
import com.sla.notifier.wecom.WeComWebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class WeeklyReviewDocService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReviewDocService.class);
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WeeklyReviewProperties properties;
    private final TencentDocsProperties tencentDocsProperties;
    private final WeeklyReviewDataService dataService;
    private final WeeklyReviewTemplateService templateService;
    private final TencentDocsClient tencentDocsClient;
    private final WeComWebhookClient weComWebhookClient;

    private volatile WeeklyReviewResult lastPrepared;

    public WeeklyReviewDocService(WeeklyReviewProperties properties,
                                  TencentDocsProperties tencentDocsProperties,
                                  WeeklyReviewDataService dataService,
                                  WeeklyReviewTemplateService templateService,
                                  TencentDocsClient tencentDocsClient,
                                  WeComWebhookClient weComWebhookClient) {
        this.properties = properties;
        this.tencentDocsProperties = tencentDocsProperties;
        this.dataService = dataService;
        this.templateService = templateService;
        this.tencentDocsClient = tencentDocsClient;
        this.weComWebhookClient = weComWebhookClient;
    }

    public WeeklyReviewResult runNow() {
        return run(LocalDateTime.now(resolveZone()));
    }

    public WeeklyReviewResult run(LocalDateTime triggerTime) {
        List<WeeklyReviewRow> rows = dataService.collect(triggerTime);
        byte[] workbook = templateService.fillTemplate(rows);
        String documentTitle = WeeklyReviewNaming.documentTitle(triggerTime);
        String filename = WeeklyReviewNaming.workbookFilename(triggerTime);

        Set<String> ownerNames = new LinkedHashSet<String>();
        Set<String> ownerMobiles = new LinkedHashSet<String>();
        for (WeeklyReviewRow row : rows) {
            if (row.isOwnerMatched()) {
                addNonBlank(ownerNames, row.getOwnerName());
                addNonBlank(ownerMobiles, row.getOwnerMobile());
            }
        }

        Path localFile = writeLocalWorkbook(workbook, filename);
        WeeklyReviewResult result = new WeeklyReviewResult();
        result.setSuccess(true);
        result.setRowCount(rows.size());
        result.setDocumentTitle(documentTitle);
        result.setLocalPath(localFile.toAbsolutePath().toString());
        result.setTriggerTime(DISPLAY.format(triggerTime));
        result.setOwnerNames(new ArrayList<String>(ownerNames));
        result.setOwnerMobiles(new ArrayList<String>(ownerMobiles));
        this.lastPrepared = result;

        if (tencentDocsProperties.isMcpMode()) {
            result.setMessage("已生成复盘 xlsx（MCP 模式），请导入腾讯文档后调用 "
                    + "POST /api/sla/weekly-review-wecom?url=文档地址；明细 "
                    + rows.size() + " 条");
            log.info("{}，localPath={}", result.getMessage(), result.getLocalPath());
            return result;
        }

        TencentDocsClient.ImportResult imported = tencentDocsClient.importWorkbook(workbook, filename);
        return notifyWecom(imported.getUrl());
    }

    public WeeklyReviewResult notifyWecom(String documentUrl) {
        WeeklyReviewResult prepared = this.lastPrepared;
        if (prepared == null) {
            throw new IllegalStateException("请先调用 POST /api/sla/weekly-review-doc 生成复盘文件");
        }
        if (documentUrl == null || documentUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("文档 url 不能为空");
        }
        String url = documentUrl.trim();
        LocalDateTime triggerTime = LocalDateTime.parse(prepared.getTriggerTime(), DISPLAY);
        String markdown = buildMarkdown(triggerTime, prepared.getRowCount(),
                prepared.getDocumentTitle(), url);
        weComWebhookClient.sendMarkdown(markdown, prepared.getOwnerNames(), prepared.getOwnerMobiles());
        prepared.setDocumentUrl(url);
        prepared.setSuccess(true);
        prepared.setMessage("周度复盘文档已推送企微，明细 " + prepared.getRowCount()
                + " 条，@" + prepared.getOwnerMobiles().size() + " 人");
        log.info("{}，url={}", prepared.getMessage(), url);
        return prepared;
    }

    private Path writeLocalWorkbook(byte[] workbook, String filename) {
        try {
            Path dir = Paths.get(properties.getOutputDir() == null
                    ? "target/weekly-review" : properties.getOutputDir());
            Files.createDirectories(dir);
            Path file = dir.resolve(filename);
            Files.write(file, workbook);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("写入本地复盘 xlsx 失败", e);
        }
    }

    private String buildMarkdown(LocalDateTime triggerTime, int rowCount,
                                 String title, String documentUrl) {
        return "## SLA 宕机复盘提醒\n"
                + "> 数据窗口：" + properties.getWindowStart()
                + " ～ " + DISPLAY.format(triggerTime) + "\n"
                + "> 待复盘明细：" + rowCount + " 条\n\n"
                + "请相关责任人打开在线文档完成复盘：\n"
                + "[" + title + "](" + documentUrl + ")";
    }

    private ZoneId resolveZone() {
        String zone = properties.getZone();
        return ZoneId.of(zone == null || zone.trim().isEmpty()
                ? "Asia/Shanghai" : zone.trim());
    }

    private static void addNonBlank(Set<String> values, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.add(value.trim());
        }
    }
}
