package com.whereyouad.WhereYouAd.domains.user.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
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

    // 매주 토요일 → 일요일로 넘어가는 새벽 2시 (일요일 02:00)
    @Scheduled(cron = "0 0 2 * * SUN")
    public void hardDeleteUsers() {
        LocalDate threshold = LocalDate.now().minusDays(SOFT_DELETE_RETENTION_DAYS);
        log.info("Soft Delete 회원 Hard Delete 스케줄러 실행 - threshold: {}", threshold);

        List<User> targetUsers =
                userRepository.findAllByStatusAndDeletedAtBefore(UserStatus.DELETED, threshold);

        int successCount = 0;
        for (User user : targetUsers) {
            try {
                userDeleteExecutor.hardDeleteSingleUser(user);
                successCount++;
            } catch (Exception e) {
                log.error("회원 Hard Delete 실패 - userId: {}", user.getId(), e);
            }
        }

        log.info("Soft Delete 회원 Hard Delete 완료 - 대상: {}, 성공: {}",
                targetUsers.size(), successCount);
    }
}