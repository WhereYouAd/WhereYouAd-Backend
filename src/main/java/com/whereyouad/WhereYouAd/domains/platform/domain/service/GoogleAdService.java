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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    public GoogleAdResponse.GoogleAdCreateReponse createAllAdInfos(String customerId, PlatformConnection platformConnection, AdAuthRequest request) {
        syncAdCampaigns(customerId, platformConnection, request);
        syncAdGroups(customerId, platformConnection, request);
        syncAdContents(customerId, platformConnection, request);
        syncMetricFacts(customerId, platformConnection, request);

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
                    // TODO: 연관된 AdCampaign을 DB에서 조회하여 매핑 필요 (현재는 null 전달)
                    AdCampaign adCampaign = null;

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
                    // TODO: 연관된 AdGroup을 DB에서 조회하여 매핑 필요 (현재는 null 전달)
                    AdGroup adGroup = null;

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
                    // TODO: 연관 엔티티들을 DB에서 조회하여 매핑 필요 (현재는 null 전달)
                    AdCampaign adCampaign = null;
                    AdContent adContent = null;
                    Project project = null;

                    MetricFact metricFact = googleConverter.toMetricFact(result, adCampaign, adContent, project, platformConnection.getPlatformAccount());
                    metricFactRepository.save(metricFact);
                }
            }
        } catch (Exception e) {
            log.error("통계 데이터(MetricFact) JSON 파싱 및 저장 실패", e);
        }
    }
}
