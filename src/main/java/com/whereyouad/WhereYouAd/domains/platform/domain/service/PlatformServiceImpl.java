package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.request.PlatformRequest;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.PlatformResponse;
import com.whereyouad.WhereYouAd.domains.platform.application.mapper.PlatformConverter;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
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
import org.springframework.transaction.annotation.Transactional;

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
