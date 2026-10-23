package com.sla.notifier.service;

import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.model.WeeklyReviewRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WeeklyReviewTemplateServiceTest {

    @Test
    void fillsFiveColumnsInActualTemplateAndPreservesSheets() throws Exception {
        WeeklyReviewProperties properties = new WeeklyReviewProperties();
        properties.setTemplatePath("doc/复盘明细.xlsx");
        WeeklyReviewTemplateService service = new WeeklyReviewTemplateService(properties);

        byte[] result = service.fillTemplate(Collections.singletonList(
                new WeeklyReviewRow("子系统", "测试系统",
                        "2026-08-21 10:00:00", "2026-08-21 10:05:00",
                        "测试责任人", "13800000000", true)));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertNotNull(workbook.getSheet("下拉选项"));
            assertNotNull(workbook.getSheet("故障说明"));
            Sheet sheet = workbook.getSheet("故障录入");
            assertNotNull(sheet);
            Header header = findHeader(sheet);
            Row data = sheet.getRow(header.rowIndex + 1);
            assertEquals("子系统", text(data.getCell(header.levelColumn)));
            assertEquals("测试系统", text(data.getCell(header.nameColumn)));
            assertEquals("2026-08-21 10:00:00", text(data.getCell(header.startColumn)));
            assertEquals("2026-08-21 10:05:00", text(data.getCell(header.endColumn)));
            assertEquals("测试责任人", text(data.getCell(header.ownerColumn)));
        }
    }

    private Header findHeader(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        for (Row row : sheet) {
            Header header = new Header();
            header.rowIndex = row.getRowNum();
            for (Cell cell : row) {
                String value = formatter.formatCellValue(cell).trim();
                if ("父/子系统".equals(value)) {
                    header.levelColumn = cell.getColumnIndex();
                } else if ("系统名称".equals(value)) {
                    header.nameColumn = cell.getColumnIndex();
                } else if ("宕机开始时间".equals(value)) {
                    header.startColumn = cell.getColumnIndex();
                } else if ("宕机结束时间".equals(value)) {
                    header.endColumn = cell.getColumnIndex();
                } else if ("责任人".equals(value)) {
                    header.ownerColumn = cell.getColumnIndex();
                }
            }
            if (header.complete()) {
                return header;
            }
        }
        throw new AssertionError("未找到表头");
    }

    private String text(Cell cell) {
        return new DataFormatter().formatCellValue(cell);
    }

    private static class Header {
        private int rowIndex = -1;
        private int levelColumn = -1;
        private int nameColumn = -1;
        private int startColumn = -1;
        private int endColumn = -1;
        private int ownerColumn = -1;

        private boolean complete() {
            return levelColumn >= 0 && nameColumn >= 0 && startColumn >= 0
                    && endColumn >= 0 && ownerColumn >= 0;
        }
    }
}
