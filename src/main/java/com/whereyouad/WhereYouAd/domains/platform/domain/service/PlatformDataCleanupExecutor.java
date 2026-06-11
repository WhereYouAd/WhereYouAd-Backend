package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.project.persistence.repository.ProjectRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformDataCleanupExecutor {

    private static final int BATCH_SIZE = 1000;

    private final UserRepository userRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final ProjectRepository projectRepository;
    private final ClickLogRepository clickLogRepository;
    private final MetricFactRepository metricFactRepository;

    // 권한 & 소유자 검증 + 삭제에 영향받는 projectId 반환 (read-only & 짧은 트랜잭션)
    @Transactional(readOnly = true)
    public List<Long> verifyAndCollectProjectIds(Long userId, Long orgId, Long accountId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ORG_MEMBER_NOT_FOUND));

        if (orgMember.getRole() != OrgRole.ADMIN) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_FORBIDDEN);
        }

        PlatformAccount platformAccount = platformAccountRepository.findById(accountId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ACCOUNT_NOT_FOUND));

        if (!platformAccount.getOrganization().getId().equals(orgId)) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_ACCOUNT_NOT_BELONG_TO_ORG);
        }

        platformConnectionRepository.findByUserIdAndPlatformAccountId(userId, accountId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_NOT_ACCOUNT_OWNER));

        return adCampaignRepository.findDistinctProjectIdsByPlatformAccountId(accountId);
    }

    // 요청자 권한 검증 없이 삭제에 영향받는 projectId 만 수집 (회원 탈퇴 스케줄러 등 시스템 내부 호출용)
    @Transactional(readOnly = true)
    public List<Long> collectProjectIds(Long accountId) {
        return adCampaignRepository.findDistinctProjectIdsByPlatformAccountId(accountId);
    }

    // 청크 단위로 ClickLog 삭제 — 메인 트랜잭션과 분리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteClickLogChunk(Long platformAccountId) {
        int deleted = clickLogRepository.deleteByPlatformAccountIdInBatch(platformAccountId, BATCH_SIZE);
        if (deleted > 0) {
            log.debug("ClickLog 청크 삭제 - platformAccountId={}, deleted={}", platformAccountId, deleted);
        }
        return deleted;
    }

    // 청크 단위로 MetricFact 삭제
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteMetricFactChunk(Long platformAccountId) {
        int deleted = metricFactRepository.deleteByPlatformAccountIdInBatch(platformAccountId, BATCH_SIZE);
        if (deleted > 0) {
            log.debug("MetricFact 청크 삭제 - platformAccountId={}, deleted={}", platformAccountId, deleted);
        }
        return deleted;
    }

    // AdCampaign + PlatformConnection + PlatformAccount 원자적 삭제
    @Transactional
    public void deleteAccountAndRelations(Long accountId) {
        PlatformAccount platformAccount = platformAccountRepository.findById(accountId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ACCOUNT_NOT_FOUND));

        if (platformAccount == null) {
            log.info("이미 삭제된 PlatformAccount - accountId={}, 정리 skip", accountId);
            return;
        }

        List<AdCampaign> campaigns = adCampaignRepository.findByPlatformAccount(platformAccount);

        if (!campaigns.isEmpty()) {
            adCampaignRepository.deleteAll(campaigns);
            adCampaignRepository.flush();
            //AdCampaign 삭제 시 CascadeType.ALL 로 인해 연관된 AdGroup, AdContent 도 함꼐 제거됨
        }

        List<PlatformConnection> connections = platformConnectionRepository.findAllByPlatformAccount_Id(accountId);

        if (!connections.isEmpty()) {
            platformConnectionRepository.deleteAll(connections);
            platformConnectionRepository.flush();
        }

        platformAccountRepository.delete(platformAccount);
    }

    //빈 Project 1개 삭제
    @Transactional
    public void deleteEmptyProject(Long projectId) {
        // 연관된 AdCampaign, MetricFact 가 없을 경우에만 Project 삭제 진행
        if (adCampaignRepository.countByProject_Id(projectId) == 0 && metricFactRepository.countByProject_Id(projectId) == 0) {
            projectRepository.deleteById(projectId);
        }
    }
}