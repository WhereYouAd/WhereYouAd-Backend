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
    public List<NaverDTO.Campaign> getCampaigns(Long orgId) {
        //
        PlatformConnection conn = resolveNaverConnection(orgId);
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    conn.getId(), AdAuthRequest.forMethodAndPath("GET", "/ncc/campaigns")
            );
            return naverClient.getCampaigns(headers);
        } catch (Exception e) {
            log.error("[NAVER] 캠페인 조회 실패 - orgId={}", orgId, e);
            throw new RuntimeException("네이버 캠페인 조회 실패", e);
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
