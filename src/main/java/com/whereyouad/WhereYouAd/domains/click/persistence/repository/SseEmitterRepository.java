package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseEmitterRepository {

    // 구조: Map<"orgId_providerType", Map<"emitterId", SseEmitter>>
    private final Map<String, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void save(String routingKey, String emitterId, SseEmitter emitter) {
        emitters.computeIfAbsent(routingKey, k -> new ConcurrentHashMap<>()).put(emitterId, emitter);
    }

    public void deleteByRoutingKeyAndEmitterId(String routingKey, String emitterId) {
        if (emitters.containsKey(routingKey)) {
            emitters.get(routingKey).remove(emitterId);
            if (emitters.get(routingKey).isEmpty()) {
                emitters.remove(routingKey);
            }
        }
    }

    public Map<String, SseEmitter> findAllByRoutingKey(String routingKey) {
        return emitters.getOrDefault(routingKey, new ConcurrentHashMap<>());
    }

    public Set<String> findAllRoutingKeys() {
        return emitters.keySet();
    }
}
