package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.NotificationAlertEvent;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationInboxClaimResult;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationAlertInboxService;
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
    private final NotificationAlertInboxService inboxService;

    // 클릭 급증 / 봇 클릭 감지 등에서 발행한 알림 이벤트를 받아 디스코드/슬랙 + 브라우저 푸시로 팬아웃
    @KafkaListener(
            topics = "notification-alert-events",
            groupId = "where-you-ad-group",
            containerFactory = "notificationKafkaListenerContainerFactory")
    public void consume(NotificationAlertEvent event) {
        String eventId = event.getEventId();
        boolean hasEventId = eventId != null && !eventId.isBlank();
        if (hasEventId) {
            NotificationInboxClaimResult claimResult = inboxService.claim(eventId);
            if (claimResult == NotificationInboxClaimResult.COMPLETED) {
                log.debug("[알림 이벤트] 완료된 중복 이벤트 skip eventId={}", eventId);
                return;
            }
            if (claimResult == NotificationInboxClaimResult.PROCESSING) {
                throw new IllegalStateException("알림 이벤트가 다른 consumer에서 처리 중입니다. eventId=" + eventId);
            }
        } else {
            log.warn("[알림 이벤트] eventId 없는 레거시 이벤트를 중복 방지 없이 처리합니다.");
        }

        try {
            notificationService.sendApiAlarmToOrgOrThrow(
                    event.getOrgId(), event.getType(), event.getTitle(), event.getMessage());
            notificationService.sendBrowserPushToOrgOrThrow(
                    event.getOrgId(), event.getType(), event.getTitle(), event.getMessage(), null);
            if (hasEventId) {
                inboxService.complete(eventId);
            }
        } catch (Exception e) {
            if (hasEventId) {
                inboxService.fail(eventId);
            }
            log.error("[알림 이벤트 처리 실패] orgId={}, type={}, reason={}",
                    event.getOrgId(), event.getType(), e.getMessage(), e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("알림 이벤트 처리 실패", e);
        }
    }
}
