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

    // 클릭 급증 / 봇 클릭 감지 등에서 발행한 알림 이벤트를 받아 디스코드/슬랙으로 발송
    @KafkaListener(topics = "notification-alert-events", groupId = "where-you-ad-group")
    public void consume(NotificationAlertEvent event) {
        try {
            notificationService.sendApiAlarmToOrg(event.getOrgId(), event.getType(), event.getTitle(), event.getMessage());
        } catch (Exception e) {
            log.error("[외부 알림 발송 실패] orgId={}, type={}, reason={}", event.getOrgId(), event.getType(), e.getMessage(), e);
        }
    }
}
