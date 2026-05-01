package com.whereyouad.WhereYouAd.domains.platform.domain.service;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v23.services.CustomerServiceClient;
import com.google.ads.googleads.v23.services.ListAccessibleCustomersRequest;
import com.google.ads.googleads.v23.services.ListAccessibleCustomersResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.UserCredentials;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.AuthType;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoogleAdOAuthService {
    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.redirect-uri}")
    private String redirectUri;

    @Value("${google.ads.developer-token}")
    private String developerToken;

    // 구글 광고 API 접근 권한 스코프
    private final String SCOPE = "https://www.googleapis.com/auth/adwords";

    private final PlatformAccountRepository platformAccountRepository;

    private final PlatformConnectionRepository platformConnectionRepository;

    private final OrgRepository orgRepository;

    private final UserRepository userRepository;

    private final OrgMemberRepository orgMemberRepository;

    private final AESUtil aesUtil;

    public String exchangeCodeAndSavePlatformConnection(Long userId, Long orgId, String code) throws IOException {
        Organization organization = orgRepository.findById(orgId).orElseThrow(() -> {
                    throw new OrgHandler(OrgErrorCode.ORG_NOT_FOUND);
        });

        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserHandler(UserErrorCode.USER_NOT_FOUND);
        });

        if(!orgMemberRepository.findByUserIdAndOrgId(userId, orgId).isPresent())
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);

        GoogleTokenResponse googleTokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                new GsonFactory(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                code,
                redirectUri
        ).execute();

        String refreshToken = googleTokenResponse.getRefreshToken();

        Long expiresInSeconds = googleTokenResponse.getExpiresInSeconds(); // Access Token 만료시간

        // 발급받은 토큰으로 Google Ads에 접근해 해당 유저의 광고 계정(Customer ID) 리스트 조회
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        GoogleAdsClient googleAdsClient = GoogleAdsClient.newBuilder()
                .setDeveloperToken(developerToken)
                .setCredentials(credentials)
                .build();

        try (CustomerServiceClient customerServiceClient = googleAdsClient.getLatestVersion().createCustomerServiceClient()) {
            ListAccessibleCustomersResponse response = customerServiceClient.listAccessibleCustomers(
                    ListAccessibleCustomersRequest.newBuilder().build()
            );

            byte[] encryptedBytes;
            try {
                encryptedBytes = aesUtil.encryptAES(refreshToken);
            } catch (GeneralSecurityException e) {
                // 암호화 실패 시 런타임 에러 발생 (GlobalExceptionHandler에서 처리됨)
                throw new RuntimeException("구글 토큰 암호화 중 오류가 발생했습니다.", e);
            }

            String encryptedRefreshToken = new String(encryptedBytes);

            // 찾아온 광고 계정 목록을 DB에 저장 (계정이 여러 개)
            for (String resourceName : response.getResourceNamesList()) {
                String customerId = resourceName.replace("customers/", "");

                // PlatformAccount 생성 및 저장
                PlatformAccount platformAccount = PlatformAccount.builder()
                        .externalAccountId(customerId) // 구글 광고 계정 ID
                        .accountName("Google Ads - " + customerId)
                        .status(PlatformStatus.ACTIVE)
                        .provider(Provider.GOOGLE)
                        .organization(organization)
                        .build();
                platformAccountRepository.save(platformAccount);

                // PlatformConnection 생성 및 저장
                PlatformConnection platformConnection = PlatformConnection.builder()
                        .authType(AuthType.OAUTH)
                        .authIdentifier(clientId) // authIdentifier: 식별자 (클라이언트 ID 등)
                        .authCredential(encryptedRefreshToken) // authCredential: refreshToken
                        .tokenExpireAt(expiresInSeconds != null ? LocalDateTime.now().plusSeconds(expiresInSeconds) : null)
                        .user(user)
                        .platformAccount(platformAccount)
                        .build();
                platformConnectionRepository.save(platformConnection);
            }
        }

        return refreshToken;
    }
}
