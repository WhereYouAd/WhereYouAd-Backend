package com.whereyouad.WhereYouAd.infrastructure.client.meta.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "meta.ad")
public class MetaAdConfig {

    private String appId;               // Meta App ID
    private String appSecret;           // Meta App Secret
    private String redirectUri;         // OAuth Redirect URI (Meta가 호출하는 백엔드 콜백 URL)
    private String frontendCallbackUrl; // 백엔드 콜백 처리 후 사용자 리다이렉트할 프론트엔드 결과 페이지 URL
    private String graphApiVersion;     // v25.0

    /**
     * Facebook OAuth 로그인 URL 생성
     */
    public String buildAuthorizationUrl(String state) {

        return UriComponentsBuilder.fromHttpUrl("https://www.facebook.com")
                .pathSegment(graphApiVersion, "dialog", "oauth")
                .queryParam("client_id", appId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "ads_read,business_management")
                .queryParam("response_type", "code")
                .queryParam("state", state)  //  orgId-userId 조각
                .build()
                .toUriString();
    }

    /**
     * Graph API base URL
     * https://graph.facebook.com/v25.0
     */
    public String getGraphApiBaseUrl() {
        return "https://graph.facebook.com/" + graphApiVersion;
    }
}
