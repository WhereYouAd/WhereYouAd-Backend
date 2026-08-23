package com.whereyouad.WhereYouAd.domains.notification.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationDeliveryRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationRepository;
import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupExecutor {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @Transactional
    public void deleteNotificationChunk(List<Long> notificationIds) {
        int userNotificationCount = userNotificationRepository.deleteByNotificationIdIn(notificationIds);
        int deliveryCount = notificationDeliveryRepository.deleteByNotificationIdIn(notificationIds);
        notificationRepository.deleteAllByIdInBatch(notificationIds);

        log.debug("만료 알림 청크 삭제 - 알림(Notification): {}건, 회원 알림 내역(UserNotification): {}건, 발송 이력(NotificationDelivery): {}건",
                notificationIds.size(), userNotificationCount, deliveryCount);
    }
}
