package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
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

        // 1. 조직 구성원 검증 + 해당 조직의 광고인지 검증
        AdContent adContent = getAuthorizedAdContent(userId, adContentId, orgId);

        // 2. 입력받은 landingUrl로 최신화하여 저장
        adContent.updateLandingUrl(landingUrl);

        // 3. 자체 발급한 트래킹 URL이 있을 때만 재사용 (플랫폼 값이면 새로 발급)
        String current = adContent.getTrackingUrl();
        if (StringUtils.hasText(current) && current.contains("/api/clicks/track/")) {
            return new ClickResponse.NewTrackingUrl(current);
        }

        // 4. 동일 코드가 DB에 이미 존재하면 재시도
        String trackingUrl;
        do {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            trackingUrl = baseUrl + "/api/clicks/track/" + code;
        } while (adContentRepository.existsByTrackingUrl(trackingUrl));

        // 5. 트래킹 주소 저장(더티 체킹)
        adContent.updateTrackingUrl(trackingUrl);

        return new ClickResponse.NewTrackingUrl(trackingUrl);
    }

    @Override
    @Transactional
    public ClickResponse.DeletedTrackingUrl deleteTrackingUrl(Long userId, Long adContentId, Long orgId) {

        // 1. 조직 구성원 검증 + 해당 조직의 광고인지 검증
        AdContent adContent = getAuthorizedAdContent(userId, adContentId, orgId);

        // 2. 삭제할 트래킹 URL이 없으면 예외 처리
        String deletedTrackingUrl = adContent.getTrackingUrl();
        if (!StringUtils.hasText(deletedTrackingUrl)) {
            throw new ClickHandler(ClickErrorCode.TRACKING_URL_NOT_FOUND);
        }

        // 3. 트래킹 URL 삭제 (더티 체킹) — 랜딩 URL은 유지
        adContent.updateTrackingUrl(null);

        return new ClickResponse.DeletedTrackingUrl(deletedTrackingUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public String handleTrackingRedirect(String code, String ipAddress, String userAgent) {
        String trackingUrl = baseUrl + "/api/clicks/track/" + code;

        // 1. 광고 조회 (trackingUrl 기반)
        AdContent adContent = adContentRepository.findByTrackingUrl(trackingUrl)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        Long orgId = adContent.getAdGroup().getAdCampaign().getProject().getOrganization().getId();
        Provider provider = adContent.getAdGroup().getAdCampaign().getProvider();

        // 2. 클릭 이벤트 생성 후 Kafka로 발행
        ClickDto clickDto = ClickDto.builder()
                .adContentId(adContent.getId())
                .orgId(orgId)
                .provider(provider)
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
        // ClickConsumer가 적재한 분 단위 키와 같은 타임존을 사용해야 조회가 정합
        LocalDateTime now = LocalDateTime.now(ClickWindowKeys.ZONE_ID);
        DateTimeFormatter formatter = ClickWindowKeys.MINUTE_FORMATTER;

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

    // 조직 구성원 검증 후 해당 조직의 AdContent 반환
    private AdContent getAuthorizedAdContent(Long userId, Long adContentId, Long orgId) {
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new ClickHandler(ClickErrorCode.CLICK_UNAUTHORIZED);
        }

        return adContentRepository.findByIdAndOrganizationId(adContentId, orgId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));
    }
}
