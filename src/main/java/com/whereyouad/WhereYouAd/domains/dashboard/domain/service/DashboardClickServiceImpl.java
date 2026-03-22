package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.SseEmitterRepository;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.application.mapper.DashboardConverter;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.DashboardException;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.code.DashboardErrorCode;
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

    private final SseEmitterRepository emitterRepository;
    private final RedisUtil redisUtil;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgRepository orgRepository;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");


    // 구독 (클라이언트 연결)
    public SseEmitter subscribe(Long userId, Long orgId, String mode) {

        orgRepository.findById(orgId).
                orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        // 라우팅 키 및 Emitter ID 생성
        // 라우팅 키 예: "1_real" (1번 조직의 실제 트래픽 채널)
        String safeMode = Optional.ofNullable(mode)
                .map(String::toLowerCase)
                .filter(m -> m.equals("dummy") || m.equals("real"))
                .orElseThrow(() -> new DashboardException(DashboardErrorCode.INVALID_MODE_PARAM));

        String routingKey = orgId + "_" + safeMode;
        // 동시 접속한 여러 유저(또는 다중 탭)를 식별하기 위해 UUID 추가
        String emitterId = userId + "_" + UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // Emitter 생명주기 콜백 설정 (연결 종료, 타임아웃, 에러 발생 시 메모리 누수 방지를 위해 저장소에서 제거)
        emitter.onCompletion(() -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));
        emitter.onError((e) -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));

        emitterRepository.save(routingKey, emitterId, emitter);

        // 최초 연결 시 즉시 데이터 전송 (프론트엔드 차트 초기 렌더링용)
        sendToClient(routingKey, emitterId, emitter, DataResponse.from(getOrgGraphData(orgId, safeMode)));

        return emitter;
    }

    // 1초마다 백그라운드에서 실행되며, 현재 연결된 모든 클라이언트에게 최신 데이터를 브로드캐스팅하는 스케줄러
    // TODO : 지금 로직에선 백 서버에서 프론트로 1초마다 SseEmitter 를 통해 클릭 데이터를 전송하고 있음.
    //        1초마다 전송이 서버 성능에 부하를 많이 줄지?, 만약 부하가 크다면 1초보다 더 길게 주기를 잡아야할지?
    @Scheduled(fixedRate = 1000)
    public void broadcastRealTimeClicks() {
        // 현재 구독자가 있는 채널(라우팅 키) 목록만 가져옴 (구독자가 없으면 Redis 조회를 생략하여 리소스 절약)
        Set<String> activeRoutingKeys = emitterRepository.findAllRoutingKeys();

        //모든 구독자 존재 채널(라우팅 키) 에 대하여,
        for (String routingKey : activeRoutingKeys) {
            //orgId, mode 추출
            String[] parts = routingKey.split("_");
            Long orgId = Long.parseLong(parts[0]);
            String mode = parts[1];

            // 이번 턴에 전송할 데이터(60분치 시계열 배열 + 봇 알림)를 Redis에서 조합
            DashboardResponse.RealTimeGraphResponse payload = getOrgGraphData(orgId, mode);
            DataResponse<DashboardResponse.RealTimeGraphResponse> responseBody = DataResponse.from(payload);

            // 해당 채널(예: 1번 조직_real)을 보고 있는 모든 유저의 Emitter를 꺼내서 전송
            Map<String, SseEmitter> sseEmitters = emitterRepository.findAllByRoutingKey(routingKey);
            sseEmitters.forEach((emitterId, emitter) -> {
                sendToClient(routingKey, emitterId, emitter, responseBody);
            });
        }
    }

    // 내부 메서드: 조직의 60분 시계열 데이터와 알림 정보를 묶어서 반환
    private DashboardResponse.RealTimeGraphResponse getOrgGraphData(Long orgId, String mode) {
        // 시계열 데이터 추출: 최근 60분(59분 전 ~ 현재 분) 동안의 Redis Key를 순회하며 카운트를 읽어옴
        List<ClickResponse.RealtimeClickCount> timeSeriesData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 59; i >= 0; i--) {
            String minute = now.minusMinutes(i).format(MINUTE_FORMATTER);

            // Redis Key 포맷
            // 예: click:real:org:1:202603211530
            String key = String.format("click:%s:org:%s:%s", mode, orgId, minute);

            String value = redisUtil.getData(key);
            long count = 0L;
            try { //Long 타입 파싱에 대한 예외처리
                count = Long.parseLong(value);
            } catch (NumberFormatException e) { //invalid 형식 값 존재 시 로그 처리
                log.warn("Redis 내부 invalid 한 클릭수 count 값 존재. key={}, value={}", key, value);
            }
            timeSeriesData.add(new ClickResponse.RealtimeClickCount(minute, count));
        }

        // 이상 징후(봇) 알림 추출: 해당 조직에서 발생한 부정 클릭 정보가 있는지 확인
        // 예: click:suspect:alert:org:1
        String suspectAlertKey = "click:suspect:alert:org:" + orgId;
        String suspectJson = redisUtil.getData(suspectAlertKey);

        boolean hasSuspect = false;
        DashboardResponse.SuspectDetail detail = null;

        // Redis에 알림 데이터가 존재한다면 (봇이 감지되었다면)
        if (suspectJson != null) {
            hasSuspect = true;
            try {
                detail = objectMapper.readValue(suspectJson, DashboardResponse.SuspectDetail.class);
                // 알림은 1회성이므로, 읽자마자 삭제하여 경고가 중복으로 뜨는 것을 방지
                redisUtil.deleteData(suspectAlertKey);
            } catch (JsonProcessingException e) {
                log.error("이상 징후 JSON 파싱 실패", e);
            }
        }

        return DashboardConverter.toRealTimeGraphResponse(timeSeriesData, mode, hasSuspect, detail);
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
