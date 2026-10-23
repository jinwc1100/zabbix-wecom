package com.sla.notifier.tencent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sla.notifier.config.TencentDocsProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;

@Component
public class TencentDocsClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TencentDocsProperties properties;

    private volatile String currentAccessToken;
    private volatile String currentOpenId;

    public TencentDocsClient(RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             TencentDocsProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.currentAccessToken = trim(properties.getAccessToken());
        this.currentOpenId = trim(properties.getOpenId());
    }

    public ImportResult importWorkbook(byte[] workbook, String fileName) {
        if (workbook == null || workbook.length == 0) {
            throw new IllegalArgumentException("待导入 workbook 不能为空");
        }
        requireText(fileName, "fileName");
        ensureAccessToken();

        String md5 = md5Hex(workbook);
        JsonNode preImport = postForm("/openapi/drive/v2/files/upload", form(
                "fileMD5", md5,
                "fileName", fileName,
                "fileSize", String.valueOf(workbook.length)));
        JsonNode preData = successData(preImport, "腾讯文档预导入失败");
        String uploadUrl = requiredNodeText(preData, "COSPutURL", "预导入未返回 COSPutURL");
        String cosFileKey = requiredNodeText(preData, "COSFileKey", "预导入未返回 COSFileKey");

        putToCos(uploadUrl, workbook, preData.path("CustomHeader"));

        JsonNode asyncImport = postForm("/openapi/drive/v2/files/async-import", form(
                "fileMD5", md5,
                "fileName", fileName,
                "COSFileKey", cosFileKey));
        String progressId = requiredNodeText(
                successData(asyncImport, "腾讯文档异步导入启动失败"),
                "progressQueryID", "异步导入未返回 progressQueryID");

        return pollImport(progressId);
    }

    private ImportResult pollImport(String progressId) {
        int attempts = Math.max(1, properties.getPollMaxAttempts());
        for (int attempt = 0; attempt < attempts; attempt++) {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(baseUrl() + "/openapi/drive/v2/files/import-progress")
                    .queryParam("progressQueryID", progressId)
                    .build(true)
                    .toUri();
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<Void>(authHeaders()), String.class);
            JsonNode data = successData(parseBody(response.getBody()), "查询腾讯文档导入进度失败");
            int progress = data.path("progress").asInt(0);
            if (progress >= 100) {
                return new ImportResult(
                        requiredNodeText(data, "ID", "导入完成但未返回文档 ID"),
                        data.path("title").asText(""),
                        requiredNodeText(data, "url", "导入完成但未返回文档 URL"));
            }
            sleep(properties.getPollIntervalMs());
        }
        throw new IllegalStateException("腾讯文档导入超时，progressQueryID=" + progressId);
    }

    private JsonNode postForm(String path, MultiValueMap<String, String> form) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + path, new HttpEntity<MultiValueMap<String, String>>(form, headers),
                String.class);
        return parseBody(response.getBody());
    }

    private void putToCos(String uploadUrl, byte[] workbook, JsonNode customHeaders) {
        HttpHeaders headers = new HttpHeaders();
        if (customHeaders != null && customHeaders.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = customHeaders.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                headers.set(field.getKey(), field.getValue().asText());
            }
        }
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
        restTemplate.exchange(uploadUrl, HttpMethod.PUT,
                new HttpEntity<byte[]>(workbook, headers), String.class);
    }

    private HttpHeaders authHeaders() {
        requireText(properties.getClientId(), "tencent-docs.client-id");
        requireText(currentAccessToken, "tencent-docs.access-token");
        requireText(currentOpenId, "tencent-docs.open-id");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Token", currentAccessToken);
        headers.set("Client-Id", properties.getClientId().trim());
        headers.set("Open-Id", currentOpenId);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private synchronized void ensureAccessToken() {
        requireText(properties.getClientId(), "tencent-docs.client-id");
        if (hasText(properties.getRefreshToken())
                && hasText(properties.getClientSecret())) {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl() + "/oauth/v2/token")
                    .queryParam("client_id", properties.getClientId())
                    .queryParam("client_secret", properties.getClientSecret())
                    .queryParam("grant_type", "refresh_token")
                    .queryParam("refresh_token", properties.getRefreshToken())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            JsonNode root = parseBody(response.getBody());
            currentAccessToken = requiredNodeText(root, "access_token",
                    "刷新腾讯文档 token 未返回 access_token");
            String userId = root.path("user_id").asText("");
            if (hasText(userId)) {
                currentOpenId = userId.trim();
            }
        }
        requireText(currentAccessToken,
                "tencent-docs.access-token（或 client-secret + refresh-token）");
        requireText(currentOpenId, "tencent-docs.open-id");
    }

    private JsonNode successData(JsonNode root, String message) {
        if (root.path("ret").asInt(-1) != 0) {
            throw new IllegalStateException(message + ": " + root.toString());
        }
        return root.path("data");
    }

    private JsonNode parseBody(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("解析腾讯文档响应失败: " + body, e);
        }
    }

    private String baseUrl() {
        String value = trim(properties.getBaseUrl());
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static MultiValueMap<String, String> form(String... values) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        for (int i = 0; i < values.length; i += 2) {
            form.add(values[i], values[i + 1]);
        }
        return form;
    }

    private static String requiredNodeText(JsonNode node, String field, String message) {
        String value = node.path(field).asText("");
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private static String md5Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(bytes);
            StringBuilder result = new StringBuilder(32);
            for (byte value : hash) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算文件 MD5 失败", e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(Math.max(1L, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待腾讯文档导入时线程被中断", e);
        }
    }

    private static void requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalStateException(field + " 未配置");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static class ImportResult {
        private final String fileId;
        private final String title;
        private final String url;

        public ImportResult(String fileId, String title, String url) {
            this.fileId = fileId;
            this.title = title;
            this.url = url;
        }

        public String getFileId() {
            return fileId;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }
}
