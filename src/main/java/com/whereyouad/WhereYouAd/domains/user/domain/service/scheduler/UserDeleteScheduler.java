package com.whereyouad.WhereYouAd.domains.user.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgService;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.AuthProviderAccountRepository;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.aws.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeleteScheduler {

    private static final int SOFT_DELETE_RETENTION_DAYS = 30;

    private final UserRepository userRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final AuthProviderAccountRepository authProviderAccountRepository;
    private final OrgService orgService;
    private final S3UploadService s3UploadService;

    // 매주 토요일 → 일요일로 넘어가는 새벽 2시 (일요일 02:00)
    // Soft Delete (status = DELETED) 상태이고 deletedAt 으로부터 30일 이상 지난 회원에 대해
    // (1) 본인이 owner 인 Soft Deleted Organization 과 관련 엔티티 Hard Delete
    // (2) 본인 소속 OrgMember Hard Delete
    // (3) 본인 소속 AuthProviderAccount Hard Delete
    // (4) profileImageUrl S3 이미지 삭제
    // (5) User Hard Delete
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void hardDeleteUsers() {
        LocalDate threshold = LocalDate.now().minusDays(SOFT_DELETE_RETENTION_DAYS);
        log.info("Soft Delete 회원 Hard Delete 스케줄러 실행 - threshold: {}", threshold);

        List<User> targetUsers =
                userRepository.findAllByStatusAndDeletedAtBefore(UserStatus.DELETED, threshold);

        int successCount = 0;
        for (User user : targetUsers) {
            try {
                hardDeleteSingleUser(user);
                successCount++;
            } catch (Exception e) {
                log.error("회원 Hard Delete 실패 - userId: {}", user.getId(), e);
            }
        }

        log.info("Soft Delete 회원 Hard Delete 완료 - 대상: {}, 성공: {}",
                targetUsers.size(), successCount);
    }

    private void hardDeleteSingleUser(User user) {
        Long userId = user.getId();
        String profileImageUrl = user.getProfileImageUrl();

        // 본인이 owner 이고 본인만 속한 Soft Deleted 조직 + 부수 데이터(OrgInvitation/Timeline/AIInsightReport) 정리
        orgService.removeOrganizationsOwnedBySoftDeletedUser(userId);

        // 본인이 속한 OrgMember 전부 제거
        // 다른 회원이 owner 인 조직에서의 멤버십
        orgMemberRepository.deleteByUserId(userId);

        // 소셜 로그인 연동 정보 제거
        authProviderAccountRepository.deleteByUserId(userId);

        // User Hard Delete
        userRepository.delete(user);

        // 프로필 이미지 S3 삭제
        if (profileImageUrl != null) {
            try {
                s3UploadService.deleteImageFromUrl(profileImageUrl);
            } catch (Exception e) {
                log.warn("회원 Hard Delete - S3 프로필 이미지 삭제 실패: userId={}, url={}",
                        userId, profileImageUrl, e);
            }
        }
    }
}