package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google.GoogleAdOAuthService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.GoogleAdOAuthDocs;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/google")
public class GoogleAdOAuthController implements GoogleAdOAuthDocs {

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.redirect-uri}")
    private String redirectUri;

    @Value("${oauth2.redirect-url}")
    private String frontendCallbackUrl;

    // 구글 광고 API 접근 권한 스코프
    private final String SCOPE = "https://www.googleapis.com/auth/adwords";

    private final GoogleAdOAuthService googleAdOAuthService;

    private final RedisUtil redisUtil;

    // 구글 연동 로그인 화면으로 리다이렉트
    @GetMapping("/login")
    public ResponseEntity<Void> redirectToGoogleAuth(@RequestParam("orgId") Long orgId, @AuthenticationPrincipal(expression = "userId") Long userId,
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

        URI location = URI.create(authUrl);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> exchangeCodeForToken(@RequestParam("code") String code, @RequestParam("state") String state) {
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

        try {
            // 2. 구글 토큰 발급 및 연동
            googleAdOAuthService.exchangeCodeAndSavePlatformConnection(userId, orgId, code);

            // 3. 연동 성공 시 프론트엔드로 리다이렉트
            return redirectToFrontend("success", null);

        } catch (Exception e) {
            // 연동 중 서버 에러 발생 시 프론트엔드로 실패 상태 전달
            return redirectToFrontend("error", "google_oauth_failed");
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String status, String detail) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(frontendCallbackUrl)
                .queryParam("status", status); // 성공/실패 여부

        if (detail != null) {
            builder.queryParam("detail", detail); // 상세 에러 내용
        }

        URI location = builder.encode().build().toUri();
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(location)
                .build();
    }
}
