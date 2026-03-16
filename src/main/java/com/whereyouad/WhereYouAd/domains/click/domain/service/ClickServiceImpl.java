package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.click.exception.ClickHandler;
import com.whereyouad.WhereYouAd.domains.click.exception.code.ClickErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClickServiceImpl implements ClickService {

    private final AdContentRepository adContentRepository;
    private final OrgMemberRepository orgMemberRepository;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public ClickResponse.NewTrackingUrl createTrackingUrl(Long userId, Long adContentId, Long orgId) {

        // 1. 유저가 해당 조직 구성원인지 검증
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new ClickHandler(ClickErrorCode.CLICK_UNAUTHORIZED);
        }

        // 2. 광고 조회
        AdContent adContent = adContentRepository.findById(adContentId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        // 3. 이미 트래킹 URL이 존재하면 새로 생성하지 않고 기존 URL 반환
        if (StringUtils.hasText(adContent.getTrackingUrl())) {
            return new ClickResponse.NewTrackingUrl(adContent.getTrackingUrl());
        }

        // 4. 동일 코드가 DB에 이미 존재하면 재시도
        String trackingUrl;
        do {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            trackingUrl = baseUrl + "/api/track/" + code;
        } while (adContentRepository.existsByTrackingUrl(trackingUrl));

        // 5. 트래킹 주소 저장(더티 체킹)
        adContent.updateTrackingUrl(trackingUrl);

        return new ClickResponse.NewTrackingUrl(trackingUrl);
    }
}
