package com.whereyouad.WhereYouAd.domains.user.domain.service.scheduler;

import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgService;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.AuthProviderAccountRepository;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.aws.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeleteExecutor {

    private final OrgService orgService;
    private final OrgMemberRepository orgMemberRepository;
    private final AuthProviderAccountRepository authProviderAccountRepository;
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;

    // Soft Delete (status = DELETED) 상태이고 deletedAt 으로부터 30일 이상 지난 회원에 대해
    // (1) 본인이 owner 인 Soft Deleted Organization 과 관련 엔티티 Hard Delete
    // (2) 본인 소속 OrgMember Hard Delete
    // (3) profileImageUrl S3 이미지 삭제
    // (4) User Hard Delete
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void hardDeleteSingleUser(User user) {
        // 기존 hardDeleteSingleUser 로직 이동
        orgService.removeOrganizationsOwnedBySoftDeletedUser(user.getId());
        orgMemberRepository.deleteByUserId(user.getId());
        authProviderAccountRepository.deleteByUserId(user.getId());
        userRepository.delete(user);

        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null) {
            try {
                s3UploadService.deleteImageFromUrl(profileImageUrl);
            } catch (Exception e) {
                log.warn("S3 프로필 이미지 삭제 실패 - userId: {}", user.getId(), e);
            }
        }
    }
}
