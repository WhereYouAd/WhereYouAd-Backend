package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.request.PlatformRequest;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.PlatformResponse;
import com.whereyouad.WhereYouAd.domains.platform.application.mapper.PlatformConverter;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.domain.service.scheduler.PlatformDataCleanupExecutor;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.global.adapi.strategy.NaverAdAuthStrategy;
import com.whereyouad.WhereYouAd.global.exception.ErrorCode;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.client.NaverClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private final UserRepository userRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final PlatformDataCleanupExecutor platformDataCleanupExecutor;
    private final NaverClient naverClient;
    private final NaverAdAuthStrategy naverAdAuthStrategy;

    @Override
    public PlatformResponse.PlatformAccount addNaverAdAccount(Long userId, Long orgId, PlatformRequest.PlatformAccount dto) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 2. 조직 멤버 여부 검증
        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ORG_MEMBER_NOT_FOUND));

        // 3. ADMIN 권한 검증
        if (orgMember.getRole() != OrgRole.ADMIN) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_FORBIDDEN);
        }

        // 4. 중복 계정 체크
        if (platformAccountRepository.existsByExternalAccountIdAndOrganizationIdAndProvider(
                dto.customerId(), orgId, Provider.NAVER)) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_ACCOUNT_ALREADY_EXISTS);
        }

        // 5. 네이버 광고 API로 자격증명 검증
        validateNaverCredentials(dto.customerId(), dto.apiKey(), dto.secretKey());

        // 6. Organization (OrgMember에서 바로 참조)
        Organization organization = orgMember.getOrganization();

        // 7. PlatformAccount 저장
        PlatformAccount platformAccount = platformAccountRepository.save(
                PlatformConverter.toPlatformAccount(dto, organization)
        );

        // 8. PlatformConnection 저장
        platformConnectionRepository.save(
                PlatformConverter.toPlatformConnection(dto, user, platformAccount)
        );

        return PlatformConverter.toPlatformAccountResponse(platformAccount);
    }

    // 사용자의 특정 조직에 대한 광고 플랫폼 연동 정보 조회
    // ADMIN 만 요청 가능 & 실제 연동한 광고 플랫폼 계정 주인만 조회 가능(ADMIN 인데 계정 주인 아닌 경우 빈 리스트 반환)
    @Override
    public PlatformResponse.PlatformAccountListResponse getPlatformSyncInfos(Long userId, Long orgId) {
        // 회원 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        // 조직 회원 검증
        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ORG_MEMBER_NOT_FOUND));

        // ADMIN 아닐 시 요청 불가
        if (orgMember.getRole() != OrgRole.ADMIN) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_FORBIDDEN);
        }

        // userId, orgId 기반 PlatformConnection 조회 (삭제 대기(DISCONNECTED) 계정도 조회 가능
        List<PlatformConnection> connections = platformConnectionRepository.findByUserIdAndOrgId(userId, orgId);

        // DTO 로 변환 및 반환
        return PlatformConverter.toPlatformAccountListResponse(connections);
    }

    @Override
    public PlatformResponse.PlatformAccount updateNaverAdAccount(Long userId, Long orgId, PlatformRequest.PlatformAccount request) {
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        // 조직 멤버 여부 검증
        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ORG_MEMBER_NOT_FOUND));

        // ADMIN 권한 검증
        if (orgMember.getRole() != OrgRole.ADMIN) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_FORBIDDEN);
        }

        // 기존 PlatformAccount 조회
        Organization organization = orgMember.getOrganization();
        PlatformAccount platformAccount = platformAccountRepository
                .findByExternalAccountIdAndProviderAndOrganization(request.customerId(), Provider.NAVER, organization)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ACCOUNT_NOT_FOUND));

        // 새 자격증명으로 네이버 광고 API 검증
        validateNaverCredentials(request.customerId(), request.apiKey(), request.secretKey());

        // 기존 PlatformConnection 조회
        PlatformConnection connection = platformConnectionRepository
                .findByUserIdAndPlatformAccountId(userId, platformAccount.getId())
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));

        // 키값 및 secret 값 갱신 (apiKey / secretKey)
        connection.renewApiKey(request.apiKey(), request.secretKey());

        return PlatformConverter.toPlatformAccountResponse(platformAccount);
    }

    // 광고 플랫폼 연동 해제 (수동 요청)
    // 권한/소유자 검증 후 상태만 DISCONNECTED 로 변경하고 즉시 반환, 실제 데이터 삭제는 PlatformAccountCleanupScheduler 에서 비동기 진행
    @Override
    public void disconnectPlatform(Long userId, Long orgId, Long accountId) {
        // 검증 로직
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

        // 계정 소유자(연동 주인) 검증
        platformConnectionRepository.findByUserIdAndPlatformAccountId(userId, accountId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_NOT_ACCOUNT_OWNER));

        // 멱등성: 이미 삭제 대기 상태면 그대로 반환
        if (platformAccount.getStatus() == PlatformStatus.DISCONNECTED) {
            return;
        }

        // 상태만 DISCONNECTED 로 변경
        platformAccount.softDelete();
    }

    @Override
    public void reconnectPlatform(Long userId, Long orgId, Long accountId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ORG_MEMBER_NOT_FOUND));

        if (orgMember.getRole() != OrgRole.ADMIN) {
            throw new PlatformHandler(PlatformErrorCode.PLATFORM_FORBIDDEN);
        }

        // 계정 소유자(연동 주인) 검증
        platformConnectionRepository.findByUserIdAndPlatformAccountId(userId, accountId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_NOT_ACCOUNT_OWNER));

        PlatformAccount platformAccount = platformAccountRepository.findById(accountId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_ACCOUNT_NOT_FOUND));

        if (platformAccount.getStatus() != PlatformStatus.DISCONNECTED) {
            throw new PlatformHandler(PlatformErrorCode.NOT_DISCONNECTED_ACCOUNT);
        }

        platformAccount.reconnect();
    }

    // 시스템 내부 호출용 플랫폼 연동 해제 — 권한 검증 없이 계정 단위로 실제 데이터를 정리
    // 호출처: 회원 탈퇴 스케줄러(UserDeleteScheduler), 수동 연동 해제 정리 스케줄러(PlatformAccountCleanupScheduler)
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void disconnectAccountBySystem(Long accountId) {
        List<Long> projectIds = platformDataCleanupExecutor.collectProjectIds(accountId);
        cleanupAccount(accountId, projectIds);
    }

    // 계정 단위 데이터 정리 메서드화
    // ClickLog / MetricFact 청크 삭제 → AdCampaign + PlatformConnection + PlatformAccount 삭제 → 빈 Project 삭제
    // 대규모 엔티티 삭제를 위해 별도 처리 클래스 (PlatformDataCleanupExecutor) 에서 Chunk 단위 삭제 처리
    private void cleanupAccount(Long accountId, List<Long> projectIds) {
        int chunkDeleted; // 하나의 청크 당 삭제 갯수
        long totalClickLogDeleted = 0L; // ClickLog 전체 삭제 갯수
        long totalMetricFactDeleted = 0L; // MetricFact 전체 삭제 갯수

        // ClickLog 청크 정리 (REQUIRES_NEW)
        do {
            chunkDeleted = platformDataCleanupExecutor.deleteClickLogChunk(accountId);
            totalClickLogDeleted += chunkDeleted;
        } while (chunkDeleted > 0);
        log.info("ClickLog 삭제 완료 - platformAccountId={}, totalCount={}", accountId, totalClickLogDeleted);

        // MetricFact 청크 정리 (REQUIRES_NEW) — Project 삭제 단계에서 FK 위반 방지
        do {
            chunkDeleted = platformDataCleanupExecutor.deleteMetricFactChunk(accountId);
            totalMetricFactDeleted += chunkDeleted;
        } while (chunkDeleted > 0);
        log.info("MetricFact 삭제 완료 - platformAccountId={}, totalCount={}", accountId, totalMetricFactDeleted);

        // AdCampaign + PlatformConnection + PlatformAccount + 비어있는 Project 삭제 진행
        // PlatformAccount 가 가장 마지막에 삭제됨 & 삭제 실패 시 전체 롤백되어 다음 회차에 재시도
        platformDataCleanupExecutor.deleteAccountAndRelations(accountId, projectIds);
    }

    private void validateNaverCredentials(String customerId, String encryptedApiKey, String encryptedSecretKey) {
        try {
            PlatformConnection tempConnection = PlatformConverter.toTempPlatformConnection(customerId, encryptedApiKey, encryptedSecretKey);

            Map<String, String> headers = naverAdAuthStrategy.generateHeaders(
                    tempConnection,
                    AdAuthRequest.forMethodAndPath("GET", "/ncc/campaigns")
            );
            naverClient.getCampaigns(headers);

        } catch (FeignException e) {
            log.warn("네이버 광고 API 자격증명 검증 실패 - customerId={}, status={}", customerId, e.status());
            throw new PlatformHandler(PlatformErrorCode.NAVER_API_AUTH_FAILED);
        } catch (Exception e) {
            log.error("네이버 API 검증 중 예외 발생 - customerId={}", customerId, e);
            throw new PlatformHandler(PlatformErrorCode.NAVER_API_AUTH_FAILED);
        }
    }
}
