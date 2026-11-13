package com.whereyouad.WhereYouAd.domains.click.presentation.scheduler;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.exception.ClickHandler;
import com.whereyouad.WhereYouAd.domains.click.exception.code.ClickErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;

import java.time.Duration;
import java.time.Instant;
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
    private static final String[] userAgents = {"UNKNOWN", "MOBILE", "PC"};

    // 더미 생성기 사용 가능 여부 (운영 기본값 false, 로컬 등 필요한 환경에서만 true)
    @Value("${click.dummy.enabled:false}")
    private boolean enabled;

    // 켜진 뒤 자동으로 꺼지기까지의 시간 (끄는 것을 잊어도 메시지가 무한히 쌓이지 않도록)
    @Value("${click.dummy.max-running-minutes:30}")
    private long maxRunningMinutes;

    // 요청 스레드(toggle)와 스케줄러 스레드(generateDummyClick)가 함께 접근하므로 volatile
    private volatile boolean isRunning = false;
    private volatile Instant startedAt;
    private volatile List<Object[]> activeAds = List.of();

    public synchronized boolean toggle() {
        if (!enabled) {
            throw new ClickHandler(ClickErrorCode.DUMMY_PRODUCER_DISABLED);
        }

        if (isRunning) {
            stop("수동 종료");
            return false;
        }

        this.activeAds = adContentRepository.findAllActiveAdOrgMappings(Status.ON_GOING);
        this.startedAt = Instant.now();
        this.isRunning = true;
        log.info("통합 대시보드 실시간 클릭수 더미데이터: 활성 상태인 광고를 모두 가져옵니다. {}개", activeAds.size());
        log.info("Dummy Click Producer is now RUNNING ({}분 후 자동 종료)", maxRunningMinutes);
        return true;
    }

    // 500ms마다 실행
    @Scheduled(fixedRate = 500)
    public void generateDummyClick() {
        if (!isRunning) {
            return;
        }
        if (isExpired()) {
            stop("최대 실행 시간 " + maxRunningMinutes + "분 경과로 자동 종료");
            return;
        }

        List<Object[]> ads = this.activeAds;
        if (ads.isEmpty()) {
            return;
        }
        // 0.5초마다 최대 30개의 클릭만 발생시키도록 Limit 설정 (서버 부하 방지)
        int limit = Math.min(ads.size(), 30);

        // 30개에 대하여 반복하여 mock 클릭수 생성
        for (int i = 0; i < limit; i++) {
            // 전체 활성 광고 중 무작위로 하나를 선택
            Object[] adOrgPair = ads.get(random.nextInt(ads.size()));
            Long adContentId = (Long) adOrgPair[0];
            Long orgId = (Long) adOrgPair[1];
            Provider provider = (Provider) adOrgPair[2];

            // 임의의 IP 주소, 기기 생성
            String ipAddress = "192.168.0." + (random.nextInt(50) + 1);
            String userAgent = userAgents[random.nextInt(userAgents.length)];

            ClickDto event = ClickDto.builder()
                    .adContentId(adContentId)
                    .orgId(orgId)
                    .provider(provider)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .clickedAt(System.currentTimeMillis())
                    .isDummy(true)
                    .build();

            // Kafka로 메시지 send (key는 adContentId, 같은 광고끼리 같은 파티션으로 분배)
            kafkaTemplate.send(TOPIC, String.valueOf(adContentId), event);
        }

        log.debug("Produced dummy clicks for {} sampled ads.", limit);
    }

    private boolean isExpired() {
        Instant started = this.startedAt;
        return started != null
                && Instant.now().isAfter(started.plus(Duration.ofMinutes(maxRunningMinutes)));
    }

    private synchronized void stop(String reason) {
        if (!isRunning) {
            return;
        }
        this.isRunning = false;
        this.startedAt = null;
        this.activeAds = List.of();
        log.info("Dummy Click Producer is now STOPPED ({})", reason);
    }
}
