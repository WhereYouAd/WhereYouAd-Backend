package com.whereyouad.WhereYouAd.domains.notification.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.notification.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private static final int RETENTION_DAYS = 30;
    private static final int CHUNK_SIZE = 1000;

    private final NotificationRepository notificationRepository;
    private final NotificationCleanupExecutor notificationCleanupExecutor;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteOldNotifications() {
        // 스케줄러 실행 시점으로 부터 만료 기준일 (30일 과거) 설정
        LocalDateTime threshold = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(RETENTION_DAYS).atStartOfDay();
        log.info("오래된 알림 내역 삭제 스케줄러 실행 - threshold: {}", threshold);

        int totalDeleted = 0;
        while (true) {
            // 삭제 대상인 알림 내역 (30일 이상 지난 내역) Id 를 List 조회
            List<Long> notificationIds =
                    notificationRepository.findIdsByCreatedAtBefore(threshold, PageRequest.of(0, CHUNK_SIZE));

            // 삭제 대상인 알림 내역이 없으면 종료
            if (notificationIds.isEmpty()) {
                break;
            }

            // 청크 단위 알림 내역 삭제
            notificationCleanupExecutor.deleteNotificationChunk(notificationIds);
            totalDeleted += notificationIds.size();
        }

        log.info("오래된 알림 내역 삭제 완료 - 삭제 알림 갯수: {}", totalDeleted);
    }
}
