package com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request.AdvertisementRequest;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.GoogleAdResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.GoogleAdErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.BudgetHistoryRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.infrastructure.client.google.GoogleAdWebClient;
import com.whereyouad.WhereYouAd.infrastructure.client.google.dto.GoogleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleBudgetService {

    private final AdCampaignRepository adCampaignRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;
    private final GoogleAdWebClient googleAdWebClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public GoogleAdResponse.BudgetUpdateResponse updateCampaignBudget(
            Long userId, Long campaignId, AdvertisementRequest.GoogleBudgetUpdateRequest request
    ) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCAMPAIGN_NOT_FOUND));

        if (campaign.getProvider() != Provider.GOOGLE) {
            throw new AdApiHandler(AdApiErrorCode.INVALID_PROVIDER_VALUE);
        }

        // 예산 유형 검증 (기존 예산 유형과 다른 유형으로 변경 불가)
        BudgetType requestType = request.budgetType();
        if (campaign.getBudgetType() != requestType) {
            throw new AdApiHandler(GoogleAdErrorCode.INVALID_BUDGET_TYPE);
        }

        // 금액 변경 여부 검증
        Long previousBudget = campaign.getBudget();
        if (Objects.equals(previousBudget, request.amount())) {
            throw new AdApiHandler(GoogleAdErrorCode.SAME_BUDGET_AMOUNT);
        }

        PlatformAccount account = campaign.getPlatformAccount();
        PlatformConnection connection = resolveConnection(userId, account);

        // 금액 단위 변환: Google Ads는 Micros 단위를 사용 (1,000,000 micros = 1 통화 단위)
        Long amountMicros = request.amount() * 1_000_000L;

        try {
            // 1. 캠페인의 campaign_budget 리소스 이름 조회를 위해 해당 캠페인이 위치한 정확한 Client Account ID 찾기
            String rootCustomerId = account.getExternalAccountId();
            AdAuthRequest emptyRequest = AdAuthRequest.empty();

            List<String> clientAccountIds = fetchAccessibleClientAccountsLocal(rootCustomerId, connection, emptyRequest);
            
            String budgetResourceName = null;
            String targetCustomerId = rootCustomerId;
            
            for (String clientAccountId : clientAccountIds) {
                try {
                    String jsonResponse = googleAdWebClient.getCampaignBudgetResourceName(clientAccountId, connection, emptyRequest, campaign.getExternalCampaignId()).block();
                    budgetResourceName = extractBudgetResourceName(jsonResponse);
                    if (budgetResourceName != null) {
                        targetCustomerId = clientAccountId;
                        break; // 캠페인을 찾았으므로 중단
                    }
                } catch (Exception e) {
                    // 해당 하위 계정에 캠페인이 없거나 권한 오류일 시 다음 계정 확인
                    log.debug("하위 계정 [{}]에서 캠페인 조회 실패 (권한 없음 또는 캠페인 없음). 다음 계정 탐색 진행", clientAccountId);
                }
            }

            if (budgetResourceName == null) {
                log.error("[Google] 예산 리소스 이름 추출 실패 - 모든 하위 계정 탐색 완료, campaignId: {}", campaign.getExternalCampaignId());
                throw new AdApiHandler(AdApiErrorCode.BUDGET_UPDATE_FAILED);
            }

            // 2. 캠페인 예산 변경(Mutate) - 찾은 targetCustomerId를 대상으로 수행
            googleAdWebClient.mutateCampaignBudget(targetCustomerId, connection, emptyRequest, budgetResourceName, amountMicros).block();

            // 3. 엔티티 업데이트 및 히스토리 저장
            campaign.updateBudget(request.amount());

            budgetHistoryRepository.save(AdvertisementConverter.toCampaignBudgetHistory(
                    campaign, previousBudget, request.amount(), userId, Provider.GOOGLE
            ));

            return new GoogleAdResponse.BudgetUpdateResponse(
                    campaign.getId(), campaign.getExternalCampaignId(), request.amount(), requestType
            );

        } catch (WebClientResponseException e) {
            log.error("[Google] 캠페인 예산 변경 실패 - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AdApiHandler(AdApiErrorCode.BUDGET_UPDATE_FAILED);
        } catch (Exception e) {
            log.error("[Google] 캠페인 예산 변경 중 오류 발생", e);
            throw new AdApiHandler(AdApiErrorCode.BUDGET_UPDATE_FAILED);
        }
    }

    private PlatformConnection resolveConnection(Long userId, PlatformAccount account) {
        return platformConnectionRepository
                .findByUserIdAndPlatformAccountId(userId, account.getId())
                .filter(c -> c.getRevokedAt() == null)
                .orElseThrow(() -> new AdApiHandler(GoogleAdErrorCode.NOT_ACCOUNT_OWNER));
    }

    private String extractBudgetResourceName(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.path("results");
            if (results.isArray() && results.size() > 0) {
                JsonNode campaign = results.get(0).path("campaign");
                if (!campaign.isMissingNode()) {
                    return campaign.path("campaignBudget").asText(null);
                }
            }
        } catch (Exception e) {
            log.error("Google Ads GAQL 응답 파싱 중 오류", e);
        }
        return null;
    }

    private List<String> fetchAccessibleClientAccountsLocal(String customerId, PlatformConnection connection, AdAuthRequest request) {
        try {
            String jsonResponse = googleAdWebClient.getAccessibleClientAccounts(customerId, connection, request).block();
            if (jsonResponse == null || jsonResponse.isBlank()) return java.util.List.of(customerId);

            GoogleDTO.AdCustomerClientResponse response =
                objectMapper.readValue(jsonResponse, GoogleDTO.AdCustomerClientResponse.class);
            
            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                return response.getResults().stream()
                        .filter(result -> result.getCustomerClient() != null)
                        .filter(result -> Boolean.FALSE.equals(result.getCustomerClient().getManager()))
                        .map(result -> result.getCustomerClient().getId())
                        .toList();
            }
        } catch (Exception e) {
            log.warn("하위 클라이언트 계정 조회 중 오류 발생 (기본 계정으로 대체): {}", e.getMessage());
        }
        return java.util.List.of(customerId); // 실패하거나 비어있으면 일단 자기 자신 반환 (단일 계정일 경우)
    }
}
