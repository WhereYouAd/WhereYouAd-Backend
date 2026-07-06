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
    private final String apiBaseUrl = "https://googleads.googleapis.com/v23/customers/";

    // 최근 7일간의 MetricFact(통계) 조회
    public Mono<String> searchGoogleAdsData(String customerId, PlatformConnection connection, AdAuthRequest request) {
        String apiUrl = apiBaseUrl + customerId + "/googleAds:search";
        String startDate = LocalDate.now().minusDays(7).toString();
        String endDate = LocalDate.now().minusDays(1).toString();

        String requestBody = "{\n" +
                "  \"query\": \"SELECT " +
                "campaign.id, " +           // 캠페인 ID 추가
                "ad_group_ad.ad.id, " +     // 광고 소재 ID 추가
                "segments.date, " +
                "metrics.impressions, " +
                "metrics.clicks, " +
                "metrics.conversions, " +
                "metrics.cost_micros, " +
                "metrics.conversions_value " +
                "FROM ad_group_ad " +       // customer 대신 ad_group_ad 에서 조회
                "WHERE segments.date BETWEEN '" + startDate + "' AND '" + endDate + "'\"\n" +
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
                "campaign.start_date_time, " +
                "campaign.end_date_time, " +
                "campaign_budget.amount_micros, " +
                "campaign_budget.period, " +
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

    // 캠페인 예산 리소스 이름 조회
    public Mono<String> getCampaignBudgetResourceName(String customerId, PlatformConnection connection, AdAuthRequest request, String externalCampaignId) {
        String apiUrl = apiBaseUrl + customerId + "/googleAds:search";

        String requestBody = "{\n" +
                "  \"query\": \"SELECT " +
                "campaign.campaign_budget " +
                "FROM campaign " +
                "WHERE campaign.id = '" + externalCampaignId + "'\"\n" +
                "}";

        return postWebClientRequest(customerId, connection, request, apiUrl, requestBody);
    }

    // 캠페인 예산 변경 (mutate)
    public Mono<String> mutateCampaignBudget(String customerId, PlatformConnection connection, AdAuthRequest request, String budgetResourceName, Long amountMicros) {
        String apiUrl = apiBaseUrl + customerId + "/campaignBudgets:mutate";

        String requestBody = "{\n" +
                "  \"operations\": [\n" +
                "    {\n" +
                "      \"updateMask\": \"amountMicros\",\n" +
                "      \"update\": {\n" +
                "        \"resourceName\": \"" + budgetResourceName + "\",\n" +
                "        \"amountMicros\": \"" + amountMicros + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
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
                        .doOnError(org.springframework.web.reactive.function.client.WebClientResponseException.class, e -> {
                            log.error("[Google Ads API Error] Customer ID: {}", customerId);
                            log.error("상태 코드: {}", e.getStatusCode());
                            log.error("에러 상세 내용: {}", e.getResponseBodyAsString());
                        })
                        .doOnError(error -> {
                            // WebClientResponseException이 아닌 다른 에러(네트워크 단절 등)일 경우
                            if (!(error instanceof org.springframework.web.reactive.function.client.WebClientResponseException)) {
                                log.error("[Google Ads API Network Error] Customer ID: {}", customerId, error);
                            }
                        });

            } catch (Exception e) {
                return Mono.error(new RuntimeException("구글 헤더 생성 및 토큰 갱신 실패", e));
            }
        });
    }
}