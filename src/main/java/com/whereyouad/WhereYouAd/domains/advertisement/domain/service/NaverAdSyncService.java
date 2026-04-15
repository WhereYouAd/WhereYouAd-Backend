package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.converter.NaverConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;

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

    // 작은 단위로 트랜잭션을 끊기 위한 템플릿
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate txTemplate;

    @PostConstruct
    public void init() {
        this.txTemplate = new TransactionTemplate(transactionManager);

        // 광고 하나/광고그룹 하나 단위로 독립 트랜잭션 처리
        // 일부 실패해도 전체 배치가 다 롤백되지 않게 하기 위함
        this.txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    // 광고 정보(캠페인/광고그룹/광고소재) 메타데이터 upsert
    public AdvertisementResponse.NaverMetadataSyncResponse syncAllMetadata(Long connectionId) {
        log.info("NAVER 광고 동기화 시작 - connectionId: {}", connectionId);

        // 1. connectionId로 연동 계정 조회
        PlatformConnection connection = platformConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));

        // 2. 연결 계정 기준으로 네이버 캠페인 목록 조회
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
                                    connection.getPlatformAccount())
                            .map(existing -> {
                                // 있으면 업데이트
                                NaverConverter.updateAdCampaign(existing, campaignDto);
                                return adCampaignRepository.save(existing);
                            })
                            .orElseGet(() -> adCampaignRepository.save(
                                    // 없으면 신규 생성
                                    NaverConverter.toAdCampaignEntity(campaignDto,
                                            connection.getPlatformAccount().getOrganization(),
                                            connection.getPlatformAccount())));
                    return entity;
                });

                // 4. 캠페인 저장 후 하위 광고 그룹 동기화 호출
                if (adCampaign != null) {
                    campaignCount++;
                    int[] subCounts = syncAdGroups(connectionId, connection.getPlatformAccount(), adCampaign);
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
}
