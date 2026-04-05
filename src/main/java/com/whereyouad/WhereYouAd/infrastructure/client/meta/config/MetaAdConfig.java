package com.whereyouad.WhereYouAd.infrastructure.client.meta.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "meta.ad")
public class MetaAdConfig {

    private String appId;            // Meta App ID
    private String appSecret;        // Meta App Secret
    private String redirectUri;      // OAuth Redirect URI
    private String graphApiVersion;  // v25.0

    /**
     * Facebook OAuth 로그인 URL 생성
     */
    public String buildAuthorizationUrl(Long orgId) {
        // [수정사항] 동적으로 치환({orgId})하지 않고 설정된 고정 redirectUri를 그대로 사용합니다.
        // 그리고 orgId라는 컨텍스트를 유지하기 위해 OAuth 표준 파라미터인 state에 넣어서 넘깁니다.
        return "https://www.facebook.com/" + graphApiVersion + "/dialog/oauth"
                + "?client_id=" + appId
                + "&redirect_uri=" + redirectUri
                + "&scope=ads_read,business_management"
                + "&response_type=code"
                + "&state=" + orgId; // state 파라미터 추가
    }

    /**
     * Graph API base URL
     * https://graph.facebook.com/v25.0
     */
    public String getGraphApiBaseUrl() {
        return "https://graph.facebook.com/" + graphApiVersion;
    }
}
