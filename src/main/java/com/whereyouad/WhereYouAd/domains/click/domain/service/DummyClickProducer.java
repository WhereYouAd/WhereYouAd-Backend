package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class DummyClickProducer {

    private final KafkaTemplate<String, ClickDto> kafkaTemplate;
    private final Random random = new Random();

    private static final String TOPIC = "ad-click-events";
    private static final String[] adIds = {"1", "2", "3"};

    // 500ms마다 실행
    @Scheduled(fixedRate = 500)
    public void generateDummyClick() {
        String adId = adIds[random.nextInt(adIds.length)];

        // 임의의 IP 주소, 기기 생성
        String ipAddress = "192.168.0." + (random.nextInt(50) + 1);
        String device = "iOS";
        long clickedAt = System.currentTimeMillis();

        ClickDto event = ClickDto.builder()
                .adId(adId)
                .ipAddress(ipAddress)
                .device(device)
                .clickedAt(clickedAt)
                .isDummy(true)
                .build();

        // Kafka로 메시지 send (key는 adId, 같은 광고끼리 같은 파티션으로 분배)
        kafkaTemplate.send(TOPIC, adId, event);

        log.info("Produced click event: {}", event);
    }

}
