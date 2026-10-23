package com.sla.notifier.service;

import com.sla.notifier.model.SlaSystemOwner;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class SystemOwnerWorkbookLoader {

    private SystemOwnerWorkbookLoader() {
    }

    public static List<SlaSystemOwner> load(InputStream in) throws IOException {
        Workbook workbook = new XSSFWorkbook(in);
        try {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<SlaSystemOwner> rows = new ArrayList<SlaSystemOwner>();
            boolean header = true;
            for (Row row : sheet) {
                if (header) {
                    header = false;
                    continue;
                }
                String level = cell(formatter, row, 0);
                String name = cell(formatter, row, 1);
                String owner = cell(formatter, row, 2);
                String mobile = cell(formatter, row, 3);
                if (level.isEmpty() || name.isEmpty() || owner.isEmpty()) {
                    continue;
                }
                SlaSystemOwner item = new SlaSystemOwner();
                item.setSystemLevel(level);
                item.setSystemName(name);
                item.setOwnerName(owner);
                item.setMobile(mobile.isEmpty() ? null : mobile);
                rows.add(item);
            }
            return rows;
        } finally {
            workbook.close();
        }
    }

    private static String cell(DataFormatter formatter, Row row, int index) {
        if (row == null || row.getCell(index) == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }
}
