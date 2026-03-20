package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.click.exception.ClickHandler;
import com.whereyouad.WhereYouAd.domains.click.exception.code.ClickErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClickServiceImpl implements ClickService {

    private final AdContentRepository adContentRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final ClickEventProducer clickEventProducer;
    private final RedisUtil redisUtil;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public ClickResponse.NewTrackingUrl createTrackingUrl(Long userId, Long adContentId, Long orgId, String landingUrl) {

        // 1. 유저가 해당 조직 구성원인지 검증
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new ClickHandler(ClickErrorCode.CLICK_UNAUTHORIZED);
        }

        // 2. 광고 조회 및 AdContent가 해당 조직것인지 검증
        AdContent adContent = adContentRepository.findByIdAndOrganizationId(adContentId, orgId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        // 3. 입력받은 landingUrl로 최신화하여 저장
        adContent.updateLandingUrl(landingUrl);

        // 4. 이미 트래킹 URL이 존재하면 새로 생성하지 않고 기존 URL 반환
        if (StringUtils.hasText(adContent.getTrackingUrl())) {
            return new ClickResponse.NewTrackingUrl(adContent.getTrackingUrl());
        }

        // 5. 동일 코드가 DB에 이미 존재하면 재시도
        String trackingUrl;
        do {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            trackingUrl = baseUrl + "/api/clicks/track/" + code;
        } while (adContentRepository.existsByTrackingUrl(trackingUrl));

        // 6. 트래킹 주소 저장(더티 체킹)
        adContent.updateTrackingUrl(trackingUrl);

        return new ClickResponse.NewTrackingUrl(trackingUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public String handleTrackingRedirect(String code, String ipAddress, String userAgent) {
        String trackingUrl = baseUrl + "/api/clicks/track/" + code;

        // 1. 광고 조회 (trackingUrl 기반)
        AdContent adContent = adContentRepository.findByTrackingUrl(trackingUrl)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        // 2. 클릭 이벤트 생성 후 Kafka로 발행
        ClickDto clickDto = ClickDto.builder()
                .adContentId(adContent.getId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .clickedAt(System.currentTimeMillis())
                .isDummy(false)
                .build();
        clickEventProducer.produce(clickDto);

        // 3. 랜딩 Url 반환
        if (StringUtils.hasText(adContent.getLandingUrl())) {
            return adContent.getLandingUrl();
        }
        // 랜딩 Url이 없을 경우 홈으로
        return baseUrl;
    }

    @Override
    public List<ClickResponse.RealtimeClickCount> getRealtimeClickCounts(Long adContentId, String mode, int minutes) {
        // mode 값 검증 (real or dummy만 허용)
        if (!"real".equals(mode) && !"dummy".equals(mode)) {
            throw new ClickHandler(ClickErrorCode.CLICK_INVALID_MODE);
        }

        List<ClickResponse.RealtimeClickCount> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        for (int i = minutes - 1; i >= 0; i--) {
            LocalDateTime time = now.minusMinutes(i);
            String minute = time.format(formatter);

            // Redis Key: click:{mode}:{adContentId}:{minute}
            String key = String.format("click:%s:%s:%s", mode, adContentId, minute);
            String value = redisUtil.getData(key);
            long count = value != null ? Long.parseLong(value) : 0L;
            result.add(new ClickResponse.RealtimeClickCount(minute, count));
        }
        return result;
    }
}
