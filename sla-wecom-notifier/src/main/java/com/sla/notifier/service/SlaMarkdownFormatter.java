package com.sla.notifier.service;

import com.sla.notifier.config.GrafanaProperties;
import com.sla.notifier.config.SlaDowntimeProperties;
import com.sla.notifier.model.DowntimeNotifyItem;
import com.sla.notifier.model.NotifyResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SlaMarkdownFormatter {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GrafanaProperties grafanaProperties;
    private final SlaDowntimeProperties downtimeProperties;

    public SlaMarkdownFormatter(GrafanaProperties grafanaProperties, SlaDowntimeProperties downtimeProperties) {
        this.grafanaProperties = grafanaProperties;
        this.downtimeProperties = downtimeProperties;
    }

    /** 完整明细（preview 使用）。 */
    public String format(NotifyResult result) {
        return formatDetail(result);
    }

    public String formatDetail(NotifyResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## SLI < ").append(grafanaProperties.getSliThreshold()).append("% 系统宕机清单\n");
        sb.append("> 时间：").append(LocalDateTime.now().format(TIME_FMT)).append("\n");
        sb.append("> 近窗：最近 ").append(downtimeProperties.getRecentWindowDays()).append(" 天\n");
        sb.append("> 系统数：").append(result.getTotalSystemCount())
                .append("｜近窗宕机记录：").append(result.getTotalRecentDowntimeCount()).append("\n");
        sb.append("> 父系统：").append(result.getParentSystemCount())
                .append("｜子系统：").append(result.getSubsystemCount()).append("\n\n");

        if (result.getItems() == null || result.getItems().isEmpty()) {
            sb.append("当前无低于阈值的系统。\n");
            appendDetailHints(sb);
            return sb.toString();
        }

        Map<String, List<DowntimeNotifyItem>> grouped = resolveGrouped(result);
        appendDetailLevel(sb, "父系统", grouped.get("父系统"));
        appendDetailLevel(sb, "子系统", grouped.get("子系统"));
        appendDetailLevel(sb, "未知", grouped.get("未知"));

        sb.append("请对应责任人及时组织 SLA 复盘。\n");
        appendDetailHints(sb);
        return sb.toString();
    }

    /**
     * 企微推送精简版：系统名称、SLA值、子系统/父系统、监控系统（新版/老版）。
     */
    public String formatWecomSummary(NotifyResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## SLI < ").append(grafanaProperties.getSliThreshold()).append("% 复盘清单\n");
        sb.append("> 时间：").append(LocalDateTime.now().format(TIME_FMT)).append("\n");
        sb.append("> 系统数：").append(result.getTotalSystemCount())
                .append("｜父系统：").append(result.getParentSystemCount())
                .append("｜子系统：").append(result.getSubsystemCount()).append("\n\n");

        if (result.getItems() == null || result.getItems().isEmpty()) {
            sb.append("当前无低于阈值的系统。\n");
            appendDetailHints(sb);
            return sb.toString();
        }

        Map<String, List<DowntimeNotifyItem>> grouped = resolveGrouped(result);
        appendSummaryLevel(sb, "父系统", grouped.get("父系统"));
        appendSummaryLevel(sb, "子系统", grouped.get("子系统"));
        appendSummaryLevel(sb, "未知", grouped.get("未知"));

        sb.append("请对应责任人及时组织 SLA 复盘。\n");
        appendDetailHints(sb);
        return sb.toString();
    }

    private void appendDetailHints(StringBuilder sb) {
        sb.append("\n");
        sb.append("- 子系统宕机明细请查看：**SLA宕机明细**\n");
        sb.append("- 父系统宕机明细请查看：**SLA父系统宕机明细**\n");
    }

    private void appendDetailLevel(StringBuilder sb, String level, List<DowntimeNotifyItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append("### ").append(level).append("（").append(items.size()).append("）\n\n");
        for (DowntimeNotifyItem item : items) {
            sb.append("#### ").append(item.getSystemName())
                    .append("（年度SLI：").append(item.getAnnualSli()).append("）\n");
            sb.append("- 系统层级：").append(item.getSystemLevel()).append("\n");
            if (item.isMonthFullyReviewed()) {
                sb.append("- 本月复盘：已复盘（企微推送将跳过）\n");
            }
            sb.append("- 责任人：").append(item.getOwnerName() == null ? "-" : item.getOwnerName());
            if (item.getOwnerMobile() != null && !item.getOwnerMobile().isEmpty()) {
                sb.append("（").append(item.getOwnerMobile()).append("）");
            }
            sb.append("\n");
            if (item.getRecentPeriods() != null && !item.getRecentPeriods().isEmpty()) {
                int i = 1;
                for (DowntimeNotifyItem.DowntimePeriod p : item.getRecentPeriods()) {
                    sb.append(i++).append(". ")
                            .append(nullToDash(p.getDowntimeStart()))
                            .append(" ~ ")
                            .append(nullToDash(p.getDowntimeEnd()))
                            .append("\n");
                }
            } else {
                sb.append("- 近窗明细：无\n");
            }
            if (item.getRemark() != null && !item.getRemark().trim().isEmpty()) {
                sb.append("> 备注：").append(item.getRemark()).append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendSummaryLevel(StringBuilder sb, String level, List<DowntimeNotifyItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append("### ").append(level).append("（").append(items.size()).append("）\n");
        int i = 1;
        for (DowntimeNotifyItem item : items) {
            sb.append(i++).append(". **").append(item.getSystemName()).append("** — ")
                    .append(item.getAnnualSli() == null ? "-" : item.getAnnualSli())
                    .append(" — ")
                    .append(item.getSystemLevel() == null ? level : item.getSystemLevel())
                    .append(" — ")
                    .append(item.getMonitorSystem() == null || item.getMonitorSystem().trim().isEmpty()
                            ? "-" : item.getMonitorSystem());
            if (item.isMonthFullyReviewed()) {
                sb.append(" — 本月已复盘");
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private Map<String, List<DowntimeNotifyItem>> resolveGrouped(NotifyResult result) {
        Map<String, List<DowntimeNotifyItem>> grouped = result.getGroupedByLevel();
        if (grouped == null || grouped.isEmpty()) {
            return group(result.getItems());
        }
        return grouped;
    }

    private Map<String, List<DowntimeNotifyItem>> group(List<DowntimeNotifyItem> items) {
        Map<String, List<DowntimeNotifyItem>> map = new LinkedHashMap<String, List<DowntimeNotifyItem>>();
        for (DowntimeNotifyItem item : items) {
            String key = item.getSystemLevel() == null ? "未知" : item.getSystemLevel();
            List<DowntimeNotifyItem> list = map.get(key);
            if (list == null) {
                list = new ArrayList<DowntimeNotifyItem>();
                map.put(key, list);
            }
            list.add(item);
        }
        return map;
    }

    private String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
