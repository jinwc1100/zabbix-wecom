package com.sla.notifier.web;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewPageBatchApplySaveTest {

    @Test
    void applyUsesFirstCompleteTemplateOntoCheckedTargetsWithoutSaveApi() throws Exception {
        String page = read("src/main/resources/static/review/events.html");
        String preview = read("src/main/resources/static/review/events-preview.html");

        for (String html : new String[]{page, preview}) {
            assertTrue(html.contains("applyTemplate"));
            assertTrue(html.contains("resolveTemplate"));
            assertTrue(html.contains("设为样板"));
            assertTrue(html.contains("套用样板"));
            assertTrue(html.contains("toggleSelectAll"));
            assertTrue(html.contains("_checked"));
            assertTrue(html.contains("请先填写样板行的全部必填项"));
            assertTrue(html.contains("r._checked&&r.id!==tpl.id")
                    || html.contains("r._checked && r.id !== tpl.id"));
            assertTrue(html.contains("部分勾选行已有填写内容，确定覆盖？"));
            assertTrue(html.contains("当前样板"));
            assertTrue(html.contains("btn-tpl"));
            assertTrue(html.contains("#e8f4ff"));
            assertTrue(html.contains("tr.tpl"));
            assertTrue(html.contains("templateName"));
            assertFalse(html.contains("inset 4px 0 0"));
            assertFalse(html.contains("sla_excluded_downtime"));
            assertFalse(html.contains("templateRowId===row.id?'btn-primary'"));
            assertTrue(html.indexOf("applyTemplate") < html.indexOf("saveChecked")
                    || html.contains("function applyTemplate")
                    || html.contains("applyTemplate(){")
                    || html.contains("applyTemplate() {"));
        }

        assertTrue(page.contains("applyTemplate(){const tpl=this.resolveTemplate()"));
        assertTrue(page.contains("targets.forEach(r=>{r.faultCategory=tpl.faultCategory"));
        int applyAt = page.indexOf("applyTemplate(){");
        int applyEnd = page.indexOf("},", applyAt);
        assertTrue(applyAt > 0 && applyEnd > applyAt);
        assertFalse(page.substring(applyAt, applyEnd).contains("fetch("));
    }

    @Test
    void batchSaveOnlyPersistsCompleteCheckedRowsAndContinuesAfterFailure() throws Exception {
        String page = read("src/main/resources/static/review/events.html");
        String preview = read("src/main/resources/static/review/events-preview.html");

        for (String html : new String[]{page, preview}) {
            assertTrue(html.contains("saveChecked"));
            assertTrue(html.contains("保存勾选"));
            assertTrue(html.contains("persistRow"));
            assertTrue(html.contains("跳过未填完"));
            assertTrue(html.contains("if(!row._checked)continue")
                    || html.contains("if (!row._checked) continue"));
            assertTrue(html.contains("if(!this.isComplete(row)){skip++")
                    || html.contains("if (!this.isComplete(row)) { skip++"));
            assertTrue(html.contains("fails.push"));
            assertTrue(html.contains("for(const row of this.rows)")
                    || html.contains("for (const row of this.rows)"));
        }

        assertTrue(page.contains("await this.persistRow(row)"));
        assertTrue(page.contains("fetch(\"/api/sla/review-events/\"+row.id"));
        assertTrue(page.contains("method:\"PUT\""));
        int persistAt = page.indexOf("async persistRow(row)");
        int saveCheckedAt = page.indexOf("async saveChecked()");
        assertTrue(persistAt > 0 && saveCheckedAt > persistAt);
        String saveCheckedBody = page.substring(saveCheckedAt);
        assertTrue(saveCheckedBody.contains("catch(e){fails.push"));
        assertTrue(saveCheckedBody.contains("ok++"));
        assertTrue(saveCheckedBody.contains("skip++"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
