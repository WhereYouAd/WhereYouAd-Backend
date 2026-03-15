package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.SseEmitterRepository;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardClickServiceImpl implements DashboardClickService {

    private final SseEmitterRepository emitterRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 30; // 30분 유지
    private final OrgMemberRepository orgMemberRepository;
    private final OrgRepository orgRepository;

    // 클라이언트 SSE 구독 요청 처리
    @Override
    public SseEmitter subscribe(Long userId, Long orgId, Provider provider) {
        orgRepository.findById(orgId).
                orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        // 라우팅 키 생성 (예: "1_KAKAO", "1_NAVER", 파라미터가 없으면 "1_ALL")
        String routingProvider = (provider != null) ? provider.name() : "ALL";
        String routingKey = orgId + "_" + routingProvider;

        // 어떤 유저의 어떤 브라우저 탭인지 식별하기 위한 고유 ID
        // 같은 회원이 두개 이상의 브라우저 탭을 열었을 경우, user_id 가 같아 첫번째 탭에서 실시간 지표가 멈춰버리고, 두번째 탭에서 지표가 시작될 수 있다.
        // 따라서 회원 식별자를 user_id + UUID 를 사용해, 회원 1명이 여러 브라우저 탭으로 열더라도 모두 정상동작하도록 설계
        String emitterId = userId + "_" + UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 연결이 끊어지거나 타임아웃 발생 시 저장소에서 제거되도록 콜백 등록
        emitter.onCompletion(() -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));
        emitter.onError((e) -> emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId));

        emitterRepository.save(routingKey, emitterId, emitter);

        // 503 Service Unavailable 에러 방지를 위한 최초 연결 더미 데이터 전송
        DashboardResponse.RealTimeClickResponse initialData =
                new DashboardResponse.RealTimeClickResponse(0L, routingProvider);

        sendToClient(routingKey, emitterId, emitter, DataResponse.from(initialData));

        return emitter;
    }

    // 1초마다 Redis에서 실시간 클릭수를 조회하여 SSE 구독 중인 클라이언트들에게 브로드캐스팅
    @Scheduled(fixedRate = 1000)
    public void broadcastRealTimeClicks() {
        Set<String> activeRoutingKeys = emitterRepository.findAllRoutingKeys();

        for (String routingKey : activeRoutingKeys) {
            // Redis에서 집계된 실시간 클릭수 조회 (Kafka Consumer가 업데이트해둔 값)
            String redisKey = "org:clicks:realtime:" + routingKey;
            String countStr = redisTemplate.opsForValue().get(redisKey);
            long clickCount = countStr != null ? Long.parseLong(countStr) : 0L;

            // 라우팅 키에서 provider 파싱 ("1_KAKAO" -> "KAKAO")
            String providerType = routingKey.split("_")[1];

            // DTO 데이터 생성
            DashboardResponse.RealTimeClickResponse payload =
                    new DashboardResponse.RealTimeClickResponse(clickCount, providerType);

            // DataResponse로 포장
            DataResponse<DashboardResponse.RealTimeClickResponse> responseBody = DataResponse.from(payload);

            // 해당 routingKey 를 구독 중인 모든 케이블(SseEmitter)에게 전송
            Map<String, SseEmitter> sseEmitters = emitterRepository.findAllByRoutingKey(routingKey);
            sseEmitters.forEach((emitterId, emitter) -> {
                sendToClient(routingKey, emitterId, emitter, responseBody);
            });
        }
    }

    // 클라이언트로 데이터 전송 및 예외 처리
    private void sendToClient(String routingKey, String emitterId, SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name("org-click-update") // 프론트엔드가 이 이름으로 이벤트를 리스닝합니다.
                    .data(data));             // DataResponse로 감싸진 JSON 객체가 전송됨
        } catch (IOException e) {
            // 전송 중 에러(클라이언트 강제 종료 등) 발생 시 저장소에서 즉시 삭제
            // 여기서 발생하는 IOException 은 실제 오류의 개념보단 "사용자가 우리 서비스 탭을 종료" 했음의 의미로 본다.
            // 따라서 DashboardException 과 같은 오류를 던지지 않고, 해당 SseEmitter 를 삭제하고 로그를 남기고 넘긴다.
            emitterRepository.deleteByRoutingKeyAndEmitterId(routingKey, emitterId);
            log.warn("SSE 연결 끊어짐. Emitter 삭제 처리: {}", emitterId);
        }
    }
}
