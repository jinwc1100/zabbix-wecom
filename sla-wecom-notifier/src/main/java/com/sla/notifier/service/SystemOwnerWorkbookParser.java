package com.sla.notifier.service;

import com.sla.notifier.model.SlaSystemOwner;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 responsible_persons.xlsx：子系统/父系统、系统名称、责任人、手机号码。
 */
public final class SystemOwnerWorkbookParser {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private SystemOwnerWorkbookParser() {
    }

    public static List<SlaSystemOwner> parse(InputStream in) throws IOException {
        Workbook workbook = WorkbookFactory.create(in);
        try {
            Sheet sheet = workbook.getSheetAt(0);
            List<SlaSystemOwner> rows = new ArrayList<SlaSystemOwner>();
            boolean header = true;
            for (Row row : sheet) {
                if (header) {
                    header = false;
                    continue;
                }
                String level = cell(row, 0);
                String name = cell(row, 1);
                if (level.isEmpty() && name.isEmpty()) {
                    continue;
                }
                SlaSystemOwner owner = new SlaSystemOwner();
                owner.setSystemLevel(level);
                owner.setSystemName(name);
                owner.setOwnerName(cell(row, 2));
                owner.setMobile(cell(row, 3));
                rows.add(owner);
            }
            return rows;
        } finally {
            workbook.close();
        }
    }

    private static String cell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }
}
