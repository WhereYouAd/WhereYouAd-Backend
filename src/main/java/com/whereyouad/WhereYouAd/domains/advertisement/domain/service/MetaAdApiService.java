package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.AuthType;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Currency;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Timezone;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.global.utils.AdApiAuthUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.client.MetaClient;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.config.MetaAdConfig;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.converter.MetaConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaDTO;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaAdApiService {

    // Repository
    private final PlatformConnectionRepository platformConnectionRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final OrgRepository orgRepository;
    private final UserRepository userRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final AdGroupRepository adGroupRepository;
    private final AdContentRepository adContentRepository;
    private final MetricFactRepository metricFactRepository;
    private final OrgMemberRepository orgMemberRepository;

    //Util & FeignClient & Config
    private final AdApiAuthUtil adApiAuthUtil;
    private final AESUtil aesUtil;
    private final MetaClient metaClient;
    private final MetaAdConfig metaAdConfig;

    // 트랜잭션 수동 제어를 위한 템플릿 Bean
    private final TransactionTemplate transactionTemplate;

    // === Graph API fields 상수 ===
    private static final String ACCOUNT_FIELDS =
            "id,name,account_id,account_status,currency,timezone_name";
    private static final String CAMPAIGN_FIELDS =
            "id,name,status,objective,daily_budget,lifetime_budget,start_time,stop_time";
    private static final String ADSET_FIELDS =
            "id,campaign_id,name,status,daily_budget,targeting,billing_event,optimization_goal";
    private static final String AD_FIELDS =
            "id,adset_id,name,status,creative{id,body,object_type}";
    private static final String INSIGHT_FIELDS =
            "ad_id,campaign_id,adset_id,impressions,clicks,spend,actions,date_start,date_stop";


    // 1. OAuth 인증 URL 생성
    public MetaResponse.AuthUrlResponse getAuthorizationUrl(Long userId, Long orgId) {

        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        String authUrl = metaAdConfig.buildAuthorizationUrl(orgId);

        return MetaConverter.toAuthUrlResponse(authUrl);
    }

    // 2. OAuth 콜백 → 토큰 발급 + 즉시 전체 동기화
    public MetaResponse.MetaSyncSummary handleCallback(Long orgId, Long userId, String code) {
        // 트랜잭션 밖에서 외부 통신 수행
        MetaDTO.TokenResponse shortLived = exchangeCodeForToken(code);
        MetaDTO.TokenResponse longLived = exchangeForLongLivedToken(shortLived.accessToken());
        MetaDTO.AdAccountListResponse adAccounts = fetchAdAccounts(longLived.accessToken());

        // 트랜잭션 안에서 DB Insert 수행
        transactionTemplate.executeWithoutResult(status -> {
            Organization org = orgRepository.findById(orgId)
                    .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));
            savePlatformConnection(longLived, org, userId, adAccounts);
        });

        String startDate = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return syncAll(orgId, startDate, endDate);
    }


    // ============================
    // 3. 전체 동기화 (UPSERT 방식)
    // ============================
    public MetaResponse.MetaSyncSummary syncAll(Long orgId, String startDate, String endDate) {

        PlatformSessionContext context = transactionTemplate.execute(status -> {

            PlatformConnection conn = resolveMetaConnection(orgId);
            PlatformAccount pAccount = conn.getPlatformAccount();
            Organization org = pAccount.getOrganization();
            org.getId();
            org.getName();
            pAccount.getAccountName();

            return new PlatformSessionContext(
                    conn.getId(),
                    pAccount.getExternalAccountId(),
                    org,
                    pAccount);
        });

        Map<String, String> authData;

        try {
            authData = adApiAuthUtil.generateAuthHeaders(context.connId(), AdAuthRequest.empty());
        } catch (Exception e) {
            log.error("Meta 인증 데이터 생성 실패 (connId: {})", context.connId(), e);
            throw new AdApiHandler(AdApiErrorCode.INVALID_API_CREDENTIALS);
        }

        // 토큰 문자열 추출
        String accessToken = authData.get("access_token");
        int campaignCount = 0, adSetCount = 0, adCount = 0, metricCount = 0;

        try {
            // ─── 3-1. 캠페인 UPSERT ───
            Map<String, AdCampaign> campaignMap = new HashMap<>();
            String campaignCursor = null;

            do {
                MetaDTO.CampaignListResponse campaignsResp =
                        metaClient.getCampaigns(accessToken, context.adAccountId(), CAMPAIGN_FIELDS, campaignCursor);
                if (campaignsResp == null || campaignsResp.data() == null || campaignsResp.data().isEmpty()) break;
                for (MetaDTO.Campaign metaCampaign : campaignsResp.data()) {
                    AdCampaign campaign = transactionTemplate.execute(status ->
                            upsertCampaign(metaCampaign, context.org(), context.platformAccount())
                    );
                    campaignMap.put(metaCampaign.id(), campaign);
                    campaignCount++;
                }
                campaignCursor = getNextCursor(campaignsResp.paging());
            } while (campaignCursor != null);

            if (campaignMap.isEmpty()) {
                return MetaConverter.toSyncSummary(0,0,0,0);
            }

            // ─── 3-2. 광고세트(AdSet) UPSERT ───
            Map<String, AdGroup> adSetMap = new HashMap<>();
            String adSetCursor = null;

            do {

                MetaDTO.AdSetListResponse adSetsResp =
                        metaClient.getAdSets(accessToken, context.adAccountId(), ADSET_FIELDS, adSetCursor);

                if (adSetsResp == null || adSetsResp.data() == null || adSetsResp.data().isEmpty()) {
                    break;
                }

                for (MetaDTO.AdSet metaAdSet : adSetsResp.data()) {

                    AdCampaign parentCampaign = campaignMap.get(metaAdSet.campaignId());

                    if (parentCampaign == null) {
                        continue;
                    }
                    AdGroup adGroup = transactionTemplate.execute(status -> upsertAdGroup(metaAdSet, parentCampaign));
                    adSetMap.put(metaAdSet.id(), adGroup);
                    adSetCount++;
                }

                adSetCursor = getNextCursor(adSetsResp.paging());

            } while (adSetCursor != null);

            // ─── 3-3. 광고(Ad) UPSERT ───
            Map<String, AdContent> adMap = new HashMap<>();
            if (!adSetMap.isEmpty()) {

                String adCursor = null;

                do {
                    MetaDTO.AdListResponse adsResp =
                            metaClient.getAds(accessToken, context.adAccountId(), AD_FIELDS, adCursor);

                    if (adsResp == null || adsResp.data() == null || adsResp.data().isEmpty()) {
                        break;
                    }

                    for (MetaDTO.Ad metaAd : adsResp.data()) {
                        AdGroup parentAdGroup = adSetMap.get(metaAd.adSetId());

                        if (parentAdGroup == null) {
                            continue;
                        }

                        AdContent content = transactionTemplate.execute(status -> upsertAdContent(metaAd, parentAdGroup));
                        adMap.put(metaAd.id(), content);
                        adCount++;
                    }

                    adCursor = getNextCursor(adsResp.paging());

                } while (adCursor != null);
            }

            // ─── 3-4. 인사이트(MetricFact) UPSERT ───
            if (startDate != null && endDate != null && !adMap.isEmpty()) {

                String timeRange = "{\"since\":\"" + startDate + "\",\"until\":\"" + endDate + "\"}";
                String insightCursor = null;

                do {

                    MetaDTO.InsightListResponse insightsResp =
                            metaClient.getInsights(accessToken, context.adAccountId(), INSIGHT_FIELDS,
                                    "ad", timeRange, "1", insightCursor);

                    if (insightsResp == null || insightsResp.data() == null || insightsResp.data().isEmpty()) {
                        break;
                    }

                    for (MetaDTO.Insight insight : insightsResp.data()) {

                        AdContent content = adMap.get(insight.adId());
                        AdCampaign campaign = campaignMap.get(insight.campaignId());

                        if (content == null || campaign == null) {
                            continue;
                        }

                        transactionTemplate.executeWithoutResult(status ->
                                upsertMetricFact(insight, content, campaign, context.platformAccount())
                        );

                        metricCount++;
                    }

                    insightCursor = getNextCursor(insightsResp.paging());

                } while (insightCursor != null);

            }

        } catch (FeignException e) {

            log.error("[META] 페이스북 API 통신 에러 (Feign) - status={}, body={}", e.status(), e.contentUTF8());
            throw new AdApiHandler(AdApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);

        } catch (Exception e) {

            log.error("[META] 데이터 동기화 파싱/통신 중 알 수 없는 에러", e);
            throw new AdApiHandler(AdApiErrorCode.SYNC_DATA_PROCESSING_ERROR);
        }

        log.info("[META] 동기화 완료 - 캠페인:{}, 광고세트:{}, 광고:{}, 지표:{}", campaignCount, adSetCount, adCount, metricCount);

        return MetaConverter.toSyncSummary(campaignCount, adSetCount, adCount, metricCount);
    }


    // ============================
    // UPSERT 메서드
    // ============================

    // 캠페인 UPSERT: externalCampaignId + Provider로 조회 → 있으면 update, 없으면 create
    private AdCampaign upsertCampaign(MetaDTO.Campaign src, Organization org, PlatformAccount platformAccount) {

        AdCampaign newData = MetaConverter.toCampaign(src, org, platformAccount);

        return adCampaignRepository
                .findByExternalCampaignIdAndProvider(src.id(), Provider.META)
                .map(existing -> {
                    existing.updateFromApi(
                            newData.getName(),
                            newData.getStatus(),
                            newData.getBudget(),
                            newData.getGoal(),
                            newData.getStartDate(),
                            newData.getEndDate()
                    );
                    return adCampaignRepository.save(existing);
                })
                .orElseGet(() -> adCampaignRepository.save(newData));
    }

    // 광고그룹 UPSERT: externalGroupId로 조회
    private AdGroup upsertAdGroup(MetaDTO.AdSet src, AdCampaign campaign) {

        AdGroup newData = MetaConverter.toAdGroup(src, campaign);

        return adGroupRepository
                .findByExternalGroupId(src.id())
                .map(existing -> {
                    existing.updateFromApi(
                            newData.getName(),
                            newData.getStatus(),
                            newData.getTargetingInfo());
                    return adGroupRepository.save(existing);
                })
                .orElseGet(() -> adGroupRepository.save(newData));
    }

    // 광고(소재) UPSERT: name + adGroup으로 조회
    private AdContent upsertAdContent(MetaDTO.Ad src, AdGroup adGroup) {

        AdContent newData = MetaConverter.toAdContent(src, adGroup);

        return adContentRepository
                .findByNameAndAdGroup(src.name(), adGroup)
                .map(existing -> {
                    existing.updateFromApi(
                            newData.getType(),
                            newData.getStatus(),
                            newData.getDescription());
                    return adContentRepository.save(existing);
                })
                .orElseGet(() -> adContentRepository.save(newData));
    }

    // 성과지표 UPSERT: adContent + timeBucket + provider로 조회
    private void upsertMetricFact(MetaDTO.Insight src, AdContent adContent, AdCampaign adCampaign, PlatformAccount platformAccount) {

        MetricFact newData = MetaConverter.toMetricFact(src, adContent, adCampaign, platformAccount);

        metricFactRepository
                .findByAdContentAndTimeBucketAndProvider(
                        adContent, newData.getTimeBucket(), Provider.META)
                .ifPresentOrElse(
                        existing -> existing.updateFromApi(
                                newData.getImpressions(),
                                newData.getClicks(),
                                newData.getConversions(),
                                newData.getSpend(),
                                newData.getRevenue()),
                        () -> metricFactRepository.save(newData)
                );

    }

    // ============================
    // private: Connection 조회
    // ============================

    //orgId 기반으로 META Connection을 찾아 반환
    private PlatformConnection resolveMetaConnection(Long orgId) {

        List<PlatformConnection> connections = platformConnectionRepository
                .findByPlatformAccount_Organization_IdAndPlatformAccount_Provider(
                        orgId, Provider.META);

        if (connections.isEmpty()) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND);
        }

        return connections.stream()
                .max(Comparator.comparing(PlatformConnection::getId))
                .get();
    }

    // ============================
    // private: OAuth 토큰 교환
    // ============================

    // 인가 코드 → short-lived Access Token 교환
    private MetaDTO.TokenResponse exchangeCodeForToken(String code) {

        RestTemplate restTemplate = new RestTemplate();
        String resolvedRedirectUri = metaAdConfig.getRedirectUri();

        String url = UriComponentsBuilder
                .fromHttpUrl(metaAdConfig.getGraphApiBaseUrl() + "/oauth/access_token")
                .queryParam("client_id", metaAdConfig.getAppId())
                .queryParam("client_secret", metaAdConfig.getAppSecret())
                .queryParam("redirect_uri", resolvedRedirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();

        try {
            ResponseEntity<MetaDTO.TokenResponse> response = restTemplate.getForEntity(url, MetaDTO.TokenResponse.class);

            if (response.getBody() == null) {
                throw new AdApiHandler(AdApiErrorCode.TOKEN_RESPONSE_EMPTY);
            }

            return response.getBody();

        } catch (HttpClientErrorException e) {

            log.error("[META] 토큰 교환 실패 - status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AdApiHandler(AdApiErrorCode.TOKEN_EXCHANGE_FAILED);
        }
    }

    //short-lived → long-lived token 교환 (60일 유효)
    private MetaDTO.TokenResponse exchangeForLongLivedToken(String shortLivedToken) {
        RestTemplate restTemplate = new RestTemplate();

        String url = UriComponentsBuilder
                .fromHttpUrl(metaAdConfig.getGraphApiBaseUrl() + "/oauth/access_token")
                .queryParam("grant_type", "fb_exchange_token")
                .queryParam("client_id", metaAdConfig.getAppId())
                .queryParam("client_secret", metaAdConfig.getAppSecret())
                .queryParam("fb_exchange_token", shortLivedToken)
                .build()
                .toUriString();
        try {
            ResponseEntity<MetaDTO.TokenResponse> response =
                    restTemplate.getForEntity(url, MetaDTO.TokenResponse.class);

            if (response.getBody() == null) {
                throw new AdApiHandler(AdApiErrorCode.TOKEN_RESPONSE_EMPTY);
            }
            log.info("[META] Long-lived 토큰 발급 - expiresIn={}초", response.getBody().expiresIn());

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("[META] Long-lived 토큰 교환 실패 - body={}", e.getResponseBodyAsString());
            throw new AdApiHandler(AdApiErrorCode.TOKEN_EXCHANGE_FAILED);
        }
    }


    // ============================
    // private: 광고계정 조회 + PlatformConnection 저장
    // ============================

    //액세스 토큰으로 Meta 광고계정 목록 조회 (PlatformAccount 저장 전 실제 정보 확보)
    private MetaDTO.AdAccountListResponse fetchAdAccounts(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        String url = UriComponentsBuilder
                .fromHttpUrl(metaAdConfig.getGraphApiBaseUrl() + "/me/adaccounts")
                .queryParam("fields", ACCOUNT_FIELDS)
                .queryParam("access_token", accessToken)
                .build()
                .toUriString();
        try {
            ResponseEntity<MetaDTO.AdAccountListResponse> response =
                    restTemplate.getForEntity(url, MetaDTO.AdAccountListResponse.class);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("[META] 광고계정 조회 실패 - body={}", e.getResponseBodyAsString());
            throw new AdApiHandler(AdApiErrorCode.AD_ACCOUNT_FETCH_FAILED);
        }
    }


    // 토큰 암호화 + 실제 광고계정 조회 + PlatformAccount/PlatformConnection 저장
    private void savePlatformConnection(MetaDTO.TokenResponse token, Organization org, Long userId, MetaDTO.AdAccountListResponse adAccounts) {
        try {

            String encAccessToken = new String(aesUtil.encryptAES(token.accessToken()), StandardCharsets.UTF_8);

            // 1. 유저 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

            // 2. 연동할 계정이 비어있는지 검사
            if (adAccounts == null || adAccounts.data() == null || adAccounts.data().isEmpty()) {
                throw new AdApiHandler(AdApiErrorCode.NO_LINKABLE_AD_ACCOUNT);
            }

            // 페이스북에서 가져온 모든 광고 계정을 순회하며 각각 저장
            for (MetaDTO.AdAccount metaAccount : adAccounts.data()) {

                // 통화 (Currency) 파싱
                Currency dynamicCurrency;
                try {
                    // API에서 넘어온 "KRW", "USD" 등을 대문자 변환 후 Enum 매칭
                    dynamicCurrency = (metaAccount.currency() != null)
                            ? Currency.valueOf(metaAccount.currency().toUpperCase())
                            : Currency.KRW;
                } catch (Exception e) {
                    // Enum에 존재하지 않는 미지원 통화 환율일 경우 기본값
                    dynamicCurrency = Currency.KRW;
                }

                Currency finalDynamicCurrency = dynamicCurrency;

                // 타임존 (Timezone) 파싱
                Timezone dynamicTimezone;
                try {
                    // 페이스북 문자열 예: "Asia/Seoul"
                    if (metaAccount.timezoneName() != null) {
                        String timeZoneString = metaAccount.timezoneName().toUpperCase();
                        // 문자열 안에 "SEOUL"이 포함되어 있으면 SEOUL
                        if (timeZoneString.contains("SEOUL")) {
                            dynamicTimezone = Timezone.SEOUL;
                        } else {
                            // 그 외의 해외 국가이거나 "ASIA"만 있을 경우 기본값 ASIA
                            dynamicTimezone = Timezone.ASIA;
                        }
                    } else {
                        dynamicTimezone = Timezone.ASIA;
                    }
                } catch (Exception e) {
                    dynamicTimezone = Timezone.ASIA; // 에러 안전망
                }

                Timezone finalDynamicTimezone = dynamicTimezone;

                PlatformAccount account = platformAccountRepository.findByExternalAccountIdAndProvider(metaAccount.id(), Provider.META)
                        .orElseGet(() -> platformAccountRepository.save(
                                PlatformAccount.builder()
                                        .externalAccountId(metaAccount.id())
                                        .accountName(metaAccount.name())
                                        .provider(Provider.META)
                                        .currency(finalDynamicCurrency)
                                        .timezone(finalDynamicTimezone)
                                        .status(PlatformStatus.ACTIVE)
                                        .organization(org).build()));

                PlatformConnection connection = PlatformConnection.builder()
                        .authType(AuthType.OAUTH)
                        .authIdentifier(encAccessToken)
                        .tokenExpireAt(LocalDateTime.now().plusSeconds(token.expiresIn() != null ? token.expiresIn() : 5184000L))
                        .user(user)
                        .platformAccount(account)
                        .build();

                platformConnectionRepository.save(connection);
            }
        } catch (UserHandler | AdApiHandler e) {
            // 이미 발생한 커스텀 예외는 한 번 더 감싸지지 않도록 그대로 통과
            throw e;
        } catch (Exception e) {
            // 그 외의 모든 알 수 없는 암호화/DB 인서트 에러 처리
            throw new AdApiHandler(AdApiErrorCode.CONNECTION_SAVE_FAILED);
        }
    }

    /**
     * 다음 페이지를 위한 Cursor 값을 안전하게 가져오는 유틸 메서드
     */
    private String getNextCursor(MetaDTO.Paging paging) {
        // next 프로퍼티 값이 null이 아닌 경우에만 다음 페이지가 있다고 판단
        if (paging == null || paging.next() == null || paging.cursors() == null) {
            return null;
        }
        String after = paging.cursors().after();
        return (after != null && !after.trim().isEmpty()) ? after : null;
    }

    // 내부 레코드 - Transactional 내부에서 영속성 객체를 미리 뽑아오기 위한 클래스
    private record PlatformSessionContext(
            Long connId,
            String adAccountId,
            Organization org,
            PlatformAccount platformAccount)
    {}

}
