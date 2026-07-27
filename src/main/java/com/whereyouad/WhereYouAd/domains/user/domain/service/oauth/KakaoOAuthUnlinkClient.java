package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoOAuthUnlinkClient implements SocialOAuthUnlinkClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public KakaoOAuthUnlinkClient(
            @Qualifier("socialOAuthRestClient") RestClient restClient,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.kakao.client-secret}") String clientSecret
    ) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    public void unlink(SocialOAuthCredential credential) {
        try {
            // 카카오는 AccessToken으로만 연결을 끊을 수 있어 만료 시 RefreshToken으로 재발급한다.
            String accessToken = credential.hasUsableAccessToken()
                    ? credential.accessToken()
                    : refreshAccessToken(credential.refreshToken());

            // 재발급을 포함해 사용할 수 있는 AccessToken을 Bearer 인증으로 전달한다.
            restClient.post()
                    .uri(UNLINK_URL)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("[KAKAO] 소셜 로그인 연동 해제 실패: {}", e.getMessage());
            throw new UserHandler(UserErrorCode.SOCIAL_UNLINK_FAILED);
        }
    }

    private String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            // 토큰을 갱신할 수 없으므로 재로그인 후 다시 탈퇴를 요청해야 한다.
            throw new UserHandler(UserErrorCode.SOCIAL_REAUTH_REQUIRED);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        KakaoTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        // HTTP 요청이 성공해도 AccessToken이 없으면 정상적으로 갱신된 것으로 볼 수 없다.
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new UserHandler(UserErrorCode.SOCIAL_UNLINK_FAILED);
        }
        return response.accessToken();
    }

    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}
