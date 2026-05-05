package com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.AuthType;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Currency;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Timezone;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.client.MetaClient;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.config.MetaAdConfig;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.converter.MetaConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaDTO;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaAuthService {

    /**
     * Meta 광고 플랫폼과의 OAuth 2.0 인증 및 계정 연동 전담 Service
     *
     * - OAuth 로그인 통신 (인가 URL 생성, 토큰 발급 및 교환)
     * - PlatformAccount 및 PlatformConnection DB 초기 세팅
     * - 외부 API 통신과 User, Organization 결합
     */

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final UserRepository userRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformConnectionRepository platformConnectionRepository;

    private final MetaClient metaClient;
    private final MetaAdConfig metaAdConfig;
    private final AESUtil aesUtil;

    // OAuth 인증 URL 생성
    public MetaResponse.AuthUrlResponse getAuthorizationUrl(Long userId, Long orgId) {

        //조직 존재 검증
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //조직에 속한 회원 검증
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        String state = orgId + "-" + userId;
        String authUrl = metaAdConfig.buildAuthorizationUrl(state);

        return MetaConverter.toAuthUrlResponse(authUrl);
    }

    // Meta 로그인 콜백 이후 핵심 연동 파이프라인(토큰 교환 -> 계정 조회 -> DB 저장) 순차 실행
    public void processAuthCallbackAndSave(Long orgId, Long userId, String code) {

        //토큰 교환 (code -> shortLived -> longLived Token)
        MetaDTO.TokenResponse shortLived = exchangeCodeForToken(code);
        MetaDTO.TokenResponse longLived = exchangeForLongLivedToken(shortLived.accessToken());

        //계정 조회
        MetaDTO.AdAccountListResponse adAccounts = fetchAdAccounts(longLived.accessToken());

        //조직 & 회원 조회 + 조직에 속하는 회원 검증
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        //DB 저장(PlatformAccount, PlatformConnection)
        savePlatformConnection(longLived, org, user, adAccounts);
    }

    // code -> short-lived 토큰으로 교환
    private MetaDTO.TokenResponse exchangeCodeForToken(String code) {
        try {

            MetaDTO.TokenResponse response = metaClient.exchangeCodeForToken(
                    metaAdConfig.getAppId(),
                    metaAdConfig.getAppSecret(),
                    metaAdConfig.getRedirectUri(),
                    code);

            if (response == null) {
                throw new AdApiHandler(AdApiErrorCode.TOKEN_RESPONSE_EMPTY);
            }

            return response;

        } catch (FeignException e) {
            log.error("[META] 토큰 교환 실패 - status={}, body={}", e.status(), e.contentUTF8());
            throw new AdApiHandler(AdApiErrorCode.TOKEN_EXCHANGE_FAILED);
        }
    }

    // short-lived -> long-lived 토큰으로 교환
    private MetaDTO.TokenResponse exchangeForLongLivedToken(String shortLivedToken) {
        try {

            MetaDTO.TokenResponse response = metaClient.exchangeForLongLivedToken(
                    "fb_exchange_token",
                    metaAdConfig.getAppId(),
                    metaAdConfig.getAppSecret(),
                    shortLivedToken);

            if (response == null) {
                throw new AdApiHandler(AdApiErrorCode.TOKEN_RESPONSE_EMPTY);
            }

            log.info("[META] Long-lived 토큰 발급 - expiresIn={}초", response.expiresIn());
            return response;
        } catch (FeignException e) {

            log.error("[META] Long-lived 토큰 교환 실패 - body={}", e.contentUTF8());
            throw new AdApiHandler(AdApiErrorCode.TOKEN_EXCHANGE_FAILED);
        }
    }

    //Feign Client 를 사용해 계정 정보 조회
    private MetaDTO.AdAccountListResponse fetchAdAccounts(String accessToken) {
        try {
            return metaClient.getAdAccounts(accessToken, "id,name,account_id,account_status,currency,timezone_name");
        } catch (FeignException e) {
            log.error("[META] 광고계정 조회 실패 - status={}, body={}", e.status(), e.contentUTF8());
            throw new AdApiHandler(AdApiErrorCode.AD_ACCOUNT_FETCH_FAILED);
        }
    }

    // 발급 받은 토큰(long-lived) 와 조직 & 회원 정보, 계정 정보 기반 PlatformAccount & PlatformConnection 을 DB 에 저장
    private void savePlatformConnection(MetaDTO.TokenResponse token, Organization org, User user, MetaDTO.AdAccountListResponse adAccounts) {
        try {
            String encAccessToken = new String(aesUtil.encryptAES(token.accessToken()), StandardCharsets.UTF_8);

            if (adAccounts == null || adAccounts.data() == null || adAccounts.data().isEmpty()) {
                throw new AdApiHandler(AdApiErrorCode.NO_LINKABLE_AD_ACCOUNT);
            }

            for (MetaDTO.AdAccount metaAccount : adAccounts.data()) {

                // Currency 파싱 -> 기본값 KRW
                Currency dynamicCurrency;
                try {
                    //Currency 가 null 로 넘어온것이 아니면 해당 String 을 대문자화 하여 Currency enum 과 매칭 시도
                    dynamicCurrency = (metaAccount.currency() != null)
                            ? Currency.valueOf(metaAccount.currency().toUpperCase())
                            : Currency.KRW;
                } catch (Exception e) {
                    //만약 우리 Currency enum 에 없는 값이면 KRW 로 통일
                    dynamicCurrency = Currency.KRW;
                }
                Currency finalDynamicCurrency = dynamicCurrency;

                // Timezone 파싱 -> 메타 마케팅 API 에서는 "Asia/Seoul" 같이 표기
                // 더 작은 단위로 설정하도록 파싱
                Timezone dynamicTimezone;
                try {
                    if (metaAccount.timezoneName() != null) {
                        String timeZoneString = metaAccount.timezoneName().toUpperCase();

                        if (timeZoneString.contains("SEOUL")) {
                            dynamicTimezone = Timezone.SEOUL;
                        } else {
                            dynamicTimezone = Timezone.ASIA;
                        }

                    } else {
                        dynamicTimezone = Timezone.ASIA;
                    }
                } catch (Exception e) {
                    dynamicTimezone = Timezone.ASIA;
                }
                Timezone finalDynamicTimezone = dynamicTimezone;

                // (externalAccountId, provider, organization) 조합으로 조회하여
                // 동일 Meta 광고계정을 다른 조직이 연결해도 서로 격리된 PlatformAccount를 갖도록 보장
                PlatformAccount account = platformAccountRepository
                        .findByExternalAccountIdAndProviderAndOrganization(metaAccount.id(), Provider.META, org)
                        .orElseGet(() -> platformAccountRepository.save(
                                PlatformAccount.builder()
                                        .externalAccountId(metaAccount.id())
                                        .accountName(metaAccount.name())
                                        .provider(Provider.META)
                                        .currency(finalDynamicCurrency)
                                        .timezone(finalDynamicTimezone)
                                        .status(PlatformStatus.ACTIVE)
                                        .organization(org)
                                        .build()));

                // Meta API 에서 만료시간이 null 값으로 내려오거나 Long 타입 직렬화 실패로 인해 null 인 경우,
                // 60일(5184000L) 디폴트로 만료시각 주입
                LocalDateTime expireAt = LocalDateTime.now().plusSeconds(token.expiresIn() != null ? token.expiresIn() : 5184000L);

                // PlatformConnection 생성(UPSERT)
                // -> Meta 계정 재연동 or 토큰 만료로 인한 재로그인 시 PlatformConnection 이 계속해서 누적되는 문제 방지
                PlatformConnection connection = platformConnectionRepository
                        .findByUserIdAndPlatformAccountId(user.getId(), account.getId())
                        .map(existing -> {
                            existing.renewOAuth(encAccessToken, expireAt);
                            return existing;
                        })
                        .orElseGet(() -> PlatformConnection.builder()
                                .authType(AuthType.OAUTH)
                                .authIdentifier(encAccessToken)
                                .tokenExpireAt(expireAt)
                                .user(user)
                                .platformAccount(account)
                                .build());

                platformConnectionRepository.save(connection);
            }

        } catch (UserHandler | AdApiHandler e) {

            throw e;
        } catch (Exception e) {

            throw new AdApiHandler(AdApiErrorCode.CONNECTION_SAVE_FAILED);
        }
    }
}
