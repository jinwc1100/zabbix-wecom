package com.sla.notifier.service;

import com.sla.notifier.config.WeeklyReviewProperties;
import com.sla.notifier.model.WeeklyReviewRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeeklyReviewTemplateService {

    private static final String[] REQUIRED_HEADERS = {
            "父/子系统", "系统名称", "宕机开始时间", "宕机结束时间", "责任人"
    };

    private final WeeklyReviewProperties properties;

    public WeeklyReviewTemplateService(WeeklyReviewProperties properties) {
        this.properties = properties;
    }

    public byte[] fillTemplate(List<WeeklyReviewRow> dataRows) {
        Path template = Paths.get(properties.getTemplatePath());
        if (!Files.isRegularFile(template)) {
            throw new IllegalStateException("复盘模版不存在: " + template.toAbsolutePath());
        }
        try (InputStream input = Files.newInputStream(template);
             Workbook workbook = new XSSFWorkbook(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            HeaderLocation location = findHeaders(workbook);
            clearTargetColumns(location);
            writeRows(location, dataRows);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("读取或填写复盘模版失败: " + template.toAbsolutePath(), e);
        }
    }

    private HeaderLocation findHeaders(Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            for (Row row : sheet) {
                Map<String, Integer> columns = new LinkedHashMap<String, Integer>();
                for (Cell cell : row) {
                    String value = formatter.formatCellValue(cell).trim();
                    for (String header : REQUIRED_HEADERS) {
                        if (header.equals(value)) {
                            columns.put(header, cell.getColumnIndex());
                        }
                    }
                }
                if (columns.size() == REQUIRED_HEADERS.length) {
                    return new HeaderLocation(sheet, row.getRowNum(), columns);
                }
            }
        }
        throw new IllegalStateException("复盘模版未找到必需表头: 父/子系统、系统名称、宕机开始时间、宕机结束时间、责任人");
    }

    private void clearTargetColumns(HeaderLocation location) {
        Sheet sheet = location.sheet;
        for (int rowIndex = location.headerRow + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (Integer column : location.columns.values()) {
                Cell cell = row.getCell(column);
                if (cell != null) {
                    cell.setBlank();
                }
            }
        }
    }

    private void writeRows(HeaderLocation location, List<WeeklyReviewRow> dataRows) {
        if (dataRows == null) {
            return;
        }
        Sheet sheet = location.sheet;
        int templateRowIndex = location.headerRow + 1;
        Row templateRow = sheet.getRow(templateRowIndex);
        int rowIndex = templateRowIndex;
        for (WeeklyReviewRow item : dataRows) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
                copyRowPresentation(templateRow, row);
            }
            setCell(row, location.columns.get("父/子系统"), item.getSystemLevel(), templateRow);
            setCell(row, location.columns.get("系统名称"), item.getSystemName(), templateRow);
            setCell(row, location.columns.get("宕机开始时间"), item.getDowntimeStart(), templateRow);
            setCell(row, location.columns.get("宕机结束时间"), item.getDowntimeEnd(), templateRow);
            setCell(row, location.columns.get("责任人"), item.getOwnerName(), templateRow);
            rowIndex++;
        }
    }

    private void setCell(Row row, int column, String value, Row templateRow) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
            if (templateRow != null) {
                Cell templateCell = templateRow.getCell(column);
                if (templateCell != null) {
                    CellStyle style = templateCell.getCellStyle();
                    if (style != null) {
                        cell.setCellStyle(style);
                    }
                }
            }
        }
        cell.setCellValue(value == null ? "" : value);
    }

    private void copyRowPresentation(Row source, Row target) {
        if (source != null) {
            target.setHeight(source.getHeight());
        }
    }

    private static class HeaderLocation {
        private final Sheet sheet;
        private final int headerRow;
        private final Map<String, Integer> columns;

        private HeaderLocation(Sheet sheet, int headerRow, Map<String, Integer> columns) {
            this.sheet = sheet;
            this.headerRow = headerRow;
            this.columns = columns;
        }
    }
}
