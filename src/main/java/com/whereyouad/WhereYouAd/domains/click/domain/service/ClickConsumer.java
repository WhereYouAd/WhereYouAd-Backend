package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
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

    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    // 집계
    @KafkaListener(topics = "ad-click-events", groupId = "where-you-ad-group")
    public void consume(ClickDto event) {

        // 현재 시간을 분 단위 문자열로 변환
        String currentMinute = LocalDateTime.now().format(MINUTE_FORMATTER);

        // 1분 동안의 클릭수 집계
        // Redis Key:Value (ex. click:1:202603192112)
        String clickKey = String.format("click:%s:%s", event.getAdId(), currentMinute);

        // Redis에 저장 (2시간 유지)
        Long currentClickCount = redisUtil.incrementDataExpire(clickKey, 7200L);

        // JSON -> ClickDto 객체
        log.info("Click Key: {}, Device: {}, IP Address: {}, Count: {}",
                clickKey, event.getDevice(), event.getIpAddress(), currentClickCount);
    }
}
