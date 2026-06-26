package com.whereyouad.WhereYouAd.domains.click.presentation.scheduler;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class DummyClickProducer {

    private final KafkaTemplate<String, ClickDto> kafkaTemplate;
    private final AdContentRepository adContentRepository;
    private final Random random = new Random();

    private static final String TOPIC = "ad-click-events";

    private List<Object[]> activeAds = new ArrayList<>();
    private static final String[] userAgents = {"UNKNOWN", "MOBILE", "PC"};

    // 기본값 false (API를 통해 켤 때만 동작)
    private boolean isRunning = false;

    public boolean toggle() {
        this.isRunning = !this.isRunning;
        if (this.isRunning) {
            this.activeAds = adContentRepository.findAllAdOrgMappings();
            log.info("통합 대시보드 실시간 클릭수 더미데이터: 활성 상태인 광고를 모두 가져옵니다. {}개", activeAds.size());
        }
        log.info("Dummy Click Producer is now {}", isRunning ? "RUNNING" : "STOPPED");
        return isRunning;
    }

    // 500ms마다 실행
    @Scheduled(fixedRate = 500)
    public void generateDummyClick() {
        if (!isRunning || activeAds.isEmpty()) {
            return;
        }
        // 0.5초마다 최대 30개의 클릭만 발생시키도록 Limit 설정 (서버 부하 방지)
        int limit = Math.min(activeAds.size(), 30);

        // 30개에 대하여 반복하여 mock 클릭수 생성
        for (int i = 0; i < limit; i++) {
            // 전체 활성 광고 중 무작위로 하나를 선택
            Object[] adOrgPair = activeAds.get(random.nextInt(activeAds.size()));
            Long adContentId = (Long) adOrgPair[0];
            Long orgId = (Long) adOrgPair[1];

            // 임의의 IP 주소, 기기 생성
            String ipAddress = "192.168.0." + (random.nextInt(50) + 1);
            String userAgent = userAgents[random.nextInt(userAgents.length)];

            ClickDto event = ClickDto.builder()
                    .adContentId(adContentId)
                    .orgId(orgId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .clickedAt(System.currentTimeMillis())
                    .isDummy(true)
                    .build();

            // Kafka로 메시지 send (key는 adContentId, 같은 광고끼리 같은 파티션으로 분배)
            kafkaTemplate.send(TOPIC, String.valueOf(adContentId), event);
        }

        log.info("Produced dummy clicks for {} sampled ads.", limit);
    }
}
