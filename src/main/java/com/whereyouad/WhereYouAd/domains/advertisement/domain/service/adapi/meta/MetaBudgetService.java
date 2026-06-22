package com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request.AdvertisementRequest;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.BudgetHistoryRepository;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Currency;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.utils.AdApiAuthUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.client.MetaClient;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.converter.MetaConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaDTO;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaBudgetService {

    private final AdCampaignRepository adCampaignRepository;
    private final AdGroupRepository adGroupRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;
    private final AdApiAuthUtil adApiAuthUtil;
    private final MetaClient metaClient;

    // 통화별 일일 최소 예산 — Meta 노출 과금 기준 하한.
    // 최적화 기반 광고세트는 이보다 높을 수 있어, 초과분은 Meta 에러 매핑
    private static final Map<Currency, Long> MIN_BUDGET_BY_CURRENCY = Map.of(
            Currency.KRW, 1000L,  // 한국 원화로 1000원
            Currency.USD, 1L  // 미국 달러로 1달러
    );

    private static final long DEFAULT_MIN_BUDGET = 1L;

    // 캠페인 예산 변경 (dailyBudget / lifetimeBudget 둘 중 하나)
    @Transactional
    public MetaResponse.BudgetUpdateResponse updateCampaignBudget(
            Long userId, Long campaignId, AdvertisementRequest.MetaBudgetUpdateRequest request
    )
    {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCAMPAIGN_NOT_FOUND));


        // 캠페인의 이전 예산 값 추출, 동일 값 검증
        Long previousBudget = campaign.getBudget();
        if (previousBudget.equals(request.amount())) {
            throw new AdApiHandler(AdApiErrorCode.SAME_BUDGET_AMOUNT);
        }

        // Meta 의 광고가 맞는지 검증
        if (campaign.getProvider() != Provider.META) {
            throw new AdApiHandler(AdApiErrorCode.INVALID_PROVIDER_VALUE);
        }

        PlatformAccount account = campaign.getPlatformAccount();
        Currency currency = account.getCurrency();

        // dailyBudget·lifetimeBudget 중 입력된 값을 통화별 최소 기준과 비교 검증
        validateMinBudget(request.amount(), currency);

        // 각 예산 수정 요청 값 (dailyBudget, lifetimeBudget) 을 Meta 에서 사용하는 통화 단위로 변환
        Long dailyMinor = MetaConverter.toMetaBudget(request.dailyBudget(), currency);
        Long lifetimeMinor = MetaConverter.toMetaBudget(request.lifetimeBudget(), currency);

        // 사용자 토큰 추출 & 검증
        String token = resolveOwnerAccessToken(userId, account);

        // Meta 광고 캠페인 예산 수정 API 요청
        callMetaBudgetUpdate(campaign.getExternalCampaignId(), token, dailyMinor, lifetimeMinor);

        // 엔티티의 예산 값 수정
        campaign.updateBudget(request.amount());

        // 캠페인 예산 변경 이력 추가 (BudgetHistory 엔티티 추가)
        budgetHistoryRepository.save(AdvertisementConverter.toCampaignBudgetHistory(
                campaign, previousBudget, request.amount(), userId, Provider.META
        ));

        return new MetaResponse.BudgetUpdateResponse(
                campaign.getId(), campaign.getExternalCampaignId(), request.amount(), request.budgetType()
        );
    }

    //  광고그룹 예산 변경 (AdSet은 daily_budget만 존재 → DAILY 고정)
    @Transactional
    public MetaResponse.BudgetUpdateResponse updateAdGroupBudget(
            Long userId, Long adGroupId, AdvertisementRequest.MetaBudgetUpdateRequest request)
    {

        // Meta 의 광고 그룹(AdSet) 은 dailyBudget 만 변경 가능 -> lifetimeBudget 에 값 존재 시 오류
        if (request.lifetimeBudget() != null) {
            throw new AdApiHandler(AdApiErrorCode.BUDGET_TYPE_NOT_SUPPORTED);
        }

        AdGroup adGroup = adGroupRepository.findById(adGroupId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADGROUP_NOT_FOUND));

        // 이전 광고 그룹 예산 값 추출, 동일값 검증
        Long previousBudget = adGroup.getBudget();
        if (previousBudget.equals(request.dailyBudget())) {
            throw new AdApiHandler(AdApiErrorCode.SAME_BUDGET_AMOUNT);
        }

        AdCampaign campaign = adGroup.getAdCampaign();

        // Meta 의 광고가 맞는지 검증
        if (campaign.getProvider() != Provider.META) {
            throw new AdApiHandler(AdApiErrorCode.INVALID_PROVIDER_VALUE);
        }

        PlatformAccount account = campaign.getPlatformAccount();
        Currency currency = account.getCurrency();

        // 광고 계정에 설정된 통화 단위(원or달러) 에 맞춰 최소 금액 이상으로 요청한게 맞는지 검증
        validateMinBudget(request.dailyBudget(), currency);

        // 사용자 토큰 추출 & 검증
        String token = resolveOwnerAccessToken(userId, account);

        // 예산 수정 요청 값 (dailyBudget) 을 Meta 에서 사용하는 통화 단위로 변환
        Long dailyMinor = MetaConverter.toMetaBudget(request.dailyBudget(), currency);

        // Meta 광고 캠페인 예산 수정 API 요청
        callMetaBudgetUpdate(adGroup.getExternalGroupId(), token, dailyMinor, null);

        // 엔티티의 예산 값 수정
        adGroup.updateBudget(request.dailyBudget(), null);

        // 광고 그룹 예산 변경 이력 추가 (BudgetHistory 엔티티 추가)
        budgetHistoryRepository.save(AdvertisementConverter.toAdGroupBudgetHistory(
                adGroup, previousBudget, request.dailyBudget(), userId, Provider.META
        ));

        return new MetaResponse.BudgetUpdateResponse(
                adGroup.getId(), adGroup.getExternalGroupId(), request.dailyBudget(), request.budgetType()
        );
    }

    //  Meta 호출하여 예산 수정 + 예외 매핑
    private void callMetaBudgetUpdate(String nodeId, String token, Long dailyMinor, Long lifetimeMinor) {
        try {
            MetaDTO.UpdateResponse response = metaClient.updateBudget(nodeId, token, dailyMinor, lifetimeMinor);

            if (response == null || !Boolean.TRUE.equals(response.success())) {
                throw new AdApiHandler(AdApiErrorCode.BUDGET_UPDATE_FAILED);
            }

        } catch (FeignException e) {

            log.error("[META] 예산 변경 실패 - nodeId={}, status={}, body={}", nodeId, e.status(), e.contentUTF8());

            // 변경하려는 예산 금액이 Meta 의 최소 기준치 미만일 시 오류
            if (isBudgetTooLowError(e)) {
                throw new AdApiHandler(AdApiErrorCode.INVALID_BUDGET_AMOUNT);
            }

            // 캠페인에 설정된 예산 유형(일일/총)과 다른 유형으로 변경 요청 시 오류
            if (isBudgetTypeMismatchError(e)) {
                throw new AdApiHandler(AdApiErrorCode.INVALID_BUDGET_TYPE);
            }

            // 그 외 오류
            throw new AdApiHandler(AdApiErrorCode.BUDGET_UPDATE_FAILED);
        }
    }


    // 요청자가 이 계정을 연동한 주인인지 검증 + 해당 사용자의 토큰 추출
    private String resolveOwnerAccessToken(Long userId, PlatformAccount account) {
        PlatformConnection connection = platformConnectionRepository
                .findByUserIdAndPlatformAccountId(userId, account.getId())
                .filter(c -> c.getRevokedAt() == null)
                .orElseThrow(() -> new AdApiHandler(AdApiErrorCode.NOT_ACCOUNT_OWNER));

        if (connection.getTokenExpireAt() == null || connection.getTokenExpireAt().isBefore(LocalDateTime.now())) {
            throw new AdApiHandler(AdApiErrorCode.INVALID_API_CREDENTIALS);
        }

        try {
            return adApiAuthUtil.generateAuthHeaders(connection.getId(), AdAuthRequest.empty()).get("access_token");

        } catch (Exception e) {

            log.error("[META] 예산 변경용 인증 데이터 생성 실패 (connId: {})", connection.getId(), e);
            throw new AdApiHandler(AdApiErrorCode.INVALID_API_CREDENTIALS);
        }
    }

    // 예산이 Meta 의 절대적 최소 기준치(1000원/1달러) 이하일 경우 오류 발생 메서드
    private void validateMinBudget(Long budget, Currency currency) {
        long min = MIN_BUDGET_BY_CURRENCY.getOrDefault(currency, DEFAULT_MIN_BUDGET);
        if (budget == null || budget < min) {
            throw new AdApiHandler(AdApiErrorCode.INVALID_BUDGET_AMOUNT);
        }
    }

    // 예산이 Meta 의 광고에서 동적 최소 금액 이하 인 경우 오류 발생 메서드
    private boolean isBudgetTooLowError(FeignException e) {
        String body = e.contentUTF8();
        if (body == null) return false;
        String lower = body.toLowerCase();
        return body.contains("1487079") || lower.contains("minimum") || lower.contains("budget is too low");
    }

    // 일일 예산 캠페인(dailyBudget)에 총 예산(lifetimeBudget) 또는 그 반대로 요청을 보내는 등
    // 예산 유형이 맞지 않을 때 예외 처리
    private boolean isBudgetTypeMismatchError(FeignException e) {
        String body = e.contentUTF8();
        if (body == null) return false;
        return body.contains("1885630");
    }
}
