package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.DeviceType;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickConsumer {

    private final RedisUtil redisUtil;
    private final AdContentRepository adContentRepository;
    private final ClickLogRepository clickLogRepository;
    private final BotDetector botDetector;

    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final long REDIS_TTL_SECONDS = 7200L; // 2시간


     // Redis 분 단위 집계 (더미/실제 클릭)
     // key click:real:{adId}:{yyyyMMddHHmm} 또는 click:dummy:{adId}:{yyyyMMddHHmm}
    @KafkaListener(topics = "ad-click-events", groupId = "where-you-ad-group")
    public void consume(ClickDto event) {

        // 현재 시간을 분 단위 문자열로 변환
        String currentMinute = LocalDateTime.now().format(MINUTE_FORMATTER);
        String mode = event.isDummy() ? "dummy" : "real";
        String clickKey = String.format("click:%s:%s:%s", mode, event.getAdId(), currentMinute);

        Long currentClickCount = redisUtil.incrementDataExpire(clickKey, REDIS_TTL_SECONDS);

        // JSON -> ClickDto 객체
        log.info("Click Key: {}, Device: {}, IP Address: {}, Count: {}",
                clickKey, event.getDevice(), event.getIpAddress(), currentClickCount);
    }

    // 실세 데이터 봇 판별
    @KafkaListener(topics = "ad-click-events", groupId = "where-you-ad-group-db")
    public void consumeToDB(ClickDto event) {
        // 더미 데이터 DB 저장 x
        if (event.isDummy()) return;

        AdContent adContent = adContentRepository.findById(Long.parseLong(event.getAdId()))
                .orElse(null);

        if (adContent == null) {
            log.warn("[DB저장] adContentId={} 광고를 찾을 수 없음", event.getAdId());
            return;
        }

        boolean isSuspect = botDetector.isSuspect(event.getIpAddress(), event.getDevice());

        LocalDateTime clickedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getClickedAt()), ZoneId.systemDefault());

        ClickLog clickLog = ClickLog.builder()
                .adContent(adContent)
                .ipAddress(event.getIpAddress())
                .device(extractDeviceType(event.getDevice()))
                .clickedAt(clickedAt)
                .isSuspect(isSuspect)
                .build();

        clickLogRepository.save(clickLog);

        log.info("[DB저장] adId={}, ip={}, isSuspect={}",
                event.getAdId(), event.getIpAddress(), isSuspect);
    }

    private DeviceType extractDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent))
            return DeviceType.UNKNOWN;
        String lower = userAgent.toLowerCase();
        return (lower.contains("mobi") || lower.contains("android") || lower.contains("iphone"))
                ? DeviceType.MOBILE
                : DeviceType.PC;
    }
}
