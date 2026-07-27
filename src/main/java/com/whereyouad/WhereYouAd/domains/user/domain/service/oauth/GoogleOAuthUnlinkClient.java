package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GoogleOAuthUnlinkClient implements SocialOAuthUnlinkClient {

    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";

    private final RestClient restClient;

    public GoogleOAuthUnlinkClient(@Qualifier("socialOAuthRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public void unlink(SocialOAuthCredential credential) {
        String token = resolveRevocationToken(credential);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);

        try {
            restClient.post()
                    .uri(REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("[GOOGLE] 소셜 로그인 연동 해제 실패: {}", e.getMessage());
            throw new UserHandler(UserErrorCode.SOCIAL_UNLINK_FAILED);
        }
    }

    private String resolveRevocationToken(SocialOAuthCredential credential) {
        if (credential.hasUsableAccessToken()) {
            return credential.accessToken();
        }
        if (credential.refreshToken() != null && !credential.refreshToken().isBlank()) {
            return credential.refreshToken();
        }
        throw new UserHandler(UserErrorCode.SOCIAL_REAUTH_REQUIRED);
    }
}
