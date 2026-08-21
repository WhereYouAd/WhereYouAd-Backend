package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.PushNotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPushNotificationEventProducer implements PushNotificationEventProducer {

    private static final String TOPIC = "notification-push-events";

    private final KafkaTemplate<String, PushNotificationEvent> kafkaTemplate;

    @Override
    public void produce(PushNotificationEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrgId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] 웹 푸시 이벤트 발행 실패: orgId={}, notificationId={}",
                                event.getOrgId(), event.getNotificationId(), ex);
                    } else {
                        log.debug("[Kafka] 웹 푸시 이벤트 발행 성공: orgId={}, notificationId={}",
                                event.getOrgId(), event.getNotificationId());
                    }
                });
    }
}
