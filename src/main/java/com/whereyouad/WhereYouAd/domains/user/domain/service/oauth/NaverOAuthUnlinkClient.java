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
public class NaverOAuthUnlinkClient implements SocialOAuthUnlinkClient {

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public NaverOAuthUnlinkClient(
            @Qualifier("socialOAuthRestClient") RestClient restClient,
            @Value("${spring.security.oauth2.client.registration.naver.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.naver.client-secret}") String clientSecret
    ) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public Provider provider() {
        return Provider.NAVER;
    }

    @Override
    public void unlink(SocialOAuthCredential credential) {
        try {
            String accessToken = credential.hasUsableAccessToken()
                    ? credential.accessToken()
                    : refreshAccessToken(credential.refreshToken());
            deleteAccessToken(accessToken);
        } catch (RestClientException e) {
            log.warn("[NAVER] 소셜 로그인 연동 해제 실패: {}", e.getMessage());
            throw new UserHandler(UserErrorCode.SOCIAL_UNLINK_FAILED);
        }
    }

    private String refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UserHandler(UserErrorCode.SOCIAL_REAUTH_REQUIRED);
        }

        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        NaverTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(NaverTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new UserHandler(UserErrorCode.SOCIAL_UNLINK_FAILED);
        }
        return response.accessToken();
    }

    private void deleteAccessToken(String accessToken) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "delete");
        form.add("access_token", accessToken);
        form.add("service_provider", "NAVER");

        NaverDeleteResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(NaverDeleteResponse.class);

        if (response == null || !"success".equalsIgnoreCase(response.result())) {
            throw new UserHandler(UserErrorCode.SOCIAL_UNLINK_FAILED);
        }
    }

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        return form;
    }

    private record NaverTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record NaverDeleteResponse(String result) {
    }
}
