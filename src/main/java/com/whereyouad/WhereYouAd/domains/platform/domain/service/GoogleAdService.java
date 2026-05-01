package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.GoogleAdResponse;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.google.GoogleAdWebClient;
import com.whereyouad.WhereYouAd.infrastructure.client.google.converter.GoogleConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.google.dto.GoogleDTO;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public GoogleAdResponse.GoogleAdCreateReponse createAllAdInfos(Long userId) {
        List<PlatformConnection> connections = platformConnectionRepository.findByUser_IdAndPlatformAccount_Provider(userId, Provider.GOOGLE);
        
        AdAuthRequest emptyRequest = AdAuthRequest.empty();

        for (PlatformConnection connection : connections) {
            String customerId = connection.getPlatformAccount().getExternalAccountId();
            syncAdCampaigns(customerId, connection, emptyRequest);
            syncAdGroups(customerId, connection, emptyRequest);
            syncAdContents(customerId, connection, emptyRequest);
            syncMetricFacts(customerId, connection, emptyRequest);
        }

        return new GoogleAdResponse.GoogleAdCreateReponse("구글 광고 데이터 연동 완료");
    }

    private void syncAdCampaigns(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchAllCampaigns(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        try {
            GoogleDTO.AdCampaignResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.AdCampaignResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.AdCampaignResult result : response.getResults()) {
                    AdCampaign adCampaign = googleConverter.toAdCampaign(result, platformConnection.getPlatformAccount());
                    adCampaignRepository.save(adCampaign);
                }
            }
        } catch (Exception e) {
            log.error("캠페인 JSON 파싱 및 저장 실패", e);
        }
    }

    private void syncAdGroups(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchAllAdGroups(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        try {
            GoogleDTO.AdGroupResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.AdGroupResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.AdGroupResult result : response.getResults()) {
                    String externalCampaignId = result.getCampaign().getId();
                    AdCampaign adCampaign = adCampaignRepository.findByExternalCampaignId(result.getCampaign().getId()).orElse(null);

                    if (adCampaign == null) {
                        log.warn("연관된 AdCampaign을 찾을 수 없어 AdGroup 저장을 건너뜁니다. (ExternalCampaignId: {})", externalCampaignId);
                        continue;
                    }

                    AdGroup adGroup = googleConverter.toAdGroup(result, adCampaign);
                    adGroupRepository.save(adGroup);
                }
            }
        } catch (Exception e) {
            log.error("광고 그룹 JSON 파싱 및 저장 실패", e);
        }
    }

    private void syncAdContents(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchAllAdContents(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        try {
            GoogleDTO.AdContentResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.AdContentResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.AdContentResult result : response.getResults()) {
                    String externalGroupId = result.getAdGroup().getId();
                    AdGroup adGroup = adGroupRepository.findByExternalGroupId(result.getAdGroup().getId()).orElse(null);

                    if (adGroup == null) {
                        log.warn("연관된 AdGroup을 찾을 수 없어 AdContent 저장을 건너뜁니다. (ExternalGroupId: {})", externalGroupId);
                        continue;
                    }

                    AdContent adContent = googleConverter.toAdContent(result, adGroup);
                    adContentRepository.save(adContent);
                }
            }
        } catch (Exception e) {
            log.error("광고 소재 JSON 파싱 및 저장 실패", e);
        }
    }

    private void syncMetricFacts(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        String jsonResponse = googleAdWebClient.searchGoogleAdsData(customerId, platformConnection, request).block();

        if (jsonResponse == null || jsonResponse.isBlank()) return;

        try {
            GoogleDTO.MetricFactResponse response = objectMapper.readValue(jsonResponse, GoogleDTO.MetricFactResponse.class);

            if (response != null && response.getResults() != null) {
                for (GoogleDTO.MetricFactResult result : response.getResults()) {
                    String googleCampaignId = result.getCampaign() != null ? result.getCampaign().getId() : null;
                    String googleAdId = result.getAdGroupAd() != null && result.getAdGroupAd().getAd() != null
                            ? result.getAdGroupAd().getAd().getId() : null;

                    // TODO: 연관 엔티티들을 DB에서 조회하여 매핑 필요 (현재는 null 전달)
                    AdCampaign adCampaign = googleCampaignId != null
                            ? adCampaignRepository.findByExternalCampaignId(googleCampaignId).orElse(null)
                            : null;

                    AdContent adContent = googleAdId != null
                            ? adContentRepository.findByExternalAdId(googleAdId).orElse(null)
                            : null;

                    // Project는 AdCampaign 연관관계를 통해 획득
                    Project project = adCampaign != null ? adCampaign.getProject() : null;

                    MetricFact metricFact = googleConverter.toMetricFact(result, adCampaign, adContent, project, platformConnection.getPlatformAccount());
                    metricFactRepository.save(metricFact);
                }
            }
        } catch (Exception e) {
            log.error("통계 데이터(MetricFact) JSON 파싱 및 저장 실패", e);
        }
    }
}
