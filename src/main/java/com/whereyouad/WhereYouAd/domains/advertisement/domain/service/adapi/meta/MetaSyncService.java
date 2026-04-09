package com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaSyncService {

    /**
     * Meta Graph API와의 실질적인 데이터 통신 및 동기화 진행 Service
     *
     * - 커넥션 풀 고갈 방지를 위해 DB 트랜잭션과 분리
     * - 캠페인, 광고세트, 광고, 그리고 성과 지표(MetricFact)를 수집.
     * - 조회된 데이터를 자체 가공한 뒤, 실제 DB 저장은 MetaUpsertService에게 위임.
     */

    //Repository
    private final PlatformConnectionRepository platformConnectionRepository;
    private final OrgRepository orgRepository;
    private final PlatformAccountRepository platformAccountRepository;

    //Feign Client, Util, Service
    private final MetaClient metaClient;
    private final AdApiAuthUtil adApiAuthUtil;
    private final MetaUpsertService metaUpsertService;

    private final TransactionTemplate transactionTemplate;

    // === Graph API fields 상수 ===
    private static final String CAMPAIGN_FIELDS =
            "id,name,status,objective,daily_budget,lifetime_budget,start_time,stop_time";
    private static final String ADSET_FIELDS =
            "id,campaign_id,name,status,daily_budget,targeting,billing_event,optimization_goal";
    private static final String AD_FIELDS =
            "id,adset_id,name,status,creative{id,body,object_type}";
    private static final String INSIGHT_FIELDS =
            "ad_id,campaign_id,adset_id,impressions,clicks,spend,actions,date_start,date_stop";

    // ============================
    // 1. 전체 동기화 오케스트레이션
    // ============================
    public MetaResponse.MetaSyncSummary syncAll(Long orgId, String startDate, String endDate) {
        // Lazy Loading 방지를 위해 트랜잭션 template 사용
        List<PlatformSessionContext> contexts = transactionTemplate.execute(status -> {
            List<PlatformConnection> connections = resolveMetaConnections(orgId);
            return connections.stream()
                    .map(conn -> {
                        PlatformAccount pAccount = conn.getPlatformAccount();
                        return new PlatformSessionContext(
                                conn.getId(),
                                pAccount.getExternalAccountId(),
                                pAccount.getOrganization().getId(),
                                pAccount.getId());
                    })
                    .toList();
        });

        if (contexts == null || contexts.isEmpty()) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND);
        }

        int totalCampaigns = 0, totalAdSets = 0, totalAds = 0, totalMetrics = 0;
        List<String> failedAccountIds = new ArrayList<>();

        for (PlatformSessionContext context : contexts) {

            try {
                MetaResponse.MetaSyncSummary partial = syncSingleAccount(context, startDate, endDate);
                totalCampaigns += partial.adCampaignCount();
                totalAdSets += partial.adGroupCount();
                totalAds += partial.adContentCount();
                totalMetrics += partial.metricCount();

            } catch (Exception e) {
                log.error("[META] 광고계정 동기화 실패 (adAccountId: {}) - {}", context.adAccountId(), e.getMessage(), e);
                failedAccountIds.add(context.adAccountId());
            }
        }

        // 모든 계정이 실패한 경우 → 호출자(수동 동기화 API 등)가 성공으로 오인하지 않도록 예외 전파
        if (failedAccountIds.size() == contexts.size()) {
            log.error("[META] 전체 동기화 실패 — 모든 계정({}) 동기화 실패", contexts.size());
            throw new AdApiHandler(AdApiErrorCode.SYNC_DATA_PROCESSING_ERROR);
        }

        log.info("[META] 전체 동기화 완료 — 계정수:{}, 실패:{}, 캠페인:{}, 광고세트:{}, 광고:{}, 지표:{}",
                contexts.size(), failedAccountIds.size(), totalCampaigns, totalAdSets, totalAds, totalMetrics);
        return MetaConverter.toSyncSummary(totalCampaigns, totalAdSets, totalAds, totalMetrics, failedAccountIds);
    }

    // ============================
    // 2. 단일 계정 상세 동기화 로직
    // ============================
    private MetaResponse.MetaSyncSummary syncSingleAccount(PlatformSessionContext context, String startDate, String endDate) {
        Map<String, String> authData;

        try {
            authData = adApiAuthUtil.generateAuthHeaders(context.connId(), AdAuthRequest.empty());
        } catch (Exception e) {
            log.error("Meta 인증 데이터 생성 실패 (connId: {})", context.connId(), e);
            throw new AdApiHandler(AdApiErrorCode.INVALID_API_CREDENTIALS);
        }

        String accessToken = authData.get("access_token");
        int campaignCount = 0, adSetCount = 0, adCount = 0, metricCount = 0;

        Organization orgEntity = orgRepository.findById(context.orgId())
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));
        PlatformAccount pAccountEntity = platformAccountRepository.findById(context.platformAccountId())
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));
        
        try {
            // ─── 캠페인 동기화 ───
            Map<String, AdCampaign> campaignMap = new HashMap<>();
            String campaignCursor = null;

            do {
                MetaDTO.CampaignListResponse campaignsResp =
                        metaClient.getCampaigns(accessToken, context.adAccountId(), CAMPAIGN_FIELDS, campaignCursor);

                if (campaignsResp == null || campaignsResp.data() == null || campaignsResp.data().isEmpty()) break;

                // 페이지 단위 배치 UPSERT — 한 페이지 전체를 하나의 트랜잭션으로 처리
                Map<String, AdCampaign> pageResult =
                        metaUpsertService.upsertCampaigns(campaignsResp.data(), orgEntity, pAccountEntity);
                campaignMap.putAll(pageResult);
                campaignCount += pageResult.size();

                campaignCursor = getNextCursor(campaignsResp.paging());

            } while (campaignCursor != null);

            if (campaignMap.isEmpty()) return MetaConverter.toSyncSummary(0, 0, 0, 0);

            // ─── 광고세트(AdSet) 동기화 ───
            Map<String, AdGroup> adSetMap = new HashMap<>();
            String adSetCursor = null;

            do {
                MetaDTO.AdSetListResponse adSetsResp =
                        metaClient.getAdSets(accessToken, context.adAccountId(), ADSET_FIELDS, adSetCursor);

                if (adSetsResp == null || adSetsResp.data() == null || adSetsResp.data().isEmpty()) break;

                // 페이지 단위 배치 UPSERT — campaignMap을 넘겨 부모 스코프 적용
                Map<String, AdGroup> pageResult =
                        metaUpsertService.upsertAdGroups(adSetsResp.data(), campaignMap);
                adSetMap.putAll(pageResult);
                adSetCount += pageResult.size();

                adSetCursor = getNextCursor(adSetsResp.paging());

            } while (adSetCursor != null);

            // ─── 광고(Ad) 동기화 ───
            Map<String, AdContent> adMap = new HashMap<>();

            if (!adSetMap.isEmpty()) {
                String adCursor = null;
                do {
                    MetaDTO.AdListResponse adsResp =
                            metaClient.getAds(accessToken, context.adAccountId(), AD_FIELDS, adCursor);

                    if (adsResp == null || adsResp.data() == null || adsResp.data().isEmpty()) break;

                    // 페이지 단위 배치 UPSERT — adSetMap을 넘겨 부모 스코프 적용
                    Map<String, AdContent> pageResult =
                            metaUpsertService.upsertAdContents(adsResp.data(), adSetMap);
                    adMap.putAll(pageResult);
                    adCount += pageResult.size();

                    adCursor = getNextCursor(adsResp.paging());

                } while (adCursor != null);
            }

            // ─── 인사이트(MetricFact) 동기화 ───
            if (startDate != null && endDate != null && !adMap.isEmpty()) {

                String timeRange = "{\"since\":\"" + startDate + "\",\"until\":\"" + endDate + "\"}";
                String insightCursor = null;
                do {
                    MetaDTO.InsightListResponse insightsResp =
                            metaClient.getInsights(accessToken, context.adAccountId(), INSIGHT_FIELDS,
                                    "ad", timeRange, "1", insightCursor);

                    if (insightsResp == null || insightsResp.data() == null || insightsResp.data().isEmpty()) break;

                    // 페이지 단위 배치 UPSERT — adMap, campaignMap 전달로 조회 스코프 유지
                    metricCount += metaUpsertService.upsertMetricFacts(
                            insightsResp.data(), adMap, campaignMap, pAccountEntity);

                    insightCursor = getNextCursor(insightsResp.paging());

                } while (insightCursor != null);
            }

        } catch (FeignException e) {

            log.error("[META] API 통신 오류 - adAccountId={}, status={}, body={}",
                    context.adAccountId(), e.status(), e.contentUTF8());
            throw new AdApiHandler(AdApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);
        } catch (Exception e) {

            log.error("[META] 데이터 동기화 알 수 없는 오류 - adAccountId={}", context.adAccountId(), e);
            throw new AdApiHandler(AdApiErrorCode.SYNC_DATA_PROCESSING_ERROR);
        }

        log.info("[META] 계정 동기화 완료 - ID:{}, 캠페인:{}, 광고세트:{}, 광고:{}, 지표:{}",
                context.adAccountId(), campaignCount, adSetCount, adCount, metricCount);
        return MetaConverter.toSyncSummary(campaignCount, adSetCount, adCount, metricCount);
    }

    // ============================
    // 3. 내부 유틸 메서드
    // ============================
    private List<PlatformConnection> resolveMetaConnections(Long orgId) {

        List<PlatformConnection> connections = platformConnectionRepository
                .findByPlatformAccount_Organization_IdAndPlatformAccount_Provider(orgId, Provider.META);

        if (connections.isEmpty()) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND);
        }

        return connections.stream()
                .collect(Collectors.groupingBy(
                        conn -> conn.getPlatformAccount().getId(),
                        Collectors.maxBy(Comparator.comparing(PlatformConnection::getId))
                ))
                .values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private String getNextCursor(MetaDTO.Paging paging) {
        if (paging == null || paging.next() == null || paging.cursors() == null) {
            return null;
        }
        String after = paging.cursors().after();
        return (after != null && !after.trim().isEmpty()) ? after : null;
    }

    private record PlatformSessionContext(
            Long connId,
            String adAccountId,
            Long orgId,
            Long platformAccountId)
    {}
}
