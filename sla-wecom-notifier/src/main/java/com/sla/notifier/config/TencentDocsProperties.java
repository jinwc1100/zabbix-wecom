package com.sla.notifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tencent-docs")
public class TencentDocsProperties {

    /**
     * mcp：进程只落盘 xlsx，由 Cursor 腾讯文档 MCP 导入；
     * openapi：进程内调用腾讯文档开放 API 导入。
     */
    private String mode = "mcp";

    private String baseUrl = "https://docs.qq.com";
    private String clientId;
    private String clientSecret;
    private String openId;
    private String accessToken;
    private String refreshToken;
    private long pollIntervalMs = 1000L;
    private int pollMaxAttempts = 120;

    public boolean isMcpMode() {
        return mode == null || "mcp".equalsIgnoreCase(mode.trim());
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getPollMaxAttempts() {
        return pollMaxAttempts;
    }

    public void setPollMaxAttempts(int pollMaxAttempts) {
        this.pollMaxAttempts = pollMaxAttempts;
    }
}
