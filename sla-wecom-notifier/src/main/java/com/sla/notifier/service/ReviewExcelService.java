package com.sla.notifier.service;

import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.grafana.GrafanaClient;
import com.sla.notifier.model.DowntimeEvent;
import com.sla.notifier.model.SlaLowSliItem;
import com.sla.notifier.model.SlaSystemOwner;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReviewExcelService {

    private static final Logger log = LoggerFactory.getLogger(ReviewExcelService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String[] HEADERS = {
            "系统名称", "系统类别", "中断原因", "宕机开始时间", "宕机结束时间", "责任人"
    };

    private final GrafanaClient grafanaClient;
    private final GrafanaProperties grafanaProperties;
    private final AnnualSliService annualSliService;
    private final DowntimeQueryService downtimeQueryService;
    private final OwnerRuleResolver ownerRuleResolver;

    public ReviewExcelService(GrafanaClient grafanaClient,
                              GrafanaProperties grafanaProperties,
                              AnnualSliService annualSliService,
                              DowntimeQueryService downtimeQueryService,
                              OwnerRuleResolver ownerRuleResolver) {
        this.grafanaClient = grafanaClient;
        this.grafanaProperties = grafanaProperties;
        this.annualSliService = annualSliService;
        this.downtimeQueryService = downtimeQueryService;
        this.ownerRuleResolver = ownerRuleResolver;
    }

    public byte[] exportWorkbook(String monthLabel) {
        String month = resolveMonthLabel(monthLabel);
        GrafanaClient.Session session = grafanaClient.login();
        List<SlaLowSliItem> inventory = annualSliService.collectAllInventoryItems(session);
        Map<String, List<DowntimeEvent>> leafIndex = downtimeQueryService.loadLeafIndex(session);
        Map<String, List<DowntimeEvent>> parentIndex = downtimeQueryService.loadParentIndex(session);

        double threshold = grafanaProperties.getSliThreshold();
        List<SlaLowSliItem> lowSli = new ArrayList<SlaLowSliItem>();
        for (SlaLowSliItem item : inventory) {
            if (item.getAnnualSliValue() < threshold) {
                lowSli.add(item);
            }
        }

        List<String[]> sheet1Rows = expandRows(lowSli, leafIndex, parentIndex);
        List<String[]> sheet2Rows = expandRows(inventory, leafIndex, parentIndex);
        log.info("复盘 Excel month={} 低SLI系统={} 全量系统={} sheet1行={} sheet2行={}",
                month, lowSli.size(), inventory.size(), sheet1Rows.size(), sheet2Rows.size());

        String sheet1Name = month + "-SLA小于99.9%待复盘";
        String sheet2Name = month + "-所有待复盘";
        try {
            return writeXlsx(sheet1Name, sheet1Rows, sheet2Name, sheet2Rows);
        } catch (IOException e) {
            throw new IllegalStateException("生成复盘 Excel 失败", e);
        }
    }

    public String resolveMonthLabel(String monthLabel) {
        if (monthLabel == null || monthLabel.trim().isEmpty()) {
            return YearMonth.from(LocalDate.now(ZONE)).format(MONTH_FMT);
        }
        String trimmed = monthLabel.trim();
        try {
            return YearMonth.parse(trimmed, MONTH_FMT).format(MONTH_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("month 须为 yyyy-MM，例如 2026-07");
        }
    }

    public String buildFilename(String monthLabel) {
        return "SLA复盘-" + resolveMonthLabel(monthLabel) + ".xlsx";
    }

    private List<String[]> expandRows(List<SlaLowSliItem> systems,
                                      Map<String, List<DowntimeEvent>> leafIndex,
                                      Map<String, List<DowntimeEvent>> parentIndex) {
        List<String[]> rows = new ArrayList<String[]>();
        Map<String, SlaSystemOwner> owners = ownerRuleResolver.loadSnapshot();
        for (SlaLowSliItem item : systems) {
            String name = item.getSystemName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            String level = item.getSourceLevel();
            List<DowntimeEvent> events;
            if ("父系统".equals(level)) {
                events = parentIndex.get(name);
            } else {
                events = leafIndex.get(name);
            }
            if (events == null || events.isEmpty()) {
                continue;
            }
            String category = level == null ? "" : level;
            String owner = ownerRuleResolver.resolve(level, name, owners).getName();
            for (DowntimeEvent event : events) {
                if (!isUnexcluded(event.getExclusionStatus())) {
                    continue;
                }
                rows.add(new String[]{
                        name,
                        category,
                        "",
                        nullToEmpty(event.getStartDisplay()),
                        nullToEmpty(event.getEndDisplay()),
                        owner == null ? "" : owner
                });
            }
        }
        return rows;
    }

    static boolean isUnexcluded(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim();
        return "未排除".equals(s) || "-".equals(s);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private byte[] writeXlsx(String sheet1Name, List<String[]> sheet1Rows,
                             String sheet2Name, List<String[]> sheet2Rows) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        try {
            writeSheet(workbook, sheet1Name, sheet1Rows);
            writeSheet(workbook, sheet2Name, sheet2Rows);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.close();
        }
    }

    private void writeSheet(Workbook workbook, String sheetName, List<String[]> dataRows) {
        Sheet sheet = workbook.createSheet(sanitizeSheetName(sheetName));
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int r = 0; r < dataRows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            String[] values = dataRows.get(r);
            for (int c = 0; c < HEADERS.length; c++) {
                row.createCell(c).setCellValue(c < values.length ? values[c] : "");
            }
        }
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /** Excel 页签名最长 31 字符，且不可含 \\ / ? * [ ] */
    private static String sanitizeSheetName(String name) {
        String cleaned = name.replace('\\', '_').replace('/', '_')
                .replace('?', '_').replace('*', '_')
                .replace('[', '_').replace(']', '_');
        if (cleaned.length() > 31) {
            return cleaned.substring(0, 31);
        }
        return cleaned;
    }
}
