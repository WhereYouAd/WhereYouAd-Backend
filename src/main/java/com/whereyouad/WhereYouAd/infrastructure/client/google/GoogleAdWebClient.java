package com.whereyouad.WhereYouAd.infrastructure.client.google;

import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.global.adapi.strategy.GoogleAdAuthStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleAdWebClient {

    private final WebClient webClient;

    private final GoogleAdAuthStrategy googleAdAuthStrategy;

    private final String apiBaseUrl = "https://googleads.googleapis.com/v15/customers/";

    // 1일간의 MetricFact 조회
    public void searchGoogleAdsData(String customerId, PlatformConnection connection, AdAuthRequest request) {

        String apiUrl = apiBaseUrl + customerId + "/googleAds:search";

        // GAQL (Google Ads Query Language) 쿼리문 작성
        // 하루 동안의 캠페인별 정보 조회
        String requestBody = "{ \n" +
                "  \"query\": \"SELECT\n" +
                "  segments.date,\n" +
                "  metrics.impressions,\n" +
                "  metrics.clicks,\n" +
                "  metrics.conversions,\n" +
                "  metrics.cost_micros,\n" +
                "  metrics.conversions_value\n" +
                "FROM customer\n" +
                "WHERE segments.date = " + LocalDate.now().minusDays(1) + "\n" +
                "}";

        postWebClientRequest(customerId, connection, request,apiUrl, requestBody);
    }

    // 전체 캠페인 조회

    // 전체 광고 그룹 조회

    // 전체 개별 광고 조회

    private Mono<String> postWebClientRequest(String customerId, PlatformConnection connection, AdAuthRequest request, String apiUrl, String requestBody) {
        return Mono.defer(() -> {
            try {
                Map<String, String> headers = googleAdAuthStrategy.generateHeaders(connection, request);

                // WebClient 비동기 POST 요청
                return webClient.post()
                        .uri(apiUrl)
                        .headers(httpHeaders -> {
                            // WebClient 헤더에 주입
                            headers.forEach(httpHeaders::add);
                        })
                        .bodyValue(requestBody)
                        .retrieve() // 응답 추출 시작
                        .bodyToMono(String.class) // JSON 응답을 String으로 변환
                        .doOnError(error -> log.error("[Google Ads API Error] Customer ID: {}", customerId, error));

            } catch (Exception e) {
                return Mono.error(new RuntimeException("구글 헤더 생성 및 토큰 갱신 실패", e));
            }
        });
    }
}
