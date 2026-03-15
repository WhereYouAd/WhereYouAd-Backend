package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 대시보드 클라이언트들의 SSE 연결(SseEmitter)을 메모리 상에서 관리하는 저장소.
 * SseEmitter?
 * SseEmitter는 단순한 데이터(문자열, 숫자 등)가 아니라, Spring 서버와 프론트엔드(브라우저 탭) 간에
 * 한 번 맺어진 '살아있는 단방향 HTTP 연결 통로(네트워크 소켓)' 자체를 나타내는 객체입니다.
 * * [JPA(DB)를 사용하지 않고 ConcurrentHashMap(메모리)에 저장하는 이유]
 * 1. 직렬화 불가 (네트워크 상태 객체)
 * SseEmitter는 내부적으로 활성화된 네트워크 I/O 스트림, 스레드 상태, 메모리 주소 등
 * 동적인 상태를 들고 있으므로, MySQL 같은 RDBMS에 텍스트나 숫자로 변환(직렬화)하여 영구 저장할 수 없습니다.
 * * 2. 철저한 휘발성 (Lifecycle)
 * 사용자가 브라우저 탭을 닫거나 와이파이가 끊기면 그 즉시 해당 네트워크 연결은 무효화됩니다.
 * 끊어진 소켓 객체를 DB에 남겨두더라도 다시 살려낼 수 없으므로,
 * 애플리케이션 구동 중 서버의 RAM(메모리)에만 들고 있다가 연결 종료 시 즉각 폐기하는 것이 올바른 설계입니다.
 */

@Repository
public class SseEmitterRepository {

    // 구조: Map<"orgId_providerType", Map<"emitterId", SseEmitter>>
    // orgId_providerType -> 해당 실시간 클릭수 유형 단위 -> orgId 에 해당하는 조직 광고중 providerType 인 플랫폼의 광고 실시간 클릭수를 전파한다.
    // emitterId -> 회원 식별자 -> user_id + UUID
    // SseEmitter -> 실시간 클릭수를 스트림으로 전파하는 케이블 역할
    private final Map<String, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void save(String routingKey, String emitterId, SseEmitter emitter) {
        emitters.computeIfAbsent(routingKey, k -> new ConcurrentHashMap<>()).put(emitterId, emitter);
    }

    //routingKey 값과 emitterId 값 기반 메모리에 존재하는 SseEmitter 제거 메서드
    public void deleteByRoutingKeyAndEmitterId(String routingKey, String emitterId) {
        if (emitters.containsKey(routingKey)) {
            emitters.get(routingKey).remove(emitterId);
            if (emitters.get(routingKey).isEmpty()) {
                emitters.remove(routingKey);
            }
        }
    }

    //routingKey 값 가진 모든 SseEmitter 조회
    public Map<String, SseEmitter> findAllByRoutingKey(String routingKey) {
        return emitters.getOrDefault(routingKey, new ConcurrentHashMap<>());
    }

    //모든 SseEmitter 조회
    public Set<String> findAllRoutingKeys() {
        return emitters.keySet();
    }
}
