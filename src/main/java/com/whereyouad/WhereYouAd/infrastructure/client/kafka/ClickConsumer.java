package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.application.mapper.ClickConverter;
import com.whereyouad.WhereYouAd.domains.click.domain.config.ClickSurgeProperties;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
import com.whereyouad.WhereYouAd.domains.click.domain.service.BotDetector;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.NotificationAlertEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationEventProducer;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickConsumer {

    private final RedisUtil redisUtil;
    private final AdContentRepository adContentRepository;
    private final ClickLogRepository clickLogRepository;
    private final BotDetector botDetector;
    private final NotificationEventProducer notificationEventProducer;
    private final ClickSurgeProperties clickSurgeProperties;

    private static final DateTimeFormatter MINUTE_FORMATTER = ClickWindowKeys.MINUTE_FORMATTER;
    private static final long REDIS_TTL_SECONDS = 7200L; // 2시간
    private static final long CLICK_ALERT_COOLDOWN_SECONDS = 600L; // 10분 - 조직별 봇 클릭 외부 알림(Kafka 발행) 쿨다운
    private final ObjectMapper objectMapper;

    // Redis 분 단위 집계 (더미/실제 클릭)
    // key: click:real:{adContentId}:{yyyyMMddHHmm} 또는 click:dummy:{adContentId}:{yyyyMMddHHmm}
    @KafkaListener(topics = "ad-click-events", groupId = "where-you-ad-group")
    public void consume(ClickDto event) {
        // 이벤트 발생 시각(clickedAt)을 분 단위 문자열로 변환 - consumer 지연 시에도 실제 클릭 시각 기준으로 집계
        LocalDateTime clickTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getClickedAt()), ClickWindowKeys.ZONE_ID);
        String currentMinute = clickTime.format(MINUTE_FORMATTER);
        String mode = event.isDummy() ? "dummy" : "real";
        String clickKey = String.format("click:%s:%s:%s", mode, event.getAdContentId(), currentMinute);

        Long currentClickCount = redisUtil.incrementDataExpire(clickKey, REDIS_TTL_SECONDS);

        // 조직 단위 통합 집계
        if (event.getOrgId() != null) {
            // 통합 대시보드용 - 조직 단위 집계
            String orgClickKey = String.format("click:%s:org:%s:%s", mode, event.getOrgId(), currentMinute);
            redisUtil.incrementDataExpire(orgClickKey, REDIS_TTL_SECONDS);

            // 플랫폼별 대시보드용 - provider 단위 통합 집계 추가
            if (event.getProvider() != null) {
                String providerClickKey = String.format("click:%s:org:%s:provider:%s:%s", mode, event.getOrgId(), event.getProvider().name(), currentMinute);
                redisUtil.incrementDataExpire(providerClickKey, REDIS_TTL_SECONDS);
            } else { // provider null 이벤트 로그 처리
                log.warn("provider 누락 이벤트 수신: adId={}, mode={}", event.getAdContentId(), mode);
            }
        } else { //orgId null 이벤트 로그 처리
            log.warn("orgId 누락 이벤트 수신: adId={}, mode={}", event.getAdContentId(), mode);
        }

        // 급증 감지 대상 등록 - 이번 5분 윈도우에 클릭이 있었던 광고만 스케줄러가 검사
        if (!event.isDummy() && event.getOrgId() != null && clickSurgeProperties.isEnabled()) {
            LocalDateTime windowStart = ClickWindowKeys.floorToWindowStart(clickTime, clickSurgeProperties.getWindowMinutes());
            redisUtil.sAddExpire(
                    ClickWindowKeys.activeAdsKey(windowStart),
                    clickSurgeProperties.getActiveSetTtlSeconds(),
                    ClickWindowKeys.activeAdsMember(event.getAdContentId(), event.getOrgId()));
        }

        log.debug("Click Key: {}, UserAgent: {}, IP Address: {}, Count: {}",
                clickKey, event.getUserAgent(), event.getIpAddress(), currentClickCount);
    }

    // 실제 데이터 봇 판별 후 DB 저장
    @KafkaListener(topics = "ad-click-events", groupId = "where-you-ad-group-db")
    public void consumeToDB(ClickDto event) {
        // 더미 데이터 DB 저장 x
        if (event.isDummy()) return;

        //OSIV 비활성화로 인해 AdContent 와 함께 AdGroup, AdCampaign 까지 모두 한번에 조회하는 Repository 메서드로 변경
        AdContent adContent = adContentRepository.findByIdWithGroupAndCampaign(event.getAdContentId())
                .orElse(null);

        if (adContent == null) {
            log.warn("[DB저장] adContentId={} 광고를 찾을 수 없음", event.getAdContentId());
            return;
        }

        boolean isSuspect = botDetector.isSuspect(event.getIpAddress(), event.getUserAgent());

        // 알림용 JSON 데이터 생성 및 Redis 적재 (JPA 연관관계 적용)
        if (isSuspect && event.getOrgId() != null) {
            String alertKey = String.format("click:suspect:alert:org:%s", event.getOrgId());

            try {
                Map<String, Object> suspectDetail = new HashMap<>();

                // 기본값 세팅 (NPE 방어)
                String providerStr = "알 수 없는 플랫폼";
                String campaignNameStr = "알 수 없는 캠페인";
                String adNameStr = (adContent.getName() != null) ? adContent.getName() : "알 수 없는 광고";
                String alertMessage = "비정상적인 트래픽(Bot)이 감지되었습니다. (IP: " + event.getIpAddress() + ")";

                // 연관관계를 타고 올라가며 실제 값 추출 및 적용
                if (adContent.getAdGroup() != null && adContent.getAdGroup().getAdCampaign() != null) {

                    // 캠페인 이름 추출
                    if (adContent.getAdGroup().getAdCampaign().getName() != null) {
                        campaignNameStr = adContent.getAdGroup().getAdCampaign().getName();
                    }

                    // 플랫폼(Provider) 추출
                    if (adContent.getAdGroup().getAdCampaign().getProvider() != null) {
                        providerStr = String.valueOf(adContent.getAdGroup().getAdCampaign().getProvider());
                    }
                }

                // Map에 이상 클릭 값 적재
                suspectDetail.put("provider", providerStr);
                suspectDetail.put("campaignName", campaignNameStr);
                suspectDetail.put("adName", adNameStr);
                suspectDetail.put("message", alertMessage);

                // JSON 변환 후 Redis 저장 (수명 60초)
                String detailJson = objectMapper.writeValueAsString(suspectDetail);
                redisUtil.setDataExpire(alertKey, detailJson, 60);

                // 플랫폼 지정 구독용 키 추가 (read 쪽 provider 분기와 정합)
                if (event.getProvider() != null) {
                    String providerAlertKey = String.format("click:suspect:alert:org:%s:provider:%s",
                            event.getOrgId(), event.getProvider().name());
                    redisUtil.setDataExpire(providerAlertKey, detailJson, 60);
                }

                log.info(" 봇 감지 : 실제 데이터 매핑 완료 및 Redis 적재: {}", alertKey);

                // 조직별 쿨다운 체크 - 쿨다운 중이 아닐 때만(=락 선점 성공 시) 외부 채널 알림 발행
                String cooldownKey = String.format("notification:cooldown:clicks:org:%s", event.getOrgId());
                boolean cooldownAcquired = Boolean.TRUE.equals(
                        redisUtil.setIfAbsent(cooldownKey, "1", CLICK_ALERT_COOLDOWN_SECONDS));

                if (cooldownAcquired) {
                    // 디스코드/슬랙 등 외부 채널 알림을 위한 Kafka 이벤트 발행
                    notificationEventProducer.produce(NotificationAlertEvent.builder()
                            .orgId(event.getOrgId())
                            .type(NotificationType.CLICKS)
                            .title("[" + campaignNameStr + "] 비정상 클릭 감지")
                            .message(alertMessage)
                            .build());
                } else {
                    log.debug("[외부 알림 발송 skip] 쿨다운 적용 중, orgId={}", event.getOrgId());
                }
            } catch (Exception e) {
                log.error("봇 알림 JSON 변환/저장 실패", e);
            }
        }

        ClickLog clickLog = ClickConverter.toClickLog(adContent, event, isSuspect);

        clickLogRepository.save(clickLog);

        log.debug("[DB저장] adId={}, ip={}, isSuspect={}",
                event.getAdContentId(), event.getIpAddress(), isSuspect);
    }
}
