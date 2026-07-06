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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
            // 1. 캠페인의 campaign_budget 리소스 이름 조회
            String customerId = account.getExternalAccountId();
            AdAuthRequest emptyRequest = AdAuthRequest.empty();
            String jsonResponse = googleAdWebClient.getCampaignBudgetResourceName(customerId, connection, emptyRequest, campaign.getExternalCampaignId()).block();
            
            String budgetResourceName = extractBudgetResourceName(jsonResponse);
            if (budgetResourceName == null) {
                log.error("[Google] 예산 리소스 이름 추출 실패 - campaignId: {}", campaign.getExternalCampaignId());
                throw new AdApiHandler(AdApiErrorCode.BUDGET_UPDATE_FAILED);
            }

            // 2. 캠페인 예산 변경(Mutate)
            googleAdWebClient.mutateCampaignBudget(customerId, connection, emptyRequest, budgetResourceName, amountMicros).block();

            // 3. 엔티티 업데이트 및 히스토리 저장
            campaign.updateBudget(request.amount());
            
            BudgetType requestType = request.budgetType();
            campaign.applyBudgetType(requestType);

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
}
