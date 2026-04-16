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

    // 1일간의 MetricFact(통계) 조회
    public Mono<String> searchGoogleAdsData(String customerId, PlatformConnection connection, AdAuthRequest request) {
        String apiUrl = apiBaseUrl + customerId + "/googleAds:search";
        String targetDate = LocalDate.now().minusDays(1).toString();

        String requestBody = "{\n" +
                "  \"query\": \"SELECT " +
                "segments.date, " +
                "metrics.impressions, " +
                "metrics.clicks, " +
                "metrics.conversions, " +
                "metrics.cost_micros, " +
                "metrics.conversions_value " +
                "FROM customer " +
                "WHERE segments.date = '" + targetDate + "'\"\n" +
                "}";

        return postWebClientRequest(customerId, connection, request, apiUrl, requestBody);
    }

    // 전체 캠페인 조회
    public Mono<String> searchAllCampaigns(String customerId, PlatformConnection connection, AdAuthRequest request) {
        String apiUrl = apiBaseUrl + customerId + "/googleAds:search";

        String requestBody = "{\n" +
                "  \"query\": \"SELECT " +
                "campaign.id, " +
                "campaign.name, " +
                "campaign.status, " +
                "campaign.start_date, " +
                "campaign.end_date, " +
                "campaign_budget.amount_micros, " +
                "campaign.advertising_channel_type " +
                "FROM campaign " +
                "WHERE campaign.status != 'REMOVED'\"\n" +
                "}";

        return postWebClientRequest(customerId, connection, request, apiUrl, requestBody);
    }

    // 전체 광고 그룹 조회
    public Mono<String> searchAllAdGroups(String customerId, PlatformConnection connection, AdAuthRequest request) {
        String apiUrl = apiBaseUrl + customerId + "/googleAds:search";

        String requestBody = "{\n" +
                "  \"query\": \"SELECT " +
                "ad_group.id, " +
                "ad_group.name, " +
                "ad_group.status, " +
                "campaign.id " +
                "FROM ad_group " +
                "WHERE ad_group.status != 'REMOVED'\"\n" +
                "}";

        return postWebClientRequest(customerId, connection, request, apiUrl, requestBody);
    }

    // 전체 개별 광고 조회
    public Mono<String> searchAllAdContents(String customerId, PlatformConnection connection, AdAuthRequest request) {
        String apiUrl = apiBaseUrl + customerId + "/googleAds:search";

        String requestBody = "{\n" +
                "  \"query\": \"SELECT " +
                "ad_group_ad.ad.id, " +
                "ad_group_ad.ad.name, " +
                "ad_group_ad.status, " +
                "ad_group_ad.ad.type, " +
                "ad_group_ad.ad.final_urls, " +
                "ad_group_ad.ad.tracking_url_template, " +
                "ad_group.id " +
                "FROM ad_group_ad " +
                "WHERE ad_group_ad.status != 'REMOVED'\"\n" +
                "}";

        return postWebClientRequest(customerId, connection, request, apiUrl, requestBody);
    }

    // 공통 WebClient 비동기 POST 요청
    private Mono<String> postWebClientRequest(String customerId, PlatformConnection connection, AdAuthRequest request, String apiUrl, String requestBody) {
        return Mono.defer(() -> {
            try {
                Map<String, String> headers = googleAdAuthStrategy.generateHeaders(connection, request);

                return webClient.post()
                        .uri(apiUrl)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::add))
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(error -> log.error("[Google Ads API Error] Customer ID: {}", customerId, error));

            } catch (Exception e) {
                return Mono.error(new RuntimeException("구글 헤더 생성 및 토큰 갱신 실패", e));
            }
        });
    }
}