package com.whereyouad.WhereYouAd.domains.click.presentation.scheduler;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    private static final Long[] adContentIds = {1L, 2L, 3L};
    private static final String[] userAgents = {"UNKNOWN", "MOBILE", "PC"};

    // 기본값 false (API를 통해 켤 때만 동작)
    private boolean isRunning = false;

    public boolean toggle() {
        this.isRunning = !this.isRunning;
        log.info("Dummy Click Producer is now {}", isRunning ? "RUNNING" : "STOPPED");
        return isRunning;
    }

    // 500ms마다 실행
    @Scheduled(fixedRate = 500)
    public void generateDummyClick() {
        if (!isRunning) {
            return;
        }
        Long adContentId = adContentIds[random.nextInt(adContentIds.length)];

        // 임의의 IP 주소, 기기 생성
        String ipAddress = "192.168.0." + (random.nextInt(50) + 1);
        String userAgent = userAgents[random.nextInt(userAgents.length)];

        ClickDto event = ClickDto.builder()
                .adContentId(adContentId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .clickedAt(System.currentTimeMillis())
                .isDummy(true)
                .build();

        // Kafka로 메시지 send (key는 adContentId, 같은 광고끼리 같은 파티션으로 분배)
        kafkaTemplate.send(TOPIC, String.valueOf(adContentId), event);

        log.info("Produced dummy click: adContentId={}, ip={}", adContentId, ipAddress);
    }
}
