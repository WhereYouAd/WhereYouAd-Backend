package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google.GoogleAdOAuthService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.GoogleAdOAuthDocs;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/google")
public class GoogleAdOAuthController implements GoogleAdOAuthDocs {

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.redirect-uri}")
    private String redirectUri;

    // 구글 광고 API 접근 권한 스코프
    private final String SCOPE = "https://www.googleapis.com/auth/adwords";

    private final GoogleAdOAuthService googleAdOAuthService;

    private final RedisUtil redisUtil;

    // 구글 연동 로그인 화면으로 리다이렉트
    @GetMapping("/login")
    public void redirectToGoogleAuth(@RequestParam("orgId") Long orgId, @AuthenticationPrincipal(expression = "userId") Long userId,
                                     HttpServletResponse response) throws IOException {

        String stateToken = UUID.randomUUID().toString();
        String rawState = userId + "_" + orgId;

        redisUtil.setDataExpire("OAUTH_STATE:" + stateToken, rawState, 300L);

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + SCOPE +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + stateToken;

        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public void exchangeCodeForToken(@RequestParam("code") String code, @RequestParam("state") String state) throws IOException {
        // Redis에서 꺼낸 state와 같은지 비교
        String redisKey = "OAUTH_STATE:" + state;
        String stateToken = redisUtil.getData(redisKey);

        // 유효하지 않거나 만료된 접근 방어
        if (stateToken == null)
            throw new AdApiHandler(AdApiErrorCode.INVALID_OAUTH_STATE);

        redisUtil.deleteData(redisKey);

        String[] parts = stateToken.split("_");
        Long userId = Long.parseLong(parts[0]);
        Long orgId = Long.parseLong(parts[1]);

        googleAdOAuthService.exchangeCodeAndSavePlatformConnection(userId, orgId, code);
    }
}
