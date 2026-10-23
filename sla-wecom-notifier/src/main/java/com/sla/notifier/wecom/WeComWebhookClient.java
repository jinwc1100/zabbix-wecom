package com.sla.notifier.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sla.notifier.config.WeComProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class WeComWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(WeComWebhookClient.class);
    private static final int MAX_MARKDOWN_BYTES = 3500;

    private final RestTemplate restTemplate;
    private final WeComProperties properties;
    private final ObjectMapper objectMapper;

    public WeComWebhookClient(RestTemplate restTemplate, WeComProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void sendMarkdown(String markdownContent) {
        sendMarkdown(markdownContent, null, null);
    }

    /**
     * @param ownerNames   责任人姓名（用于文案）
     * @param ownerMobiles 责任人手机（用于 @，会与配置中的静态列表合并去重）
     */
    public void sendMarkdown(String markdownContent, Collection<String> ownerNames, Collection<String> ownerMobiles) {
        if (!properties.isEnabled()) {
            log.warn("企业微信推送已禁用(wecom.enabled=false)，跳过发送");
            return;
        }
        String webhookUrl = properties.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || webhookUrl.contains("YOUR_KEY")) {
            throw new IllegalStateException("请先在 application.yml 配置有效的 wecom.webhook-url");
        }

        List<String> parts = splitMarkdown(markdownContent, MAX_MARKDOWN_BYTES);
        int index = 1;
        for (String part : parts) {
            String content = part;
            if (parts.size() > 1) {
                content = "> （第 " + index + "/" + parts.size() + " 段）\n" + part;
            }
            postMarkdown(webhookUrl, content);
            index++;
        }
        log.info("企业微信 Markdown 推送成功，共 {} 段", parts.size());

        Set<String> mobiles = new LinkedHashSet<String>();
        if (ownerMobiles != null) {
            for (String m : ownerMobiles) {
                if (m != null && !m.trim().isEmpty()) {
                    mobiles.add(m.trim());
                }
            }
        }
        if (properties.getMentionedMobileList() != null) {
            for (String m : properties.getMentionedMobileList()) {
                if (m != null && !m.trim().isEmpty()) {
                    mobiles.add(m.trim());
                }
            }
        }
        List<String> userIds = properties.getMentionedList() == null
                ? new ArrayList<String>() : properties.getMentionedList();

        if (!mobiles.isEmpty() || (userIds != null && !userIds.isEmpty())) {
            StringBuilder text = new StringBuilder("请以下责任人关注上方 SLA 复盘清单并及时组织复盘");
            if (ownerNames != null && !ownerNames.isEmpty()) {
                text.append("：");
                boolean first = true;
                Set<String> names = new LinkedHashSet<String>();
                for (String n : ownerNames) {
                    if (n != null && !n.trim().isEmpty()) {
                        names.add(n.trim());
                    }
                }
                for (String n : names) {
                    if (!first) {
                        text.append("、");
                    }
                    text.append(n);
                    first = false;
                }
            }
            text.append("。");
            sendMentionText(text.toString(), userIds, new ArrayList<String>(mobiles));
        }
    }

    private void postMarkdown(String webhookUrl, String markdownContent) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("msgtype", "markdown");
        ObjectNode markdown = root.putObject("markdown");
        markdown.put("content", markdownContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl, new HttpEntity<String>(root.toString(), headers), String.class);

        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            int errcode = body.path("errcode").asInt(-1);
            if (errcode != 0) {
                throw new IllegalStateException("企业微信返回错误: " + response.getBody());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析企业微信响应失败: " + response.getBody(), e);
        }
    }

    static List<String> splitMarkdown(String content, int maxBytes) {
        List<String> parts = new ArrayList<String>();
        if (content == null || content.isEmpty()) {
            parts.add("");
            return parts;
        }
        if (content.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            parts.add(content);
            return parts;
        }

        String[] lines = content.split("\n", -1);
        StringBuilder buf = new StringBuilder();
        for (String line : lines) {
            String candidate = buf.length() == 0 ? line : buf.toString() + "\n" + line;
            if (candidate.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
                if (buf.length() > 0) {
                    parts.add(buf.toString());
                    buf.setLength(0);
                }
                if (line.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
                    int offset = 0;
                    while (offset < line.length()) {
                        int end = offset;
                        while (end < line.length()
                                && line.substring(offset, end + 1).getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
                            end++;
                        }
                        if (end == offset) {
                            end = offset + 1;
                        }
                        parts.add(line.substring(offset, end));
                        offset = end;
                    }
                } else {
                    buf.append(line);
                }
            } else {
                if (buf.length() > 0) {
                    buf.append('\n');
                }
                buf.append(line);
            }
        }
        if (buf.length() > 0) {
            parts.add(buf.toString());
        }
        return parts;
    }

    private void sendMentionText(String content, List<String> userIds, List<String> mobiles) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("msgtype", "text");
        ObjectNode text = root.putObject("text");
        text.put("content", content);
        ArrayNode mentioned = text.putArray("mentioned_list");
        if (userIds != null) {
            for (String userId : userIds) {
                mentioned.add(userId);
            }
        }
        ArrayNode mobileArr = text.putArray("mentioned_mobile_list");
        if (mobiles != null) {
            for (String mobile : mobiles) {
                mobileArr.add(mobile);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                properties.getWebhookUrl(), new HttpEntity<String>(root.toString(), headers), String.class);
        log.info("企业微信 @提醒 推送完成: mobiles={}, body={}", mobiles, response.getBody());
    }
}
