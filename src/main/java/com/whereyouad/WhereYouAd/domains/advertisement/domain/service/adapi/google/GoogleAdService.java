package com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.GoogleAdResponse;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.infrastructure.client.google.GoogleAdWebClient;
import com.whereyouad.WhereYouAd.infrastructure.client.google.converter.GoogleConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.google.dto.GoogleDTO;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAdService {

    private final GoogleAdWebClient googleAdWebClient;

    private final AdCampaignRepository adCampaignRepository;

    private final AdGroupRepository adGroupRepository;

    private final AdContentRepository adContentRepository;

    private final MetricFactRepository metricFactRepository;

    private final GoogleConverter googleConverter;

    private final ObjectMapper objectMapper;

    private final PlatformConnectionRepository platformConnectionRepository;

    @Transactional
    public GoogleAdResponse.GoogleAdCreateResponse createAllAdInfos(Long userId) {
        List<PlatformConnection> connections = platformConnectionRepository.findByUser_IdAndPlatformAccount_Provider(userId, Provider.GOOGLE);
        
        AdAuthRequest emptyRequest = AdAuthRequest.empty();

        for (PlatformConnection connection : connections) {
            String customerId = connection.getPlatformAccount().getExternalAccountId();
            try {
                syncAdCampaigns(customerId, connection, emptyRequest);
                syncAdGroups(customerId, connection, emptyRequest);
                syncAdContents(customerId, connection, emptyRequest);
                syncMetricFacts(customerId, connection, emptyRequest);
            } catch (Exception e) {
                log.warn("계정 [{}]의 구글 광고 데이터 연동 중 오류가 발생했습니다. (광고가 없는 계정이거나 권한 문제일 수 있습니다.) 사유: {}", customerId, e.getMessage());
            }
        }

        return new GoogleAdResponse.GoogleAdCreateResponse("구글 광고 데이터 연동 완료");
    }

    @Transactional
    public void syncAllGoogleAdsData() {
        List<PlatformConnection> connections = platformConnectionRepository.findByPlatformAccount_Provider(Provider.GOOGLE);
        AdAuthRequest emptyRequest = AdAuthRequest.empty();

        for (PlatformConnection connection : connections) {
            String customerId = connection.getPlatformAccount().getExternalAccountId();
            try {
                syncAdCampaigns(customerId, connection, emptyRequest);
                syncAdGroups(customerId, connection, emptyRequest);
                syncAdContents(customerId, connection, emptyRequest);
                syncMetricFacts(customerId, connection, emptyRequest);
            } catch (Exception e) {
                log.warn("계정 [{}]의 구글 광고 데이터 동기화 중 오류가 발생했습니다. 사유: {}", customerId, e.getMessage());
            }
        }
    }

    private void syncAdCampaigns(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchAllCampaigns(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        PlatformAccount currentAccount = platformConnection.getPlatformAccount(); // 현재 연동된 계정

        try {
            GoogleDTO.AdCampaignResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.AdCampaignResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.AdCampaignResult result : response.getResults()) {
                    String externalId = result.getCampaign().getId();
                    Optional<AdCampaign> existing = adCampaignRepository.findByPlatformAccountAndExternalCampaignId(currentAccount, externalId);

                    AdCampaign newCampaign = googleConverter.toAdCampaign(result, currentAccount);

                    if (existing.isPresent()) {
                        existing.get().update(
                                newCampaign.getName(),
                                newCampaign.getStatus(),
                                newCampaign.getBudget(),
                                newCampaign.getGoal(),
                                newCampaign.getStartDate(),
                                newCampaign.getEndDate(),
                                newCampaign.getDescription()
                        );
                    } else {
                        adCampaignRepository.save(newCampaign);
                    }
                }
            }
        } catch (Exception e) {
            log.error("캠페인 JSON 파싱 및 저장 실패", e);
            throw new AdApiHandler(AdApiErrorCode.GOOGLE_DATA_SYNC_FAILED);
        }
    }

    private void syncAdGroups(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchAllAdGroups(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        PlatformAccount currentAccount = platformConnection.getPlatformAccount(); // 현재 연동된 계정

        try {
            GoogleDTO.AdGroupResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.AdGroupResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.AdGroupResult result : response.getResults()) {
                    String externalCampaignId = result.getCampaign().getId();
                    AdCampaign adCampaign = adCampaignRepository.findByPlatformAccountAndExternalCampaignId(currentAccount, externalCampaignId).orElse(null);

                    if (adCampaign == null) continue;

                    String externalGroupId = result.getAdGroup().getId();
                    Optional<AdGroup> existing = adGroupRepository.findByAdCampaignAndExternalGroupId(adCampaign, externalGroupId);
                    AdGroup newGroup = googleConverter.toAdGroup(result, adCampaign);

                    if (existing.isPresent()) {
                        existing.get().update(
                                newGroup.getName(), 
                                newGroup.getStatus(), 
                                existing.get().getTargetingInfo()
                        );
                    } else {
                        adGroupRepository.save(newGroup);
                    }
                }
            }
        } catch (Exception e) {
            log.error("광고 그룹 JSON 파싱 및 저장 실패", e);
            throw new AdApiHandler(AdApiErrorCode.GOOGLE_DATA_SYNC_FAILED);
        }
    }

    private void syncAdContents(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchAllAdContents(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        PlatformAccount currentAccount = platformConnection.getPlatformAccount();

        try {
            GoogleDTO.AdContentResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.AdContentResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.AdContentResult result : response.getResults()) {
                    String externalGroupId = result.getAdGroup().getId();
                    AdGroup adGroup = adGroupRepository.findByAdCampaign_PlatformAccountAndExternalGroupId(currentAccount, externalGroupId).orElse(null);
                    if (adGroup == null) continue;

                    String externalAdId = result.getAdGroupAd().getAd().getId();
                    Optional<AdContent> existing = adContentRepository.findByAdGroupAndExternalAdId(adGroup, externalAdId);                    AdContent newContent = googleConverter.toAdContent(result, adGroup);

                    if (existing.isPresent()) {
                        existing.get().update(
                                newContent.getName(),
                                newContent.getType(),
                                newContent.getStatus(),
                                existing.get().getDescription(),
                                newContent.getTrackingUrl(),
                                newContent.getLandingUrl()
                        );
                    } else {
                        adContentRepository.save(newContent);
                    }
                }
            }
        } catch (Exception e) {
            log.error("광고 소재 JSON 파싱 및 저장 실패", e);
            throw new AdApiHandler(AdApiErrorCode.GOOGLE_DATA_SYNC_FAILED);
        }
    }

    private void syncMetricFacts(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchGoogleAdsData(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        PlatformAccount currentAccount = platformConnection.getPlatformAccount();

        try {
            GoogleDTO.MetricFactResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.MetricFactResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.MetricFactResult result : response.getResults()) {
                    String googleCampaignId = result.getCampaign() != null ? result.getCampaign().getId() : null;
                    String googleAdId = result.getAdGroupAd() != null && result.getAdGroupAd().getAd() != null ? result.getAdGroupAd().getAd().getId() : null;

                    AdCampaign adCampaign = googleCampaignId != null ?
                            adCampaignRepository.findByPlatformAccountAndExternalCampaignId(currentAccount, googleCampaignId).orElse(null) : null;

                    AdContent adContent = googleAdId != null ?
                            adContentRepository.findByAdGroup_AdCampaign_PlatformAccountAndExternalAdId(currentAccount, googleAdId).orElse(null) : null;

                    Project project = adCampaign != null ? adCampaign.getProject() : null;

                    MetricFact newFact = googleConverter.toMetricFact(result, adCampaign, adContent, project, platformConnection.getPlatformAccount());

                    if (adContent != null && newFact.getTimeBucket() != null) {
                        Optional<MetricFact> existing = metricFactRepository.findByPlatformAccount_IdAndAdContent_IdAndTimeBucket(
                                platformConnection.getPlatformAccount().getId(), adContent.getId(), newFact.getTimeBucket());

                        if (existing.isPresent()) {
                            existing.get().update(
                                    newFact.getImpressions(),
                                    newFact.getClicks(),
                                    newFact.getConversions(),
                                    newFact.getSpend(),
                                    newFact.getRevenue()
                            );
                        } else {
                            metricFactRepository.save(newFact);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("통계 데이터(MetricFact) JSON 파싱 및 저장 실패", e);
            throw new AdApiHandler(AdApiErrorCode.GOOGLE_DATA_SYNC_FAILED);
        }
    }
}
