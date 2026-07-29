package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.NotificationAlertEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationEventProducer implements NotificationEventProducer {

    private static final String TOPIC = "notification-alert-events";

    private final KafkaTemplate<String, NotificationAlertEvent> kafkaTemplate;

    @Override
    public void produce(NotificationAlertEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrgId()), event);
        log.debug("[Kafka] 외부 알림 이벤트 발행: orgId={}, type={}", event.getOrgId(), event.getType());
    }
}
