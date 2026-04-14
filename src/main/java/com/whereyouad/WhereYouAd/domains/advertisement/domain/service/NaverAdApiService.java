package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.global.utils.AdApiAuthUtil;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.client.NaverClient;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAdApiService {

    private final PlatformConnectionRepository connectionRepository;
    private final AdApiAuthUtil adApiAuthUtil;
    private final NaverClient naverClient;

    // 캠페인 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.CampaignResponse> getCampaigns(Long connectionId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/campaigns"));
            return naverClient.getCampaigns(headers);
        } catch (Exception e) {
            log.error("[NAVER] 캠페인 조회 실패 - orgId={}", orgId, e);
            throw new RuntimeException("네이버 캠페인 조회 실패", e);
        }
            log.error("[NAVER] 캠페인 조회 실패 - connectionId={}", connectionId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_CAMPAIGN_FETCH_FAILED);
        }
    }

    // 광고 그룹 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.AdGroupResponse> getAdGroups(Long connectionId, String nccCampaignId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/adgroups"));
            return naverClient.getAdGroups(headers, nccCampaignId);
        } catch (Exception e) {
            log.error("[NAVER] 광고 그룹 조회 실패 - connectionId={}, campaignId={}", connectionId, nccCampaignId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_AD_GROUP_FETCH_FAILED);
        }
    }

    // 광고(소재) 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.AdResponse> getAds(Long connectionId, String nccAdgroupId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/ads"));
            return naverClient.getAds(headers, nccAdgroupId);
        } catch (Exception e) {
            log.error("[NAVER] 광고 소재 조회 실패 - connectionId={}, adGroupId={}", connectionId, nccAdgroupId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_AD_CONTENT_FETCH_FAILED);
        }
    }

    // 키워드 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.KeywordResponse> getKeywords(Long connectionId, String nccAdgroupId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/keywords"));
            return naverClient.getKeywords(headers, nccAdgroupId);
        } catch (Exception e) {
            log.error("[NAVER] 키워드 조회 실패 - connectionId={}, adGroupId={}", connectionId, nccAdgroupId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_KEYWORD_FETCH_FAILED);
        }
    }

    }

    // private 내부 메서드

    // orgId 기반으로 NAVER Connection을 찾아 반환
    private PlatformConnection resolveNaverConnection(Long orgId) {
        List<PlatformConnection> connections = connectionRepository
                .findByPlatformAccount_Organization_IdAndPlatformAccount_Provider(orgId, Provider.NAVER);
        if (connections.isEmpty()) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND);
        }
        return connections.get(0);
    }
}
