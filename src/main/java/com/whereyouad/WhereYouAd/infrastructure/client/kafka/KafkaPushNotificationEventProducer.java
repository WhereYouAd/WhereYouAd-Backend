package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.PushNotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPushNotificationEventProducer implements PushNotificationEventProducer {

    static final String TOPIC = "notification-push-events";

    private final KafkaTemplate<String, PushNotificationEvent> kafkaTemplate;

    @Override
    public void produce(PushNotificationEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.getOrgId()), event).get(10, TimeUnit.SECONDS);
            log.debug("[Kafka] 웹 푸시 이벤트 발행 성공: orgId={}, notificationId={}",
                    event.getOrgId(), event.getNotificationId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw publicationException(event, e);
        } catch (ExecutionException | TimeoutException e) {
            throw publicationException(event, e);
        }
    }

    private IllegalStateException publicationException(PushNotificationEvent event, Exception cause) {
        log.error("[Kafka] 웹 푸시 이벤트 발행 실패: orgId={}, notificationId={}",
                event.getOrgId(), event.getNotificationId(), cause);
        return new IllegalStateException("웹 푸시 Kafka 이벤트 발행 실패", cause);
    }
}
