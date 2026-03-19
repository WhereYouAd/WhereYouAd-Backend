package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.click.application.mapper.ClickConverter;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
// 큐에 적재된 클릭 이벤트를 5초마다 100개씩 db에 insert하는 메서드
// TODO: kafka 도입 시 1번 로직 수정 필요, 현재는 redis 구조
public class ClickEventConsumerService {

    private static final String QUEUE_NAME = "click_queue";
    private static final int BATCH_SIZE = 100;

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final ClickLogRepository clickLogRepository;
    private final AdContentRepository adContentRepository;

    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    @Transactional
    public void consumeClickEvents() {
        List<ClickResponse.ClickEvent> events = new ArrayList<>();

        // 1. Redis에서 배치 사이즈만큼 RPOP
        for (int i = 0; i < BATCH_SIZE; i++) {
            String jsonEvent = redisUtil.rightPop(QUEUE_NAME);
            if (jsonEvent == null) {
                break; // 큐가 비어있으면 루프 종료
            }

            try {
                ClickResponse.ClickEvent event = objectMapper.readValue(jsonEvent, ClickResponse.ClickEvent.class);
                events.add(event);
            } catch (JsonProcessingException e) {
                log.error("클릭 이벤트 역직렬화 실패: {}", jsonEvent, e);
            }
        }

        if (events.isEmpty()) {
            return;
        }

        // 2. 유효한 이벤트 필터링 (adContentId 추출)
        List<ClickLog> clickLogsToSave = new ArrayList<>();
        List<Long> adContentIdsToFetch = new ArrayList<>();

        for (ClickResponse.ClickEvent event : events) {
            adContentIdsToFetch.add(event.adContentId());
        }

        // 3. 광고 정보 조회
        Map<Long, AdContent> adContentMap = adContentRepository
                .findAllById(adContentIdsToFetch.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(AdContent::getId, ad -> ad));

        // 4. ClickLog 엔티티 생성
        for (ClickResponse.ClickEvent event : events) {
            AdContent adContent = adContentMap.get(event.adContentId());
            if (adContent == null) {
                log.warn("{} 번 광고를 찾을 수 없음", event.adContentId());
                continue;
            }

            // 봇 판별
            boolean isUserAgentBot = isBot(event.userAgent()); // 알려진 봇이나 개발 도구들을 이용한 접근한 경우 봇
            boolean isAbnormalIp = checkAbnormalIp(event.ipAddress()); // 동일 IP에서 1분에 20회 이상 요청을 보낸 경우 봇
            boolean suspect = isUserAgentBot || isAbnormalIp; // 위 경우 중 둘 중 하나라도 봇인 경우 봇

            ClickLog clickLog = ClickConverter.toClickLog(adContent, event, suspect);

            clickLogsToSave.add(clickLog);
        }

        // 5. db 저장
        if (!clickLogsToSave.isEmpty()) {
            clickLogRepository.saveAll(clickLogsToSave);
            log.info("DB에 {}개 저장", clickLogsToSave.size());
        }
    }

    // 봇 판별 내부 메서드
    private boolean isBot(String userAgent) {
        // 값이 없거나 너무 짧은 경우 정상적인 브라우저 X
        if (!StringUtils.hasText(userAgent) || userAgent.length() < 10) {
            return true;
        }

        String lowerAgent = userAgent.toLowerCase();

        // 1. 헤더에서 봇이라고 밝히는 경우 (검색엔진, 미리보기 등)
        if (isKnownBot(lowerAgent)) {
            return true;
        }

        // 2. 악의적인 봇이거나 스크래퍼/자동화 도구인 경우
        if (isMaliciousBot(lowerAgent)) {
            return true;
        }

        return false;
    }

    private boolean isKnownBot(String lowerAgent) {
        String[] botKeywords = { // 봇 키워드
                "bot", "crawler", "spider", "ping", "slurp",
                "lighthouse", "postman", "curl", "kakaotalk-scrap",
                "yeti", "googlebot", "bingbot"
        };

        for (String keyword : botKeywords) {
            if (lowerAgent.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // 악의적이거나 비정상적인 접근 패턴을 걸러내는 내부 메서드
    private boolean isMaliciousBot(String lowerAgent) {
        // 1. 프로그래밍 언어 및 HTTP 클라이언트 라이브러리의 기본 User-Agent
        String[] scraperKeywords = {
                "python-requests", "python-urllib", "java/", "go-http-client", "axios",
                "node-fetch", "okhttp", "wget", "scrapy", "httpclient", "apache-httpclient"
        };
        for (String keyword : scraperKeywords) {
            if (lowerAgent.contains(keyword)) {
                return true;
            }
        }

        // 2. 자동화/테스트용 Headless 브라우저
        String[] headlessKeywords = {
                "headlesschrome", "phantomjs", "puppeteer", "selenium", "playwright", "cypress"
        };
        for (String keyword : headlessKeywords) {
            if (lowerAgent.contains(keyword)) {
                return true;
            }
        }

        // 3. 비정상적인 구조 검사 (대부분 userAgent에 mozilla 또는 opera를 포함)
        if (!lowerAgent.contains("mozilla") && !lowerAgent.contains("opera")) {
            return true;
        }

        return false;
    }

    // 특정 IP에서 비정상적으로 많은 클릭이 발생하는지 검사 (예: 1분에 20회 초과 시 봇으로 간주)
    private boolean checkAbnormalIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return false;
        }

        String key = "click_ip_count:" + ipAddress;

        // 해당 IP의 카운트를 1 증가
        Long count = redisUtil.increment(key);

        if (count != null && count == 1) {
            // 처음 측정되는 IP면 만료 시간(TTL)을 1분으로 설정
            redisUtil.expire(key, 1, TimeUnit.MINUTES);
        }

        // 1분 내에 20회 초과 접속 시 봇으로 취급
        if (count != null && count > 20) {
            // 봇으로 취급된 경우 10분 차단
            redisUtil.expire(key, 10, TimeUnit.MINUTES);
            return true;
        }

        return false;
    }
}
