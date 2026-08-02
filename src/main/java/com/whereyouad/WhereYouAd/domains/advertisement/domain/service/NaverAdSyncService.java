package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.NaverAdErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.converter.NaverConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAdSyncService {

    private final NaverAdApiService naverAdApiService;
    private final AdCampaignRepository adCampaignRepository;
    private final AdGroupRepository adGroupRepository;
    private final AdContentRepository adContentRepository;
    private final MetricFactRepository metricFactRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final RedisUtil redisUtil;

    // 작은 단위로 트랜잭션을 끊기 위한 템플릿
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate txTemplate;

    private static final long SYNC_COOLDOWN_SECONDS = 60L;
    private static final long SYNC_LOCK_TTL_SECONDS = 300L;

    @PostConstruct
    public void init() {
        this.txTemplate = new TransactionTemplate(transactionManager);

        // 광고 하나/광고그룹 하나 단위로 독립 트랜잭션 처리
        // 일부 실패해도 전체 배치가 다 롤백되지 않게 하기 위함
        this.txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    // 광고 정보(캠페인/광고그룹/광고소재) 메타데이터 upsert
    public AdvertisementResponse.NaverMetadataSyncResponse syncAllMetadata(Long userId, Long connectionId) {
        PlatformConnection connection = getConnection(connectionId);
        validateOrganizationMembership(userId, connection.getPlatformAccount().getOrganization().getId());
        validateNaverProvider(connection);
        return syncAllMetadata(connectionId, connection);
    }

    AdvertisementResponse.NaverMetadataSyncResponse syncAllMetadata(Long connectionId) {
        return syncAllMetadata(connectionId, getNaverConnection(connectionId));
    }

    private AdvertisementResponse.NaverMetadataSyncResponse syncAllMetadata(
            Long connectionId, PlatformConnection connection) {
        log.info("NAVER 광고 동기화 시작 - connectionId: {}", connectionId);

        // 2. 연결 계정 및 플랫폼 계정 정보 추출 (Lazy Loading 방지를 위해 미리 조회)
        PlatformAccount platformAccount = connection.getPlatformAccount();
        log.info("NAVER 광고 플랫폼 계정 확인: {}", platformAccount.getProvider()); // 강제 초기화 유도

        // 3. 연결 계정 기준으로 네이버 캠페인 목록 조회
        List<NaverDTO.CampaignResponse> campaigns = naverAdApiService.getCampaigns(connectionId);

        int campaignCount = 0;
        int groupCount = 0;
        int contentCount = 0;

        for (NaverDTO.CampaignResponse campaignDto : campaigns) {
            try {
                AdCampaign adCampaign = txTemplate.execute(status -> {
                    // 3. 같은 platformAccount 범위(NAVER) 안에서만 캠페인 조회
                    AdCampaign entity = adCampaignRepository
                            .findByExternalCampaignIdAndPlatformAccount(campaignDto.nccCampaignId(),
                                    platformAccount)
                            .map(existing -> {
                                // 있으면 업데이트
                                NaverConverter.updateAdCampaign(existing, campaignDto);
                                return adCampaignRepository.save(existing);
                            })
                            .orElseGet(() -> adCampaignRepository.save(
                                    // 없으면 신규 생성
                                    NaverConverter.toAdCampaignEntity(campaignDto,
                                            platformAccount.getOrganization(),
                                            platformAccount)));
                    return entity;
                });

                // 4. 캠페인 저장 후 하위 광고 그룹 동기화 호출
                if (adCampaign != null) {
                    campaignCount++;
                    int[] subCounts = syncAdGroups(connectionId, platformAccount, adCampaign);
                    groupCount += subCounts[0];
                    contentCount += subCounts[1];
                }

            } catch (Exception e) {
                // 한 캠페인 실패해도 다음 캠페인은 계속 진행
                log.error("NAVER 광고 캠페인(ID:{}) 동기화 중 오류 발생: {}", campaignDto.nccCampaignId(), e.getMessage());
            }
        }

        log.info("NAVER 광고 동기화 완료 - connectionId: {}, campaigns={}, groups={}, contents={}", 
                connectionId, campaignCount, groupCount, contentCount);
        return new AdvertisementResponse.NaverMetadataSyncResponse(connectionId, campaignCount, groupCount, contentCount);
    }

    private int[] syncAdGroups(Long connectionId,
            PlatformAccount platformAccount,
            AdCampaign adCampaign) {

        // 1. 특정 캠페인 아래 광고그룹 목록 조회
        List<NaverDTO.AdGroupResponse> adGroups = naverAdApiService.getAdGroups(connectionId,
                adCampaign.getExternalCampaignId());

        int count = 0;
        int subContentCount = 0;

        for (NaverDTO.AdGroupResponse groupDto : adGroups) {
            try {
                // 2. 광고그룹에 속한 키워드 목록을 조회해서 targetingInfo 문자열로 압축
                List<NaverDTO.KeywordResponse> keywords = naverAdApiService.getKeywords(connectionId,
                        groupDto.nccAdgroupId());

                log.info("NAVER 광고 그룹 동기화 시도 - groupId={}", groupDto.nccAdgroupId());

                AdGroup adGroup = txTemplate.execute(status -> {

                    // 3. 같은 platformAccount 범위 안에서만 광고그룹 조회
                    return adGroupRepository
                            .findByExternalGroupIdAndPlatformAccount(groupDto.nccAdgroupId(), platformAccount)
                            .map(entity -> {
                                // 있으면 업데이트
                                NaverConverter.updateAdGroup(entity, groupDto, keywords);
                                return adGroupRepository.save(entity);
                            })
                            .orElseGet(() -> adGroupRepository.save(
                                    // 없으면 신규 생성
                                    NaverConverter.toAdGroupEntity(groupDto, adCampaign, keywords)));
                });

                // 4. 광고그룹 저장 후 하위 광고소재 동기화
                if (adGroup != null) {
                    count++;
                    subContentCount += syncAdContents(connectionId, platformAccount, adGroup);
                }

            } catch (Exception e) {
                log.error("NAVER 광고 광고 그룹(ID:{}) 동기화 중 오류 발생: {}", groupDto.nccAdgroupId(), e.getMessage(), e);
            }
        }
        return new int[]{count, subContentCount};
    }

    private int syncAdContents(Long connectionId,
            PlatformAccount platformAccount,
            AdGroup adGroup) {

        // 1. 특정 광고그룹 아래 광고소재 목록 조회
        List<NaverDTO.AdResponse> ads = naverAdApiService.getAds(connectionId, adGroup.getExternalGroupId());

        int count = 0;
        for (NaverDTO.AdResponse adDto : ads) {
            try {
                txTemplate.execute(status -> {

                    // 2. 같은 platformAccount 범위 안에서만 광고소재 조회
                    return adContentRepository.findByExternalAdIdAndPlatformAccount(adDto.nccAdId(), platformAccount)
                            .map(entity -> {
                                // 있으면 업데이트
                                NaverConverter.updateAdContent(entity, adDto);
                                return adContentRepository.save(entity);
                            })
                            .orElseGet(() -> adContentRepository.save(
                                    // 없으면 신규 생성
                                    NaverConverter.toAdContentEntity(adDto, adGroup)));
                });
                count++;
            } catch (Exception e) {
                log.error("NAVER 광고 광고 소재(ID:{}) 동기화 중 오류 발생: {}", adDto.nccAdId(), e.getMessage());
            }
        }
        return count;
    }


    // 일별 기본 지표 MetricFact upsert (/stats 사용)
    public AdvertisementResponse.NaverStatSyncResponse syncBasicStats(
            Long userId, Long connectionId, String statDate) {
        PlatformConnection connection = getConnection(connectionId);
        validateOrganizationMembership(userId, connection.getPlatformAccount().getOrganization().getId());
        validateNaverProvider(connection);
        return syncBasicStats(connectionId, statDate, connection);
    }

    AdvertisementResponse.NaverStatSyncResponse syncBasicStats(Long connectionId, String statDate) {
        return syncBasicStats(connectionId, statDate, getNaverConnection(connectionId));
    }

    private AdvertisementResponse.NaverStatSyncResponse syncBasicStats(
            Long connectionId, String statDate, PlatformConnection connection) {
        log.info("NAVER Basic Stats 동기화 시작 - connectionId: {}, date: {}", connectionId, statDate);

        PlatformAccount platformAccount = connection.getPlatformAccount();
        log.info("NAVER 광고 플랫폼 계정 확인: {}", platformAccount.getProvider());
        List<AdContent> adContents = adContentRepository.findAllByPlatformAccount(platformAccount);

        int processedCount = 0;
        for (AdContent adContent : adContents) {
            if (adContent.getExternalAdId() == null) continue;

            try {
                List<NaverDTO.StatResponse> stats = naverAdApiService.getDailyStats(connectionId,
                        adContent.getExternalAdId(), statDate, statDate);

                if (stats.isEmpty()) {
                    processedCount++;
                    continue;
                }

                NaverDTO.StatResponse stat = stats.get(0);
                long impCnt = stat.impCnt() != null ? stat.impCnt() : 0L;
                long clkCnt = stat.clkCnt() != null ? stat.clkCnt() : 0L;
                BigDecimal salesAmt = stat.salesAmt() != null
                        ? BigDecimal.valueOf(stat.salesAmt()) : BigDecimal.ZERO;
                boolean hasConversionData = stat.ccnt() != null || stat.convAmt() != null;
                long ccnt = stat.ccnt() != null ? stat.ccnt() : 0L;
                BigDecimal convAmt = stat.convAmt() != null
                        ? BigDecimal.valueOf(stat.convAmt()) : BigDecimal.ZERO;

                LocalDateTime dailyTimeBucket = LocalDate.parse(statDate).atStartOfDay();

                txTemplate.execute(status -> {
                    MetricFact dailyFact = metricFactRepository
                            .findByAdContentAndTimeBucketAndGrain(adContent, dailyTimeBucket, Grain.DAILY)
                            .orElseGet(() -> AdvertisementConverter.createMetricFact(
                                    adContent, dailyTimeBucket, Grain.DAILY, Provider.NAVER));
                    dailyFact.updateBasicMetrics(impCnt, clkCnt, salesAmt);
                    if (hasConversionData) {
                        dailyFact.updateConversionMetrics(ccnt, convAmt);
                    }
                    metricFactRepository.save(dailyFact);
                    return null;
                });

                processedCount++;
            } catch (Exception e) {
                log.error("NAVER 광고 소재(ID:{}) 통계(Basic) 동기화 오류: {}", adContent.getExternalAdId(), e.getMessage());
            }
        }

        log.info("NAVER Basic Stats 동기화 완료 - connectionId: {}, date: {}, processed: {}",
                connectionId, statDate, processedCount);
        return new AdvertisementResponse.NaverStatSyncResponse(connectionId, statDate, processedCount);
    }

    // orgId + 날짜 범위 기반 수동 동기화 (분산락 적용)
    public AdvertisementResponse.NaverManualSyncSummary syncAllForOrg(
            Long userId, Long orgId, LocalDate startDate, LocalDate endDate) {

        validateOrganizationMembership(userId, orgId);

        String cooldownKey = "naver:sync:cooldown:" + orgId;
        if (redisUtil.getData(cooldownKey) != null) {
            throw new AdApiHandler(AdApiErrorCode.SYNC_COOLDOWN);
        }

        String lockKey = "naver:sync:lock:" + orgId;
        Boolean acquired = redisUtil.setIfAbsent(lockKey, String.valueOf(System.currentTimeMillis()), SYNC_LOCK_TTL_SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new AdApiHandler(AdApiErrorCode.SYNC_IN_PROGRESS);
        }

        try {
            log.info("NAVER 수동 동기화 시작 - orgId: {}, startDate: {}, endDate: {}", orgId, startDate, endDate);

            List<PlatformConnection> connections = platformConnectionRepository
                    .findByPlatformAccount_Organization_IdAndPlatformAccount_Provider(orgId, Provider.NAVER);

            int totalCampaigns = 0, totalGroups = 0, totalContents = 0, totalMetrics = 0;
            List<Long> failedConnectionIds = new ArrayList<>();

            for (PlatformConnection conn : connections) {
                try {
                    AdvertisementResponse.NaverMetadataSyncResponse metadata = syncAllMetadata(conn.getId());
                    totalCampaigns += metadata.syncedCampaignCount();
                    totalGroups    += metadata.syncedAdGroupCount();
                    totalContents  += metadata.syncedAdContentCount();

                    for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                        String statDate = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        AdvertisementResponse.NaverStatSyncResponse stats = syncBasicStats(conn.getId(), statDate);
                        totalMetrics += stats.processedAdContentCount();
                    }
                } catch (Exception e) {
                    log.warn("[NAVER] 연결 ID {} 동기화 실패", conn.getId(), e);
                    failedConnectionIds.add(conn.getId());
                }
            }

            log.info("NAVER 수동 동기화 완료 - orgId: {}", orgId);
            return new AdvertisementResponse.NaverManualSyncSummary(
                    totalCampaigns, totalGroups, totalContents, totalMetrics, failedConnectionIds
            );
        } finally {
            redisUtil.deleteData(lockKey);
            redisUtil.setDataExpire(cooldownKey, "1", SYNC_COOLDOWN_SECONDS);
        }
    }

    private PlatformConnection getNaverConnection(Long connectionId) {
        PlatformConnection connection = getConnection(connectionId);
        validateNaverProvider(connection);
        return connection;
    }

    private PlatformConnection getConnection(Long connectionId) {
        return platformConnectionRepository.findWithAccountAndOrgById(connectionId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));
    }

    private void validateNaverProvider(PlatformConnection connection) {
        if (connection.getPlatformAccount().getProvider() != Provider.NAVER) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_CONNECTION_NOT_FOUND);
        }
    }

    private void validateOrganizationMembership(Long userId, Long orgId) {
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ORG_MEMBER_NOT_FOUND));
    }

}
