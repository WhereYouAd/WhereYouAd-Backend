package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.DashboardException;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.code.DashboardErrorCode;
import com.whereyouad.WhereYouAd.global.sse.repository.SseEmitterRepository;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.application.mapper.DashboardConverter;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardClickServiceImpl implements DashboardClickService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 30; // SseEmitter 생명주기 30분 = 연결 30분 유지
    private static final String ALL_PROVIDERS_TOKEN = "ALL";
    private static final Set<Provider> SUPPORTED_PROVIDERS =
            EnumSet.of(Provider.GOOGLE, Provider.NAVER, Provider.META);

    private final SseEmitterRepository emitterRepository;
    private final RedisUtil redisUtil;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgRepository orgRepository;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");


    // 구독 (클라이언트 연결)
    public SseEmitter subscribe(Long userId, Long orgId, String mode, String providerType) {

        orgRepository.findById(orgId).
                orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        // mode 값, provider 값 파싱
        String safeMode = parseMode(mode);
        String providerToken = parseProviderToken(providerType); // "ALL" 또는 "NAVER"/"GOOGLE"/"META"

        // routingKey 예: "1_ALL_dummy"(조직 전체), "1_NAVER_real"(네이버 플랫폼)
        String routingKey = orgId + "_" + providerToken + "_" + safeMode;
        String emitterId = userId + "_" + UUID.randomUUID();

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // Emitter 생명주기 콜백 설정 (연결 종료, 타임아웃, 에러 발생 시 메모리 누수 방지를 위해 저장소에서 제거)
        emitter.onCompletion(() -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));
        emitter.onError((e) -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));

        emitterRepository.save(routingKey, emitterId, emitter);

        // 최초 연결 시 즉시 데이터 전송 (프론트엔드 차트 초기 렌더링용)
        sendToClient(routingKey, emitterId, emitter, DataResponse.from(getGraphData(orgId, providerToken, safeMode)));

        return emitter;
    }

    // 1초마다 백그라운드에서 실행되며, 현재 연결된 모든 클라이언트에게 최신 데이터를 브로드캐스팅하는 스케줄러
    @Scheduled(fixedRate = 1000)
    public void broadcastRealTimeClicks() {
        // 현재 구독자가 있는 채널(라우팅 키) 목록만 가져옴 (구독자가 없으면 Redis 조회를 생략하여 리소스 절약)
        Set<String> activeRoutingKeys = emitterRepository.findAllRoutingKeys();

        //모든 구독자 존재 채널(라우팅 키) 에 대하여,
        for (String routingKey : activeRoutingKeys) {

            // routingKey 예: "1_NAVER_dummy" → 3토큰
            // provider/mode 에는 '_' 가 없으므로 split("_", 3) 으로 안전하게 분해
            String[] parts = routingKey.split("_", 3);
            Long orgId = Long.parseLong(parts[0]);
            String providerToken = parts[1]; // "ALL" 또는 "NAVER"...
            String mode = parts[2];

            // 이번 턴에 전송할 데이터(60분치 시계열 배열 + 봇 알림)를 Redis에서 조합
            DashboardResponse.RealTimeGraphResponse payload = getGraphData(orgId, providerToken, mode);
            DataResponse<DashboardResponse.RealTimeGraphResponse> responseBody = DataResponse.from(payload);

            // 해당 채널(예: 1번 조직_real)을 보고 있는 모든 유저의 Emitter를 꺼내서 전송
            Map<String, SseEmitter> sseEmitters = emitterRepository.findAllByRoutingKey(routingKey);
            sseEmitters.forEach((emitterId, emitter) -> {
                sendToClient(routingKey, emitterId, emitter, responseBody);
            });
        }
    }

    // 내부 메서드: 조직의 60분 시계열 데이터와 알림 정보를 묶어서 반환
    private DashboardResponse.RealTimeGraphResponse getGraphData(Long orgId, String providerToken, String mode) {
        boolean isOrgWide = ALL_PROVIDERS_TOKEN.equals(providerToken);

        // 시계열 데이터 추출: 최근 60분(59분 전 ~ 현재 분) 동안의 Redis Key를 순회하며 카운트를 읽어옴
        List<ClickResponse.RealtimeClickCount> timeSeriesData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 59; i >= 0; i--) {
            String minute = now.minusMinutes(i).format(MINUTE_FORMATTER);

            // 조직 전체면 기존 키 그대로, 플랫폼이면 provider 추가
            String key = isOrgWide
                    ? String.format("click:%s:org:%s:%s", mode, orgId, minute)
                    : String.format("click:%s:org:%s:provider:%s:%s", mode, orgId, providerToken, minute);

            String value = redisUtil.getData(key);
            long count = 0L;
            if (value != null) {
                try {
                    count = Long.parseLong(value);
                } catch (NumberFormatException e) { // null 이 아닌데 파싱 실패 → invalid 값
                    log.warn("Redis 내부 invalid 한 클릭수 count 값 존재. key={}, value={}", key, value);
                }
            }
            timeSeriesData.add(new ClickResponse.RealtimeClickCount(minute, count));
        }

        boolean hasSuspect = false;
        DashboardResponse.SuspectDetail detail = null;

        // 클릭 봇 의심 경고는 real 모드일때만 동작하도록 설정
        // dummy 모드에서도 봇 의심 경고 표시 필요할 시 해당 코드 변경
        if ("real".equalsIgnoreCase(mode)) {
            String suspectAlertKey = isOrgWide ?
                    "click:suspect:alert:org:" + orgId
                    : String.format("click:suspect:alert:org:%s:provider:%s", orgId, providerToken);

            String suspectJson = redisUtil.getData(suspectAlertKey);

            if (suspectJson != null) {
                try {
                    detail = objectMapper.readValue(suspectJson, DashboardResponse.SuspectDetail.class);
                    hasSuspect = true;
                    redisUtil.deleteData(suspectAlertKey);
                } catch (JsonProcessingException e) {
                    log.error("이상 징후 JSON 파싱 실패. key={}, error={}", suspectAlertKey, e.getMessage(), e);
                }
            }
        }

        // 조직 전체면 provider = null, 플랫폼이면 해당 provider 명
        String providerForResponse = isOrgWide ? null : providerToken;

        return DashboardConverter.toRealTimeGraphResponse(
                providerForResponse, timeSeriesData, mode, hasSuspect, detail);
    }

    ///플랫폼별 클릭수 조회 관련 헬퍼 메서드 추가
    // provider 미지정 → ALL, 지정 시 NAVER/GOOGLE/META 만 허용
    private String parseProviderToken(String providerType) {
        if (providerType == null || providerType.isBlank()) {
            return ALL_PROVIDERS_TOKEN;
        }
        try {
            Provider provider = Provider.valueOf(providerType.trim().toUpperCase());
            if (!SUPPORTED_PROVIDERS.contains(provider)) {
                throw new DashboardException(DashboardErrorCode.PROVIDER_NOT_VALID);
            }
            return provider.name();
        } catch (IllegalArgumentException e) {
            throw new DashboardException(DashboardErrorCode.PROVIDER_NOT_VALID);
        }
    }

    // mode 값 파싱 : dummy || real
    private String parseMode(String mode) {
        return Optional.ofNullable(mode)
                .map(String::toLowerCase)
                .filter(m -> m.equals("dummy") || m.equals("real"))
                .orElseThrow(() -> new DashboardException(DashboardErrorCode.INVALID_MODE_PARAM));
    }

    // 클라이언트로 데이터 전송 및 예외 처리
    private void sendToClient(String routingKey, String emitterId, SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name("org-click-update") // 프론트엔드가 이 이름으로 이벤트를 리스닝합니다.
                    .data(data));             // DataResponse로 감싸진 JSON 객체가 전송됨
        } catch (IOException | IllegalStateException e) {
            // 전송 중 에러(클라이언트 강제 종료 등) 발생 시 저장소에서 즉시 삭제
            // 여기서 발생하는 IOException 은 실제 오류의 개념보단 "사용자가 우리 서비스 탭을 종료" 했음의 의미로 본다.
            // 따라서 DashboardException 과 같은 오류를 던지지 않고, 해당 SseEmitter 를 삭제하고 로그를 남기고 넘긴다.
            emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId);
            log.info("SSE 연결 끊어짐. Emitter 삭제 처리: {}", emitterId);
        } catch (Exception e) {
            // 2. JSON 직렬화 실패 등 예상치 못한 치명적인 런타임 서버 에러
            // 스케줄러 루프가 죽는 것을 막기 위해 최상위 Exception으로 한번 더 catch
            emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId);
            log.error("SSE 데이터 전송 중 예기치 않은 오류 발생. Emitter 강제 삭제 (emitterId: {})", emitterId, e);
        }
    }
}
