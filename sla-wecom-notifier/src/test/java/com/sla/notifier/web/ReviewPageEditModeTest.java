package com.sla.notifier.web;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewPageEditModeTest {

    @Test
    void savedRowsStartLockedAndCanBeEditedThenLockedAgain() throws Exception {
        String page = read("src/main/resources/static/review/events.html");

        assertTrue(page.contains("row._editing=blank(row.countSla)"));
        assertTrue(page.contains("v-if=\"!row._editing\""));
        assertTrue(page.contains("@click=\"editRow(row)\">编辑"));
        assertTrue(page.contains(":disabled=\"!row._editing\""));
        assertTrue(page.contains("editRow(row){row._editing=true;}"));
        assertTrue(page.contains("row._dirty=false;row._editing=false"));
        assertTrue(page.contains("persistRow"));
        assertTrue(page.contains("reviewStatus:\"all\""));
        assertTrue(page.contains("countSla:\"all\""));
        assertTrue(page.contains("option value=\"incomplete\">未复盘"));
        assertTrue(page.contains("是否计算SLA"));
        assertTrue(page.contains("option value=\"是\">是"));
        assertTrue(page.contains("option value=\"否\">否"));
        assertTrue(page.contains("p.set(\"reviewStatus\",this.q.reviewStatus)"));
        assertTrue(page.contains("p.set(\"countSla\",this.q.countSla)"));
        assertTrue(page.contains("\"reviewStatus\",\"countSla\",\"timeMode\""));
        assertTrue(page.contains("option value=\"all\">全部时间"));
        assertTrue(page.contains("systemLevel"));
        assertTrue(page.contains("queryFromUrl()"));
        assertFalse(page.contains("exclude_time"));
        assertFalse(page.contains("excludeTime"));
        assertTrue(page.contains("9999-12-31"));
        assertTrue(page.contains("未恢复"));

        String preview = read("src/main/resources/static/review/events-preview.html");
        assertTrue(preview.contains("countSla: \"all\""));
        assertTrue(preview.contains("是否计算SLA"));
        assertTrue(preview.contains("p.has(\"countSla\")"));
        assertTrue(preview.contains("this.q.countSla === \"是\""));
        assertTrue(preview.contains("r.countSla !== this.q.countSla"));
        assertTrue(preview.contains("9999-12-31"));
        assertTrue(preview.contains("未恢复"));
    }

    @Test
    void demoPagesRedirectToMatchingServedPages() throws Exception {
        assertTrue(read("../review-web-demo/index.html").contains(
                "static/review/index.html"));
        assertTrue(read("../review-web-demo/events.html").contains(
                "static/review/events.html"));
        assertFalse(read("../review-web-demo/index.html").contains("exclude_time"));
        assertFalse(read("../review-web-demo/events.html").contains("exclude_time"));
    }

    @Test
    void overviewContainsCountsRanksAndDeepLinks() throws Exception {
        String page = read("src/main/resources/static/review/index.html");
        assertTrue(page.contains("incompleteParentCount"));
        assertTrue(page.contains("incompleteChildCount"));
        assertTrue(page.contains("incompleteByOwner"));
        assertTrue(page.contains("lowSliParents"));
        assertTrue(page.contains("lowMonthlySliParents"));
        assertTrue(page.contains("monthlyGrafanaError"));
        assertTrue(page.contains("monthlySelect"));
        assertTrue(page.contains("monthlyReportYear"));
        assertTrue(page.contains("monthlyCurrentMonth"));
        assertTrue(page.contains(":disabled=\"month.disabled\""));
        assertTrue(page.contains("selectedMonthly.parents"));
        assertTrue(page.contains("-- 无月度数据"));
        assertTrue(page.contains("无低于阈值系统"));
        assertTrue(page.contains("countedDowntimeError"));
        assertTrue(page.contains("countedSlaDowntimes"));
        assertTrue(page.contains("查看宕机明细"));
        assertTrue(page.contains("downtime.html?"));
        assertFalse(page.contains("计入SLA宕机"));
        String detail = read("src/main/resources/static/review/downtime.html");
        assertTrue(detail.contains("子系统/父系统"));
        assertTrue(detail.contains("复盘描述"));
        assertTrue(detail.contains("d.reviewDesc||\"\""));
        assertFalse(detail.contains("d.reviewDesc||\"-\""));
        assertTrue(detail.contains("d.systemName!==q.name"));
        assertTrue(detail.contains("String(q.month).padStart"));
        assertTrue(detail.contains("/api/sla/review-overview"));
        assertTrue(detail.contains("href=\"index.html\""));
        assertTrue(page.contains("title=\"月度父系统\""));
        assertTrue(page.contains("title=\"年度父系统\""));
        int monthlyAt = page.indexOf("title=\"月度父系统\"");
        int annualAt = page.indexOf("title=\"年度父系统\"");
        assertTrue(monthlyAt > 0 && monthlyAt < annualAt);
        assertTrue(page.contains("更多（共"));
        assertTrue(page.contains("timeMode:\"all\""));
        assertTrue(page.contains("href=\"events.html\">去复盘"));
        assertFalse(page.contains("exclude_time"));
        assertFalse(page.contains("excludeTime"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
