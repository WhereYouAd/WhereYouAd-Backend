package com.whereyouad.WhereYouAd.global.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIFeignConfig {
    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    public RequestInterceptor openAiAuthInterceptor() {
        return requestTemplate -> {
            // 1. Authorization 헤더 추가 (Bearer + API KEY)
            requestTemplate.header("Authorization", "Bearer " + apiKey);
            // 2. Content-Type 설정 (application/json)
            requestTemplate.header("Content-Type", "application/json");
        };
    }
}
