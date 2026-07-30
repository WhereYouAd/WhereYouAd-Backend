package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class SocialOAuthClientConfig {

    // 소셜 제공자의 토큰 갱신 및 연동 해제 API 호출에 사용하는 공통 RestClient
    @Bean
    @Qualifier("socialOAuthRestClient")
    RestClient socialOAuthRestClient() {
        // 외부 API 장애가 회원탈퇴 요청을 장시간 점유하지 않도록 연결 및 응답 제한 시간을 설정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
