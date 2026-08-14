package com.whereyouad.WhereYouAd.domains.notification.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private static final int RETENTION_DAYS = 30;
    private static final int CHUNK_SIZE = 1000;

    private final NotificationRepository notificationRepository;
    private final NotificationCleanupExecutor notificationCleanupExecutor;

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void deleteOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("오래된 알림 내역 삭제 스케줄러 실행 - threshold: {}", threshold);

        int totalDeleted = 0;
        while (true) {
            List<Long> notificationIds =
                    notificationRepository.findIdsByCreatedAtBefore(threshold, PageRequest.of(0, CHUNK_SIZE));

            if (notificationIds.isEmpty()) {
                break;
            }

            notificationCleanupExecutor.deleteNotificationChunk(notificationIds);
            totalDeleted += notificationIds.size();
        }

        log.info("오래된 알림 내역 삭제 완료 - 삭제 알림 갯수: {}", totalDeleted);
    }
}
