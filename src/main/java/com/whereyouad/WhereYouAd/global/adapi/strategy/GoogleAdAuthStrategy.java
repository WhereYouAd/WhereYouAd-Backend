package com.whereyouad.WhereYouAd.global.adapi.strategy;

import com.google.auth.oauth2.UserCredentials;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import com.whereyouad.WhereYouAd.global.adapi.AdAuthStrategy;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class GoogleAdAuthStrategy implements AdAuthStrategy {

    private final AESUtil aesUtil;

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.client-secret}")
    private String clientSecret;

    @Value("${google.ads.developer-token}")
    private String developerToken;

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public Map<String, String> generateHeaders(PlatformConnection connection, AdAuthRequest request) throws GeneralSecurityException {
        Map<String, String> headers = new HashMap<>();

        // 리프레시 토큰을 통해 액세스 토큰 가져오기
        String refreshToken = new String(aesUtil.decryptAES(connection.getAuthCredential()), StandardCharsets.UTF_8);

        // 발급받은 토큰으로 Google Ads에 접근해 해당 유저의 광고 계정(Customer ID) 리스트 조회
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        // 만료/처음일 시 새 액세스 토큰 발급
        try {
            credentials.refreshIfExpired();
        } catch (IOException e) {
            log.error("Google Access Token 갱신 중 네트워크 오류 발생", e);
            throw new RuntimeException("구글 광고 API 토큰 갱신 실패", e);
        }

        String freshAccessToken = credentials.getAccessToken().getTokenValue();

        // 헤더 생성 로직 추가
        headers.put("developer-token", developerToken);
        headers.put("Authorization", "Bearer " + freshAccessToken);
        headers.put("Content-Type", "application/json");

        return headers;
    }
}
