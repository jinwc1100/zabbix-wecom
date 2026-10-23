package com.sla.notifier.grafana;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sla.notifier.config.GrafanaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grafana 登录、按 uid 拉看板，并通过 /api/ds/query 执行 SQL。
 */
@Component
public class GrafanaClient {

    private static final Logger log = LoggerFactory.getLogger(GrafanaClient.class);

    private final RestTemplate restTemplate;
    private final GrafanaProperties properties;
    private final ObjectMapper objectMapper;

    public GrafanaClient(RestTemplate restTemplate, GrafanaProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Session login() {
        String url = properties.getBaseUrl() + "/login";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("user", properties.getUsername());
        body.put("password", properties.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<String>(body.toString(), headers), String.class);

        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null || setCookies.isEmpty()) {
            throw new IllegalStateException("Grafana 登录成功但未返回 session cookie");
        }

        StringBuilder cookieHeader = new StringBuilder();
        for (String raw : setCookies) {
            String part = raw.split(";", 2)[0];
            if (cookieHeader.length() > 0) {
                cookieHeader.append("; ");
            }
            cookieHeader.append(part);
        }

        log.info("Grafana 登录成功: user={}", properties.getUsername());
        return new Session(cookieHeader.toString());
    }

    public JsonNode fetchDashboard(Session session, String dashboardUid) {
        String url = properties.getBaseUrl() + "/api/dashboards/uid/" + dashboardUid;
        HttpHeaders headers = cookieHeaders(session);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<Void>(headers), String.class);
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("解析 Grafana 看板 JSON 失败: " + dashboardUid, e);
        }
    }

    public PanelQueryMeta resolvePanelMeta(JsonNode dashboardRoot, int panelId) {
        JsonNode panels = dashboardRoot.path("dashboard").path("panels");
        for (JsonNode panel : panels) {
            if (panel.path("id").asInt(-1) == panelId) {
                JsonNode target = panel.path("targets").get(0);
                return new PanelQueryMeta(
                        panel.path("title").asText("panel-" + panelId),
                        target.path("rawSql").asText(),
                        target.path("datasource").path("uid").asText(),
                        target.path("datasource").path("type").asText("mysql")
                );
            }
        }
        throw new IllegalStateException("未找到面板 id=" + panelId);
    }

    public List<Map<String, String>> queryTable(Session session, PanelQueryMeta meta, String rawSql) {
        return queryTable(session, meta, rawSql, "now-1h", "now");
    }

    /**
     * @param from Grafana time range start (e.g. {@code now-1h} or epoch millis string)
     * @param to   Grafana time range end (e.g. {@code now} or epoch millis string)
     */
    public List<Map<String, String>> queryTable(Session session, PanelQueryMeta meta, String rawSql,
                                                String from, String to) {
        ObjectNode query = objectMapper.createObjectNode();
        query.put("refId", "A");
        query.put("format", "table");
        query.put("rawQuery", true);
        query.put("rawSql", rawSql);
        ObjectNode ds = query.putObject("datasource");
        ds.put("type", meta.getDatasourceType());
        ds.put("uid", meta.getDatasourceUid());

        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode queries = payload.putArray("queries");
        queries.add(query);
        payload.put("from", from == null || from.trim().isEmpty() ? "now-1h" : from.trim());
        payload.put("to", to == null || to.trim().isEmpty() ? "now" : to.trim());

        String url = properties.getBaseUrl() + "/api/ds/query";
        HttpHeaders headers = cookieHeaders(session);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<String>(payload.toString(), headers), String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return parseTableFrames(root);
        } catch (Exception e) {
            throw new IllegalStateException("解析面板查询结果失败: " + meta.getTitle(), e);
        }
    }

    public List<Map<String, String>> queryLowSliRows(Session session, PanelQueryMeta meta, double threshold) {
        return queryLowSliRows(session, meta, threshold, "年度SLI");
    }

    public List<Map<String, String>> queryLowSliRows(Session session, PanelQueryMeta meta,
                                                     double threshold, String sliColumn) {
        String col = (sliColumn == null || sliColumn.trim().isEmpty()) ? "年度SLI" : sliColumn.trim();
        String wrappedSql = "SELECT * FROM (\n" + meta.getRawSql() + "\n) AS t WHERE `" + col + "` < "
                + threshold + " ORDER BY `" + col + "` ASC";
        return queryTable(session, meta, wrappedSql);
    }

    public List<Map<String, String>> queryAllRows(Session session, PanelQueryMeta meta) {
        return queryTable(session, meta, meta.getRawSql());
    }

    private List<Map<String, String>> parseTableFrames(JsonNode root) {
        JsonNode frames = root.path("results").path("A").path("frames");
        if (!frames.isArray() || frames.size() == 0) {
            JsonNode error = root.path("results").path("A").path("error");
            if (!error.isMissingNode() && !error.asText().isEmpty()) {
                throw new IllegalStateException("Grafana 查询错误: " + error.asText());
            }
            return Collections.emptyList();
        }

        JsonNode frame = frames.get(0);
        JsonNode fields = frame.path("schema").path("fields");
        JsonNode values = frame.path("data").path("values");
        if (!fields.isArray() || !values.isArray() || values.size() == 0) {
            return Collections.emptyList();
        }

        List<String> columns = new ArrayList<String>();
        for (JsonNode field : fields) {
            columns.add(field.path("name").asText());
        }

        int rowCount = values.get(0).size();
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            Map<String, String> row = new LinkedHashMap<String, String>();
            for (int c = 0; c < columns.size(); c++) {
                JsonNode cell = values.get(c).get(i);
                row.put(columns.get(c), cell == null || cell.isNull() ? null : cell.asText());
            }
            rows.add(row);
        }
        return rows;
    }

    private HttpHeaders cookieHeaders(Session session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session.getCookieHeader());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    public static class Session {
        private final String cookieHeader;

        public Session(String cookieHeader) {
            this.cookieHeader = cookieHeader;
        }

        public String getCookieHeader() {
            return cookieHeader;
        }
    }

    public static class PanelQueryMeta {
        private final String title;
        private final String rawSql;
        private final String datasourceUid;
        private final String datasourceType;

        public PanelQueryMeta(String title, String rawSql, String datasourceUid, String datasourceType) {
            this.title = title;
            this.rawSql = rawSql;
            this.datasourceUid = datasourceUid;
            this.datasourceType = datasourceType;
        }

        public String getTitle() {
            return title;
        }

        public String getRawSql() {
            return rawSql;
        }

        public String getDatasourceUid() {
            return datasourceUid;
        }

        public String getDatasourceType() {
            return datasourceType;
        }
    }
}
