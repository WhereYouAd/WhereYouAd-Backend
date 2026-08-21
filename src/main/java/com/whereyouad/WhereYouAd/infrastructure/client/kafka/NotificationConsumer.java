package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.NotificationAlertEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    // 클릭 급증 / 봇 클릭 감지 등에서 발행한 알림 이벤트를 받아 디스코드/슬랙 + 브라우저 푸시로 팬아웃
    @KafkaListener(topics = "notification-alert-events", groupId = "where-you-ad-group")
    public void consume(NotificationAlertEvent event) {
        try {
            notificationService.sendApiAlarmToOrg(event.getOrgId(), event.getType(), event.getTitle(), event.getMessage());
        } catch (Exception e) {
            log.error("[외부 알림 발송 실패] orgId={}, type={}, reason={}", event.getOrgId(), event.getType(), e.getMessage(), e);
        }
        try {
            // 브라우저 푸시 (자체 Kafka topic 을 다시 발행하므로 여기서는 트리거만)
            notificationService.sendBrowserPushToOrg(
                    event.getOrgId(), event.getType(), event.getTitle(), event.getMessage(), null);
        } catch (Exception e) {
            log.error("[웹푸시 트리거 실패] orgId={}, type={}, reason={}", event.getOrgId(), event.getType(), e.getMessage(), e);
        }
    }
}
