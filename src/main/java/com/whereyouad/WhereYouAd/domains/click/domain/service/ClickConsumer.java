package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.application.mapper.ClickConverter;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    // key: click:real:{adContentId}:{yyyyMMddHHmm} 또는 click:dummy:{adContentId}:{yyyyMMddHHmm}
    @KafkaListener(topics = "ad-click-events", groupId = "where-you-ad-group")
    public void consume(ClickDto event) {
        // 현재 시간을 분 단위 문자열로 변환
        String currentMinute = LocalDateTime.now().format(MINUTE_FORMATTER);
        String mode = event.isDummy() ? "dummy" : "real";
        String clickKey = String.format("click:%s:%s:%s", mode, event.getAdContentId(), currentMinute);

        Long currentClickCount = redisUtil.incrementDataExpire(clickKey, REDIS_TTL_SECONDS);

        log.info("Click Key: {}, UserAgent: {}, IP Address: {}, Count: {}",
                clickKey, event.getUserAgent(), event.getIpAddress(), currentClickCount);
    }

    // 실제 데이터 봇 판별 후 DB 저장
    @KafkaListener(topics = "ad-click-events", groupId = "where-you-ad-group-db")
    public void consumeToDB(ClickDto event) {
        // 더미 데이터 DB 저장 x
        if (event.isDummy()) return;

        AdContent adContent = adContentRepository.findById(event.getAdContentId())
                .orElse(null);

        if (adContent == null) {
            log.warn("[DB저장] adContentId={} 광고를 찾을 수 없음", event.getAdContentId());
            return;
        }

        boolean isSuspect = botDetector.isSuspect(event.getIpAddress(), event.getUserAgent());

        ClickLog clickLog = ClickConverter.toClickLog(adContent, event, isSuspect);

        clickLogRepository.save(clickLog);

        log.info("[DB저장] adId={}, ip={}, isSuspect={}",
                event.getAdContentId(), event.getIpAddress(), isSuspect);
    }
}
