package com.sla.notifier.tencent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sla.notifier.config.TencentDocsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TencentDocsClientTest {

    @Test
    void importsWorkbookThroughOfficialV2Flow() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TencentDocsProperties properties = new TencentDocsProperties();
        properties.setBaseUrl("https://docs.qq.com");
        properties.setClientId("client");
        properties.setOpenId("open");
        properties.setAccessToken("token");
        properties.setPollIntervalMs(1);
        properties.setPollMaxAttempts(1);
        TencentDocsClient client = new TencentDocsClient(
                restTemplate, new ObjectMapper(), properties);

        server.expect(once(), requestTo("https://docs.qq.com/openapi/drive/v2/files/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Access-Token", "token"))
                .andExpect(header("Client-Id", "client"))
                .andExpect(header("Open-Id", "open"))
                .andRespond(withSuccess("{\"ret\":0,\"data\":{"
                                + "\"COSPutURL\":\"https://cos.example/upload\","
                                + "\"COSFileKey\":\"temp/file.xlsx\","
                                + "\"CustomHeader\":{\"Content-Type\":"
                                + "\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\"}}}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://cos.example/upload"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess());
        server.expect(once(), requestTo("https://docs.qq.com/openapi/drive/v2/files/async-import"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"ret\":0,\"data\":{\"progressQueryID\":\"progress-1\"}}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(
                        "https://docs.qq.com/openapi/drive/v2/files/import-progress?progressQueryID=progress-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ret\":0,\"data\":{"
                                + "\"ID\":\"file-1\",\"title\":\"复盘明细-20260821\","
                                + "\"url\":\"https://docs.qq.com/sheet/example\",\"progress\":100}}",
                        MediaType.APPLICATION_JSON));

        TencentDocsClient.ImportResult result = client.importWorkbook(
                new byte[]{1, 2, 3}, "复盘明细-20260821.xlsx");

        assertEquals("file-1", result.getFileId());
        assertEquals("复盘明细-20260821", result.getTitle());
        assertEquals("https://docs.qq.com/sheet/example", result.getUrl());
        server.verify();
    }
}
