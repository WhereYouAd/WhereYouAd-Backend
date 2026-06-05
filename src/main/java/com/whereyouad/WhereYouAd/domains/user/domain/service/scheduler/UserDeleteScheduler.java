package com.whereyouad.WhereYouAd.domains.user.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.platform.domain.service.PlatformService;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeleteScheduler {

    private static final int SOFT_DELETE_RETENTION_DAYS = 30;

    private final UserRepository userRepository;
    private final UserDeleteExecutor userDeleteExecutor;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final PlatformService platformService;

    // 매일 새벽 3시
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void hardDeleteUsers() {
        LocalDate threshold = LocalDate.now().minusDays(SOFT_DELETE_RETENTION_DAYS);
        log.info("Soft Delete 회원 Hard Delete 스케줄러 실행 - threshold: {}", threshold);

        // 삭제 대상 User 엔티티의 Id 를 리스트 조회
        List<Long> targetUserIds =
                userRepository.findIdsByStatusAndDeletedAtBefore(UserStatus.DELETED, threshold);

        int successCount = 0;
        for (Long userId : targetUserIds) {
            try {
                // 광고 플랫폼 연동 자동 해제 (계정 + 연관 광고 엔티티/ClickLog/MetricFact/비어있는 Project 정리)
                // PlatformAccount → Organization FK 위반 방지를 위해 조직 Hard Delete 전에 수행
                List<Long> accountIds = platformConnectionRepository.findDistinctAccountIdsByUserId(userId);
                for (Long accountId : accountIds) {
                    platformService.disconnectAccountBySystem(accountId);
                }

                // 회원/조직/멤버 Hard Delete
                userDeleteExecutor.hardDeleteSingleUser(userId);
                successCount++;
            } catch (Exception e) {
                log.error("회원 Hard Delete 실패 - userId: {}", userId, e);
            }
        }

        log.info("Soft Delete 회원 Hard Delete 완료 - 대상: {}, 성공: {}",
                targetUserIds.size(), successCount);
    }
}