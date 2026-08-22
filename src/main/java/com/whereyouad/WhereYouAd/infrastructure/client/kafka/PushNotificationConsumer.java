package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushDeliveryResult;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushDeliveryTarget;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;
import com.whereyouad.WhereYouAd.domains.notification.application.mapper.NotificationConverter;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.push.BrowserPushDataAccess;
import com.whereyouad.WhereYouAd.infrastructure.client.webpush.WebPushClient;
import com.whereyouad.WhereYouAd.infrastructure.client.webpush.WebPushSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationConsumer {

    private final BrowserPushDataAccess dataAccess;
    private final WebPushClient webPushClient;
    private final ObjectMapper objectMapper;

    // 웹 푸시 발송 이벤트 수신. 발송 흐름:
    // 1) delivery + subscription 로드 (짧은 read tx)
    // 2) 각 subscription 으로 Web Push 전송 (트랜잭션 밖)
    // 3) 결과 일괄 반영 (짧은 write tx)
    @KafkaListener(
            topics = KafkaPushNotificationEventProducer.TOPIC,
            groupId = "where-you-ad-group",
            containerFactory = "pushKafkaListenerContainerFactory")
    public void consume(PushNotificationEvent event) {
        try {
            List<PushDeliveryTarget> targets = dataAccess.loadTargets(event.getNotificationId());
            if (targets.isEmpty()) {
                log.debug("[웹푸시] 발송 대상 없음 orgId={}, notificationId={}",
                        event.getOrgId(), event.getNotificationId());
                return;
            }

            String payload = buildPayload(event);
            List<PushDeliveryResult> results = new ArrayList<>(targets.size());
            for (PushDeliveryTarget target : targets) {
                if (!target.hasSubscription()) {
                    results.add(NotificationConverter.toPushDeliveryResult(target, 0, "no subscription"));
                    continue;
                }
                results.add(send(target, payload));
            }

            dataAccess.recordResults(results);
            log.info("[웹푸시] 발송 완료 orgId={}, notificationId={}, 발송={}",
                    event.getOrgId(), event.getNotificationId(), results.size());
        } catch (Exception e) {
            log.error("[웹푸시] 발송 처리 실패 orgId={}, notificationId={}",
                    event.getOrgId(), event.getNotificationId(), e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("웹푸시 발송 처리 실패", e);
        }
    }

    private PushDeliveryResult send(PushDeliveryTarget target, String payload) {
        try {
            int status = webPushClient.send(target.endpoint(), target.p256dhKey(), target.authSecret(), payload);
            return NotificationConverter.toPushDeliveryResult(target, status, null);
        } catch (WebPushSendException e) {
            log.warn("[웹푸시] 전송 오류 subscriptionId={}, reason={}", target.subscriptionId(), e.getMessage());
            return NotificationConverter.toPushDeliveryResult(target, e.getStatusCode(), e.getMessage());
        }
    }

    // Service Worker 가 event.data.json() 으로 파싱하는 payload
    private String buildPayload(PushNotificationEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", event.getNotificationId());
        payload.put("type", event.getType() == null ? null : event.getType().name());
        payload.put("title", event.getTitle());
        payload.put("body", event.getBody());
        payload.put("linkUrl", event.getLinkUrl());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("웹푸시 payload 직렬화 실패", e);
        }
    }
}
