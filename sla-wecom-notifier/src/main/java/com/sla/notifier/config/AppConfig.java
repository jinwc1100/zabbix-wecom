package com.sla.notifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(GrafanaProperties grafanaProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(grafanaProperties.getConnectTimeoutMs());
        factory.setReadTimeout(grafanaProperties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }
}
